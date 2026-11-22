package com.example.aulasegura.data.local

import android.content.Context
import androidx.room.Room

/**
 * Process-wide Room instance.
 *
 * A double-checked singleton is enough for Fase 0; Bloque G replaces it with
 * the dependency-injection container. No `fallbackToDestructiveMigration` is
 * set on purpose: silently dropping the outbox would lose a teacher's offline
 * attendance, so a real schema change must ship a migration.
 */
object DatabaseProvider {

    @Volatile
    private var instance: AulaSeguraDatabase? = null

    fun get(context: Context): AulaSeguraDatabase =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): AulaSeguraDatabase =
        Room.databaseBuilder(
            context,
            AulaSeguraDatabase::class.java,
            AulaSeguraDatabase.DATABASE_NAME,
        )
            .addMigrations(
                AulaSeguraDatabase.MIGRATION_1_2,
                AulaSeguraDatabase.MIGRATION_2_3,
                AulaSeguraDatabase.MIGRATION_3_4,
                AulaSeguraDatabase.MIGRATION_4_5,
                AulaSeguraDatabase.MIGRATION_5_6,
                AulaSeguraDatabase.MIGRATION_6_7,
            )
            .build()
}
