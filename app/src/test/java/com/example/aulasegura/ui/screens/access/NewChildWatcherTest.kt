package com.example.aulasegura.ui.screens.access

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: the first live emission must never be mistaken for a new child. */
class NewChildWatcherTest {

    @Test
    fun `first emission seeds the baseline on a cold start`() {
        val watcher = NewChildWatcher(emptySet())

        assertFalse(watcher.hasNewChild(listOf("stu-andres")))
        assertFalse(watcher.hasNewChild(listOf("stu-andres")))
    }

    @Test
    fun `a child added after the first emission is detected`() {
        val watcher = NewChildWatcher(emptySet())

        assertFalse(watcher.hasNewChild(listOf("stu-andres")))
        assertTrue(watcher.hasNewChild(listOf("stu-andres", "stu-enrique")))
    }

    @Test
    fun `a warm baseline from the UI state is honored`() {
        val watcher = NewChildWatcher(setOf("stu-andres"))

        assertFalse(watcher.hasNewChild(listOf("stu-andres")))
        assertTrue(watcher.hasNewChild(listOf("stu-andres", "stu-enrique")))
    }
}
