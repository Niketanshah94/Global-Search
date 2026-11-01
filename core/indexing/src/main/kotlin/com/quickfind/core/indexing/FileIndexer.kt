package com.quickfind.core.indexing

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.quickfind.core.data.db.FileDao
import com.quickfind.core.data.db.FileEntity
import com.quickfind.core.data.db.FileOrigin
import com.quickfind.core.data.db.IndexMetaDao
import com.quickfind.core.data.db.IndexMetaEntity
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.matching.FileNameTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and maintains the file/folder index (spec 5.3).
 *
 * 1. MediaStore (primary, fast): incremental via GENERATION_MODIFIED on API 30+, else
 *    DATE_MODIFIED; a full ID comparison once a day catches deletions.
 * 2. Full walk (Tier B only): java.nio walkFileTree for paths MediaStore misses.
 * 3. SAF trees the user picked: DocumentsContract queries (DocumentFile is too slow).
 * Folders are derived from distinct relative paths after every scan.
 */
@Singleton
class FileIndexer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDao: FileDao,
    private val meta: IndexMetaDao,
    private val settings: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val storageRoot: File = Environment.getExternalStorageDirectory()

    suspend fun quickScan() = run(full = false)

    suspend fun fullScan() = run(full = true)

    /** "Delete all index data" (spec 13). The index rebuilds on the next scan. */
    suspend fun clear() = mutex.withLock {
        withContext(io) {
            fileDao.clearAll()
            meta.clear()
        }
    }

    private suspend fun run(full: Boolean) = mutex.withLock {
        withContext(io) {
            val s = settings.settings.value
            if (!s.filesEnabled || s.indexingPaused) return@withContext

            if (FilePermissions.hasMediaAccess(context) || FilePermissions.hasAllFilesAccess(context)) {
                scanMediaStore(s, forceFull = full)
            } else {
                // Permission revoked: drop what we can no longer open.
                fileDao.deleteByOrigin(FileOrigin.MEDIASTORE)
            }
            if (full) {
                if (FilePermissions.hasAllFilesAccess(context)) walkStorage(s) else fileDao.deleteByOrigin(FileOrigin.WALK)
                scanDocumentTrees(s)
            }
            s.excludedFolders.forEach { fileDao.deleteUnderPath(it) }
            rebuildFolders()
        }
    }

    // ---------------------------------------------------------------- MediaStore

    private suspend fun scanMediaStore(s: UserSettings, forceFull: Boolean) {
        val collection = MediaStore.Files.getContentUri("external")
        val now = System.currentTimeMillis()
        // getVersion exists from API 29; before that generations do not exist either.
        val version = if (Build.VERSION.SDK_INT >= 29) MediaStore.getVersion(context) else "legacy"
        val lastVersion = meta.get(KEY_VERSION)
        val lastGeneration = meta.get(KEY_GENERATION)?.toLongOrNull()
        val lastScanSeconds = meta.get(KEY_LAST_SCAN_SECONDS)?.toLongOrNull()
        val lastCompare = meta.get(KEY_LAST_FULL_COMPARE)?.toLongOrNull() ?: 0L
        // A new MediaStore version means generations were reset: start over.
        val versionChanged = lastVersion != null && lastVersion != version

        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            if (Build.VERSION.SDK_INT >= 29) add(MediaStore.MediaColumns.RELATIVE_PATH)
            @Suppress("DEPRECATION")
            add(MediaStore.MediaColumns.DATA)
            if (Build.VERSION.SDK_INT >= 30) add(MediaStore.MediaColumns.GENERATION_MODIFIED)
        }.toTypedArray()

        val (selection, args) = when {
            versionChanged || lastScanSeconds == null -> null to null
            Build.VERSION.SDK_INT >= 30 && lastGeneration != null ->
                "${MediaStore.MediaColumns.GENERATION_MODIFIED} > ?" to arrayOf(lastGeneration.toString())
            // Pre-API 30: DATE_MODIFIED (seconds). Slight overlap is fine, rows are upserted.
            else -> "${MediaStore.MediaColumns.DATE_MODIFIED} >= ?" to arrayOf((lastScanSeconds - 1).toString())
        }

        var maxGeneration = lastGeneration ?: 0L
        val batch = ArrayList<FileEntity>(BATCH)
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val cols = MediaColumns(c)
            while (c.moveToNext()) {
                if ((batch.size and 63) == 0) currentCoroutineContext().ensureActive()
                val entity = cols.read(c, s) ?: continue
                if (cols.generation >= 0) maxGeneration = maxOf(maxGeneration, c.getLong(cols.generation))
                batch += entity
                if (batch.size >= BATCH) {
                    fileDao.upsertAll(batch)
                    batch.clear()
                }
            }
        }
        if (batch.isNotEmpty()) fileDao.upsertAll(batch)

        if (forceFull || versionChanged || now - lastCompare > DAY_MS) {
            removeDeletedMedia(collection)
            meta.put(IndexMetaEntity(KEY_LAST_FULL_COMPARE, now.toString()))
        }
        meta.put(IndexMetaEntity(KEY_VERSION, version))
        meta.put(IndexMetaEntity(KEY_GENERATION, maxGeneration.toString()))
        meta.put(IndexMetaEntity(KEY_LAST_SCAN_SECONDS, (now / 1000).toString()))
    }

    private inner class MediaColumns(c: Cursor) {
        val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val iMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val iModified = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        val iMediaType = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val iRelPath = if (Build.VERSION.SDK_INT >= 29) c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
        @Suppress("DEPRECATION")
        val iData = c.getColumnIndex(MediaStore.MediaColumns.DATA)
        val generation = if (Build.VERSION.SDK_INT >= 30) c.getColumnIndex(MediaStore.MediaColumns.GENERATION_MODIFIED) else -1

        fun read(c: Cursor, s: UserSettings): FileEntity? {
            val mediaId = c.getLong(iId)
            val dataPath = if (iData >= 0) c.getString(iData) else null
            val name = c.getString(iName) ?: dataPath?.substringAfterLast('/') ?: return null
            // Directories show up in the Files collection on some devices; folders are derived instead.
            if (dataPath != null && File(dataPath).isDirectory) return null
            val relativePath = (if (iRelPath >= 0) c.getString(iRelPath) else null)
                ?: dataPath?.let(::relativePathOf)
                ?: return null
            if (!isIncluded(relativePath, s)) return null
            val mime = c.getString(iMime) ?: mimeFromName(name)
            val uri = when (c.getInt(iMediaType)) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }
            return FileEntity(
                sourceKey = "ms:$mediaId",
                displayName = name,
                nameTokens = FileNameTokens.of(name),
                relativePath = relativePath,
                mimeType = mime,
                size = c.getLong(iSize),
                dateModified = c.getLong(iModified) * 1000,
                isFolder = false,
                contentUri = ContentUris.withAppendedId(uri, mediaId).toString(),
                origin = FileOrigin.MEDIASTORE,
            )
        }
    }

    private suspend fun removeDeletedMedia(collection: Uri) {
        val present = HashSet<String>()
        context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { c ->
            while (c.moveToNext()) present += "ms:${c.getLong(0)}"
        } ?: return
        val stale = fileDao.sourceKeys(FileOrigin.MEDIASTORE).filter { it !in present }
        stale.chunked(500).forEach { fileDao.deleteBySourceKeys(it) }
    }

    // ---------------------------------------------------------------- Full walk (Tier B)

    private suspend fun walkStorage(s: UserSettings) {
        val scanId = System.currentTimeMillis()
        val known = fileDao.mediaStorePaths().toHashSet()
        val root = storageRoot.toPath()
        val batch = ArrayList<FileEntity>(BATCH)
        val job = currentCoroutineContext()

        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!job.isActive) return FileVisitResult.TERMINATE
                    if (dir == root) return FileVisitResult.CONTINUE
                    val rel = relativeDir(dir.toFile())
                    if (SKIPPED_DIRS.any { rel.startsWith(it, ignoreCase = true) }) return FileVisitResult.SKIP_SUBTREE
                    if (!isIncluded(rel, s)) return FileVisitResult.SKIP_SUBTREE
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!attrs.isRegularFile) return FileVisitResult.CONTINUE
                    val f = file.toFile()
                    val name = f.name
                    if (!s.indexHiddenFolders && name.startsWith('.')) return FileVisitResult.CONTINUE
                    val rel = relativeDir(f.parentFile ?: return FileVisitResult.CONTINUE)
                    if (rel + name in known) return FileVisitResult.CONTINUE
                    batch += FileEntity(
                        sourceKey = "fs:$rel$name",
                        displayName = name,
                        nameTokens = FileNameTokens.of(name),
                        relativePath = rel,
                        mimeType = mimeFromName(name),
                        size = attrs.size(),
                        dateModified = attrs.lastModifiedTime().toMillis(),
                        isFolder = false,
                        contentUri = Uri.fromFile(f).toString(),
                        origin = FileOrigin.WALK,
                        scanId = scanId,
                    )
                    if (batch.size >= BATCH) {
                        fileDao.upsertAllBlocking(batch.toList())
                        batch.clear()
                    }
                    return FileVisitResult.CONTINUE
                }

                // Unreadable entries (permission, broken symlink) must not stop the walk.
                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
            },
        )
        if (batch.isNotEmpty()) fileDao.upsertAll(batch)
        currentCoroutineContext().ensureActive()
        // Only after a complete walk: rows not seen this time are gone.
        fileDao.deleteStale(FileOrigin.WALK, scanId)
    }

    // ---------------------------------------------------------------- SAF trees

    private suspend fun scanDocumentTrees(s: UserSettings) {
        val scanId = System.currentTimeMillis()
        val persisted = context.contentResolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
        for (tree in s.documentTreeUris) {
            if (tree !in persisted) continue
            runCatching { scanTree(Uri.parse(tree), scanId) }
        }
        fileDao.deleteStale(FileOrigin.SAF, scanId)
    }

    private suspend fun scanTree(treeUri: Uri, scanId: Long) {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootName = queryName(DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)) ?: "Folder"
        val batch = ArrayList<FileEntity>(BATCH)
        // Iterative DFS: (documentId, relativePath, depth).
        val stack = ArrayDeque<Triple<String, String, Int>>()
        stack.addLast(Triple(rootId, "$rootName/", 0))
        var seen = 0
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        while (stack.isNotEmpty() && seen < SAF_MAX_ENTRIES) {
            currentCoroutineContext().ensureActive()
            val (docId, rel, depth) = stack.removeLast()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            context.contentResolver.query(children, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    seen++
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (depth < SAF_MAX_DEPTH && !name.startsWith('.')) stack.addLast(Triple(id, "$rel$name/", depth + 1))
                        continue
                    }
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    batch += FileEntity(
                        sourceKey = "saf:$docUri",
                        displayName = name,
                        nameTokens = FileNameTokens.of(name),
                        relativePath = rel,
                        mimeType = mime ?: mimeFromName(name),
                        size = if (c.isNull(3)) 0 else c.getLong(3),
                        dateModified = if (c.isNull(4)) 0 else c.getLong(4),
                        isFolder = false,
                        contentUri = docUri.toString(),
                        origin = FileOrigin.SAF,
                        scanId = scanId,
                    )
                    if (batch.size >= BATCH) {
                        fileDao.upsertAll(batch)
                        batch.clear()
                    }
                }
            }
        }
        if (batch.isNotEmpty()) fileDao.upsertAll(batch)
    }

    private fun queryName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    // ---------------------------------------------------------------- Folders

    /** Every path segment becomes a folder entry: "DCIM/Camera/" -> "DCIM/", "DCIM/Camera/". */
    private suspend fun rebuildFolders() {
        val folders = HashMap<String, FileEntity>()
        for (path in fileDao.distinctFilePaths()) {
            val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
            var parent = ""
            for (segment in segments) {
                val full = "$parent$segment/"
                folders.getOrPut(full) {
                    FileEntity(
                        sourceKey = "dir:$full",
                        displayName = segment,
                        nameTokens = FileNameTokens.of(segment),
                        relativePath = parent,
                        mimeType = null,
                        size = 0,
                        dateModified = 0,
                        isFolder = true,
                        contentUri = full,
                        origin = FileOrigin.DERIVED,
                    )
                }
                parent = full
            }
        }
        fileDao.replaceFolders(folders.values.toList())
    }

    // ---------------------------------------------------------------- Helpers

    private fun relativePathOf(absolutePath: String): String? {
        val root = storageRoot.absolutePath.trimEnd('/') + "/"
        if (!absolutePath.startsWith(root)) return null
        val rel = absolutePath.removePrefix(root)
        val idx = rel.lastIndexOf('/')
        return if (idx < 0) "" else rel.substring(0, idx + 1)
    }

    private fun relativeDir(dir: File): String {
        val root = storageRoot.absolutePath.trimEnd('/')
        val abs = dir.absolutePath
        if (abs == root) return ""
        return abs.removePrefix("$root/").trimEnd('/') + "/"
    }

    private fun isIncluded(relativePath: String, s: UserSettings): Boolean {
        if (s.excludedFolders.any { relativePath.startsWith(it) }) return false
        if (!s.indexHiddenFolders && relativePath.split('/').any { it.startsWith('.') }) return false
        return true
    }

    private fun mimeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private companion object {
        const val BATCH = 500
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val SAF_MAX_DEPTH = 12
        const val SAF_MAX_ENTRIES = 50_000
        val SKIPPED_DIRS = listOf("Android/data/", "Android/obb/")
        const val KEY_VERSION = "mediastore_version"
        const val KEY_GENERATION = "mediastore_generation"
        const val KEY_LAST_SCAN_SECONDS = "mediastore_last_scan_s"
        const val KEY_LAST_FULL_COMPARE = "mediastore_last_full_compare"
    }
}
