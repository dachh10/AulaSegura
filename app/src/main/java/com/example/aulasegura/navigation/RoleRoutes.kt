package com.example.aulasegura.navigation

import com.example.aulasegura.domain.model.AppUser
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

/**
 * Destination after a real sign-in. Email verification is handled before this
 * point; here the account state decides whether the user may enter the app or
 * has to wait on the review / linking screen.
 */
fun AppUser.destinationRoute(): String = when (role) {
    UserRole.ADMIN -> Routes.ADMIN_DASHBOARD
    UserRole.TEACHER ->
        if (estado == "aprobado") Routes.TEACHER_HOME else Routes.ACCOUNT_REVIEW
    UserRole.TUTOR ->
        if (vinculacion == "vinculado") Routes.PARENT_HOME else Routes.LINK_CHILD
    UserRole.UNKNOWN -> Routes.LOGIN
}
