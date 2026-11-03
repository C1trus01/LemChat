package com.flxrs.dankchat.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flxrs.dankchat.data.database.converter.InstantConverter
import com.flxrs.dankchat.data.database.dao.BadgeHighlightDao
import com.flxrs.dankchat.data.database.dao.BlacklistedUserDao
import com.flxrs.dankchat.data.database.dao.EmoteUsageDao
import com.flxrs.dankchat.data.database.dao.MessageHighlightDao
import com.flxrs.dankchat.data.database.dao.MessageIgnoreDao
import com.flxrs.dankchat.data.database.dao.RecentUploadsDao
import com.flxrs.dankchat.data.database.dao.UserDisplayDao
import com.flxrs.dankchat.data.database.dao.UserHighlightDao
import com.flxrs.dankchat.data.database.dao.UserIgnoreDao
import com.flxrs.dankchat.data.database.entity.BadgeHighlightEntity
import com.flxrs.dankchat.data.database.entity.BlacklistedUserEntity
import com.flxrs.dankchat.data.database.entity.EmoteUsageEntity
import com.flxrs.dankchat.data.database.entity.MessageHighlightEntity
import com.flxrs.dankchat.data.database.entity.MessageIgnoreEntity
import com.flxrs.dankchat.data.database.entity.UploadEntity
import com.flxrs.dankchat.data.database.entity.UserDisplayEntity
import com.flxrs.dankchat.data.database.entity.UserHighlightEntity
import com.flxrs.dankchat.data.database.entity.UserIgnoreEntity

@Database(
    version = 7,
    entities = [
        BadgeHighlightEntity::class,
        EmoteUsageEntity::class,
        UploadEntity::class,
        MessageHighlightEntity::class,
        MessageIgnoreEntity::class,
        UserHighlightEntity::class,
        UserIgnoreEntity::class,
        BlacklistedUserEntity::class,
        UserDisplayEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
    ],
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
abstract class DankChatDatabase : RoomDatabase() {
    abstract fun badgeHighlightDao(): BadgeHighlightDao

    abstract fun emoteUsageDao(): EmoteUsageDao

    abstract fun recentUploadsDao(): RecentUploadsDao

    abstract fun userDisplayDao(): UserDisplayDao

    abstract fun messageHighlightDao(): MessageHighlightDao

    abstract fun userHighlightDao(): UserHighlightDao

    abstract fun userIgnoreDao(): UserIgnoreDao

    abstract fun messageIgnoreDao(): MessageIgnoreDao

    abstract fun blacklistedUserDao(): BlacklistedUserDao

    companion object {
        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE user_highlight ADD COLUMN create_notification INTEGER DEFAULT 1 NOT NUll")
                    db.execSQL("ALTER TABLE message_highlight ADD COLUMN create_notification INTEGER DEFAULT 0 NOT NUll")
                    db.execSQL("UPDATE message_highlight SET create_notification=1 WHERE type = 'Username' OR type = 'Custom'")
                }
            }
    }
}
