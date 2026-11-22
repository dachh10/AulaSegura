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
import com.example.aulasegura.data.local.dao.SchoolDao
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
import com.example.aulasegura.data.local.entity.SchoolEntity
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
        SchoolEntity::class,
    ],
    version = 7,
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

    abstract fun schoolDao(): SchoolDao

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

        /**
         * Adds the school cache, which is what makes the geofence work offline.
         *
         * No data is migrated and nothing is dropped: the table starts empty and
         * the next refresh fills it. Until then the gate evaluates `NO_SCHOOL`,
         * which leaves the roll call unverified instead of blocking it.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `schools` (" +
                        "`schoolId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`latitude` REAL, " +
                        "`longitude` REAL, " +
                        "`radiusMeters` REAL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`schoolId`))",
                )
            }
        }

        /**
         * Adds the directora's GPS opt-out to the school cache.
         *
         * `DEFAULT 1` makes every existing row keep requiring GPS, which is the
         * behaviour those rows were written under. Nothing is dropped and no
         * other table is touched: the outbox and the teacher's offline
         * attendance survive the upgrade.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `schools` ADD COLUMN `requireLocation` INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        /**
         * Adds the linking-flow fields to the roster cache. Every column has a
         * default matching the value those rows were written under, so existing
         * students stay synced and stay visible to the teacher. Nothing is
         * dropped and no other table is touched: the outbox survives the upgrade.
         */
        /**
         * Caches the account state and the tutor's linking data. Without these
         * columns an offline start rebuilt a user with no `vinculacion` and no
         * `hijoSolicitado`, so the QR disappeared until the network came back.
         * Defaults keep every existing row valid; nothing is dropped.
         */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `profile` ADD COLUMN `estado` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `profile` ADD COLUMN `vinculacion` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `profile` ADD COLUMN `hijoSolicitado` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `profile` ADD COLUMN `fechaSolicitud` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /** Carries the configured roll-call window into the teacher's cache. */
        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `schools` ADD COLUMN `entryTime` TEXT")
                db.execSQL("ALTER TABLE `schools` ADD COLUMN `exitTime` TEXT")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `students` ADD COLUMN `grado` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `students` ADD COLUMN `grupo` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `students` ADD COLUMN `creadoPor` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `students` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'",
                )
            }
        }
    }
}
