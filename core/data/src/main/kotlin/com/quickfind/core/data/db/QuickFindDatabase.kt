package com.quickfind.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [
        QueryHistoryEntity::class,
        LaunchEventEntity::class,
        HiddenResultEntity::class,
        PinnedResultEntity::class,
        FileEntity::class,
        FileFtsEntity::class,
        IndexMetaEntity::class,
    ],
    version = 1,
    // TODO(Phase 2): turn on schema export with the Room Gradle plugin before the first migration.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class QuickFindDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun resultPrefsDao(): ResultPrefsDao
    abstract fun fileDao(): FileDao
    abstract fun indexMetaDao(): IndexMetaDao

    companion object {
        /** Excluded from Auto Backup (see app/src/main/res/xml/data_extraction_rules.xml). */
        const val NAME = "quickfind.db"
    }
}

class Converters {
    @TypeConverter fun fromOrigin(origin: FileOrigin): String = origin.name
    @TypeConverter fun toOrigin(value: String): FileOrigin = FileOrigin.valueOf(value)
}
