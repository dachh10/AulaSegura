package com.example.aulasegura.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.aulasegura.data.local.dao.AttendanceDao
import com.example.aulasegura.data.local.dao.GroupDao
import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.local.dao.NoticeDao
import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.data.local.dao.ProfileDao
import com.example.aulasegura.data.local.dao.StudentDao
import com.example.aulasegura.data.local.dao.TaskDao
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.GroupEntity
import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.NoticeEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.ProfileEntity
import com.example.aulasegura.data.local.entity.StudentEntity
import com.example.aulasegura.data.local.entity.TaskEntity

/**
 * Room is the single local source of truth: Firestore persistent cache is off
 * (`AulaSeguraApp`), so every offline read and every offline write goes through
 * this database.
 *
 * The schema is exported to `app/schemas/` so a migration can be written and
 * tested against a real version, instead of guessing the old column list.
 */
@Database(
    entities = [
        GroupEntity::class,
        StudentEntity::class,
        AttendanceSessionEntity::class,
        AttendanceRecordEntity::class,
        IncidentDraftEntity::class,
        IncidentMediaEntity::class,
        NoticeEntity::class,
        TaskEntity::class,
        OutboxEntity::class,
        ProfileEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AulaSeguraDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao

    abstract fun studentDao(): StudentDao

    abstract fun attendanceDao(): AttendanceDao

    abstract fun incidentDao(): IncidentDao

    abstract fun noticeDao(): NoticeDao

    abstract fun taskDao(): TaskDao

    abstract fun outboxDao(): OutboxDao

    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "aulasegura.db"

        /**
         * Adds the profile cache. No `fallbackToDestructiveMigration` anywhere:
         * dropping the database would erase the outbox and the teacher's offline
         * attendance with it.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (" +
                        "`uid` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`fullName` TEXT NOT NULL, " +
                        "`schoolId` TEXT NOT NULL, " +
                        "`active` INTEGER NOT NULL, " +
                        "`photoUrl` TEXT, " +
                        "`subjects` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`uid`))",
                )
            }
        }
    }
}
