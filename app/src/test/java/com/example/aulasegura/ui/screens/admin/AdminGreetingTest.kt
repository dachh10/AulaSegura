package com.example.aulasegura.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Test

/** The dashboard greeting must never assume the director's gender. */
class AdminGreetingTest {

    @Test
    fun `the greeting uses the name without a gendered title`() {
        assertEquals(
            "Hola, Diego Adrian Corral Hernandez",
            adminGreeting("Diego Adrian Corral Hernandez"),
        )
    }

    @Test
    fun `a blank name still greets`() {
        assertEquals("Hola", adminGreeting("  "))
    }
}
