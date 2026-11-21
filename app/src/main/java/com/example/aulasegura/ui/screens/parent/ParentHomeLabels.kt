package com.example.aulasegura.ui.screens.parent

/** The chip subtitle: honest about a child still waiting for a classroom. */
fun childGradeLabel(groupId: String, groupLabel: String): String = when {
    groupId.isBlank() -> "Grupo por asignar"
    groupLabel.isNotBlank() -> groupLabel
    else -> groupId
}

/** The card status never invents a 0% figure for a child with no records yet. */
fun childAttendanceStatus(percent: Int?): String =
    percent?.let { "$it%" } ?: "Sin datos aún"
