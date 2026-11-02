package com.quickfind.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        QueryHistoryEntity::class,
        LaunchEventEntity::class,
        HiddenResultEntity::class,
        PinnedResultEntity::class,
        FileEntity::class,
        FileFtsEntity::class,
        IndexMetaEntity::class,
        NicknameEntity::class,
        NotificationEntity::class,
    ],
    version = 3,
    // TODO(Phase 2): turn on schema export with the Room Gradle plugin before the first migration.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class QuickFindDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun resultPrefsDao(): ResultPrefsDao
    abstract fun fileDao(): FileDao
    abstract fun indexMetaDao(): IndexMetaDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        /** Excluded from Auto Backup (see app/src/main/res/xml/data_extraction_rules.xml). */
        const val NAME = "quickfind.db"

        /** v2 adds nicknames. Hand-written so upgrading keeps history, pins and the index. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `nicknames` (`result_id` TEXT NOT NULL, `nickname` TEXT NOT NULL, " +
                        "`payload` TEXT NOT NULL, PRIMARY KEY(`result_id`))",
                )
            }
        }

        /** v3 adds notification history. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`notification_key` TEXT NOT NULL, `sbn_key` TEXT NOT NULL, `package_name` TEXT NOT NULL, " +
                        "`app_label` TEXT NOT NULL, `title` TEXT NOT NULL, `text` TEXT NOT NULL, `posted_at` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_posted_at` ON `notifications` (`posted_at`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_notifications_notification_key` ON `notifications` (`notification_key`)",
                )
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromOrigin(origin: FileOrigin): String = origin.name
    @TypeConverter fun toOrigin(value: String): FileOrigin = FileOrigin.valueOf(value)
}
