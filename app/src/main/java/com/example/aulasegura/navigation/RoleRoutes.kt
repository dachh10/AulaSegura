package com.example.aulasegura.navigation

import com.example.aulasegura.domain.model.UserRole

/**
 * Home destination for each role. Single place mapping a domain role to a route,
 * used by Splash right after resolving the session.
 */
fun UserRole.homeRoute(): String = when (this) {
    UserRole.TEACHER -> Routes.TEACHER_HOME
    UserRole.ADMIN -> Routes.ADMIN_DASHBOARD
    UserRole.TUTOR -> Routes.PARENT_HOME
    UserRole.UNKNOWN -> Routes.LOGIN
}
