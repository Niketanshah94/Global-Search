package com.quickfind.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** (query, resultId, timestamp) for history-based ranking (spec 4.7). */
@Entity(tableName = "query_history", indices = [Index("query"), Index("timestamp")])
data class QueryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    @ColumnInfo(name = "result_id") val resultId: String,
    val timestamp: Long,
)

/** Every launch, regardless of query, for frecency. */
@Entity(tableName = "launch_events", indices = [Index("result_id"), Index("timestamp")])
data class LaunchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "result_id") val resultId: String,
    val timestamp: Long,
)

@Entity(tableName = "hidden_results")
data class HiddenResultEntity(
    @PrimaryKey @ColumnInfo(name = "result_id") val resultId: String,
    val title: String,
    val type: String,
    @ColumnInfo(name = "hidden_at") val hiddenAt: Long,
)

/** A pinned result, stored as serialized [com.quickfind.core.model.SearchResult] JSON. */
@Entity(tableName = "pinned_results")
data class PinnedResultEntity(
    @PrimaryKey @ColumnInfo(name = "result_id") val resultId: String,
    val payload: String,
    @ColumnInfo(name = "pinned_at") val pinnedAt: Long,
)

/** A user-chosen nickname for a result ("ma" for Mom), with the result stored as JSON. */
@Entity(tableName = "nicknames")
data class NicknameEntity(
    @PrimaryKey @ColumnInfo(name = "result_id") val resultId: String,
    val nickname: String,
    val payload: String,
)

/** A notification kept for search for 7 days (spec 10.7). Never leaves the phone. */
@Entity(
    tableName = "notifications",
    indices = [Index("posted_at"), Index(value = ["notification_key"], unique = true)],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** StatusBarNotification.key plus a hash of the content, so updated chats keep each message. */
    @ColumnInfo(name = "notification_key") val key: String,
    /** The live StatusBarNotification.key, to find the notification's own tap target. */
    @ColumnInfo(name = "sbn_key") val sbnKey: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    val title: String,
    val text: String,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
)

enum class FileOrigin { MEDIASTORE, WALK, SAF, DERIVED }

/**
 * One file or folder in the on-device index (spec 5.3).
 *
 * Folder rows ([isFolder]) are derived from distinct relative paths: [relativePath] is the
 * parent ("DCIM/"), [displayName] the folder name ("Camera") and [contentUri] the folder's
 * own full path ("DCIM/Camera/").
 */
@Entity(
    tableName = "files",
    indices = [
        Index(value = ["source_key"], unique = true),
        Index("relative_path"),
        Index("date_modified"),
        Index("origin"),
    ],
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "ms:<mediaId>", "fs:<path>", "saf:<documentUri>" or "dir:<path>". */
    @ColumnInfo(name = "source_key") val sourceKey: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    /** Normalized words of the name, space separated, for FTS prefix queries. */
    @ColumnInfo(name = "name_tokens") val nameTokens: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    val size: Long,
    /** Epoch millis. */
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "is_folder") val isFolder: Boolean,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    val origin: FileOrigin,
    @ColumnInfo(name = "scan_id") val scanId: Long = 0,
)

/** External-content FTS4 table over [FileEntity]; Room keeps it in sync with triggers. */
@Fts4(contentEntity = FileEntity::class)
@Entity(tableName = "files_fts")
data class FileFtsEntity(
    @ColumnInfo(name = "name_tokens") val nameTokens: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
)

@Entity(tableName = "index_meta")
data class IndexMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
