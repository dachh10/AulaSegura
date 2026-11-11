package com.example.aulasegura.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.aulasegura.data.local.dao.AttendanceDao
import com.example.aulasegura.data.local.dao.GroupDao
import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.local.dao.NoticeDao
import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.data.local.dao.StudentDao
import com.example.aulasegura.data.local.dao.TaskDao
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.GroupEntity
import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.NoticeEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
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
    ],
    version = 1,
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

    companion object {
        const val DATABASE_NAME = "aulasegura.db"
    }
}
