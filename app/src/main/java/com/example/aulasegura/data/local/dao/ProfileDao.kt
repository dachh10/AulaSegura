package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.ProfileEntity

/**
 * The signed-in profile cache. At most one row exists at a time: a sign-in
 * clears whatever was there and writes the new profile, so a shared school
 * device never keeps a previous teacher's name on disk.
 */
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile WHERE uid = :uid LIMIT 1")
    suspend fun findById(uid: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()
}
