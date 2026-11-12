package com.example.aulasegura.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the two properties the block promises: the sync work only runs with
 * connectivity, and asking for a sync twice does not create a second worker
 * (that is the "no duplicates" half of the criterion).
 */
@RunWith(AndroidJUnit4::class)
class SyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork()
    }

    @Test
    fun schedulesOneWorkerThatRequiresNetwork() = runBlocking {
        SyncScheduler(context).schedule()

        val infos = activeWorkInfos()
        assertEquals(1, infos.size)

        val info = infos.first()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
        assertTrue(info.tags.contains(SyncScheduler.TAG))
    }

    @Test
    fun schedulingAgainKeepsTheExistingWorker() = runBlocking {
        SyncScheduler(context).schedule()
        val firstId = activeWorkInfos().single().id

        SyncScheduler(context).schedule()

        val afterSecond = activeWorkInfos()
        assertEquals("KEEP must not stack workers", 1, afterSecond.size)
        assertEquals(firstId, afterSecond.single().id)
    }

    /** Work finished by an earlier test stays in the database; only live work matters. */
    private fun activeWorkInfos(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(SyncScheduler.UNIQUE_WORK_NAME).get()
            .filterNot { it.state.isFinished }
}
