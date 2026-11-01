package com.quickfind.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert suspend fun insertPick(pick: QueryHistoryEntity)
    @Insert suspend fun insertLaunch(launch: LaunchEventEntity)

    @Query("SELECT * FROM query_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentPicks(limit: Int): List<QueryHistoryEntity>

    @Query("SELECT * FROM launch_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun launchesSince(since: Long): List<LaunchEventEntity>

    @Query("DELETE FROM query_history WHERE timestamp < :before")
    suspend fun prunePicks(before: Long)

    @Query("DELETE FROM launch_events WHERE timestamp < :before")
    suspend fun pruneLaunches(before: Long)

    @Query("DELETE FROM query_history WHERE result_id = :resultId")
    suspend fun forgetPicks(resultId: String)

    @Query("DELETE FROM launch_events WHERE result_id = :resultId")
    suspend fun forgetLaunches(resultId: String)

    @Query("DELETE FROM query_history") suspend fun clearPicks()
    @Query("DELETE FROM launch_events") suspend fun clearLaunches()
}

@Dao
interface ResultPrefsDao {
    @Upsert suspend fun hide(entity: HiddenResultEntity)
    @Query("DELETE FROM hidden_results WHERE result_id = :resultId") suspend fun unhide(resultId: String)
    @Query("SELECT * FROM hidden_results ORDER BY hidden_at DESC") fun hidden(): Flow<List<HiddenResultEntity>>

    @Upsert suspend fun pin(entity: PinnedResultEntity)
    @Query("DELETE FROM pinned_results WHERE result_id = :resultId") suspend fun unpin(resultId: String)
    @Query("SELECT * FROM pinned_results ORDER BY pinned_at ASC") fun pinned(): Flow<List<PinnedResultEntity>>
}

@Dao
interface FileDao {
    /** REPLACE resolves conflicts on the unique source_key; FTS triggers follow the delete+insert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(files: List<FileEntity>)

    /** Blocking variant for the file-tree walker, which runs inside a non-suspending visitor. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAllBlocking(files: List<FileEntity>)

    @Query(
        """
        SELECT files.* FROM files
        JOIN files_fts ON files.id = files_fts.rowid
        WHERE files_fts MATCH :match
        LIMIT :limit
        """,
    )
    suspend fun ftsSearch(match: String, limit: Int): List<FileEntity>

    /** Fallback for in-word fragments the FTS prefix query cannot see ("sume" in "resume"). */
    @Query("SELECT * FROM files WHERE name_tokens LIKE '%' || :fragment || '%' LIMIT :limit")
    suspend fun substringSearch(fragment: String, limit: Int): List<FileEntity>

    @Query("SELECT * FROM files WHERE relative_path = :path ORDER BY is_folder DESC, display_name COLLATE NOCASE")
    suspend fun inFolder(path: String): List<FileEntity>

    @Query("SELECT * FROM files WHERE is_folder = 0 ORDER BY date_modified DESC LIMIT :limit")
    suspend fun recentFiles(limit: Int): List<FileEntity>

    @Query("SELECT DISTINCT relative_path FROM files WHERE is_folder = 0")
    suspend fun distinctFilePaths(): List<String>

    @Query("SELECT source_key FROM files WHERE origin = :origin")
    suspend fun sourceKeys(origin: FileOrigin): List<String>

    @Query("SELECT relative_path || display_name FROM files WHERE origin = 'MEDIASTORE'")
    suspend fun mediaStorePaths(): List<String>

    @Query("DELETE FROM files WHERE source_key IN (:keys)")
    suspend fun deleteBySourceKeys(keys: List<String>)

    @Query("DELETE FROM files WHERE origin = :origin AND scan_id < :scanId")
    suspend fun deleteStale(origin: FileOrigin, scanId: Long)

    @Query("DELETE FROM files WHERE origin = :origin")
    suspend fun deleteByOrigin(origin: FileOrigin)

    @Query("DELETE FROM files WHERE relative_path LIKE :prefix || '%'")
    suspend fun deleteUnderPath(prefix: String)

    @Query("DELETE FROM files") suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM files WHERE is_folder = 0") fun fileCount(): Flow<Int>

    @Transaction
    suspend fun replaceFolders(folders: List<FileEntity>) {
        deleteByOrigin(FileOrigin.DERIVED)
        folders.chunked(500).forEach { upsertAll(it) }
    }
}

@Dao
interface IndexMetaDao {
    @Query("SELECT value FROM index_meta WHERE `key` = :key") suspend fun get(key: String): String?
    @Upsert suspend fun put(entity: IndexMetaEntity)
    @Query("DELETE FROM index_meta") suspend fun clear()
}
