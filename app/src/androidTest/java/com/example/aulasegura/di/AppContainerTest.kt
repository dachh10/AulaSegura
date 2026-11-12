package com.example.aulasegura.di

import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.ui.screens.access.ForgotPasswordViewModel
import com.example.aulasegura.ui.screens.access.LoginViewModel
import com.example.aulasegura.ui.screens.access.SessionViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The container is the one place where wiring mistakes hide until a screen
 * crashes, so it gets its own test: it exists after startup, it hands out
 * singletons, and the factory can build every ViewModel the access flow asks for.
 */
@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()

    @Test
    fun containerExistsAfterStartup() {
        assertNotNull(app.container)
    }

    @Test
    fun dependenciesAreSingleInstances() {
        val container = app.container

        assertSame(container.database, container.database)
        assertSame(container.authRepository, container.authRepository)
        assertSame(container.outboxRepository, container.outboxRepository)
        assertSame(container.viewModelFactory, container.viewModelFactory)
    }

    @Test
    fun factoryBuildsEveryAccessViewModel() {
        val factory = app.container.viewModelFactory

        assertTrue(factory.create(SessionViewModel::class.java) is SessionViewModel)
        assertTrue(factory.create(LoginViewModel::class.java) is LoginViewModel)
        assertTrue(factory.create(ForgotPasswordViewModel::class.java) is ForgotPasswordViewModel)
    }

    @Test(expected = IllegalStateException::class)
    fun unknownViewModelFailsLoudly() {
        app.container.viewModelFactory.create(UnregisteredViewModel::class.java)
    }

    /** Stands in for a screen that forgot to register its ViewModel. */
    private class UnregisteredViewModel : ViewModel()
}
