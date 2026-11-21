package com.example.aulasegura.domain.model

/**
 * The fixed school catalog. Grades and sections are a domain decision, not data
 * read from Firestore: with an empty database the director can still assign a
 * classroom, and the group document is created on demand.
 */
object SchoolCatalog {
    val GRADES: List<Int> = (1..6).toList()
    val SECTIONS: List<String> = listOf("A", "B", "C", "D")
}

val AvailableGrades: List<Int> get() = SchoolCatalog.GRADES
val AvailableSections: List<String> get() = SchoolCatalog.SECTIONS

fun isValidGrade(grade: Int): Boolean = grade in SchoolCatalog.GRADES

fun isValidSection(section: String): Boolean =
    section.trim().uppercase() in SchoolCatalog.SECTIONS

/** Canonical, idempotent id: the same grade+section is always the same document. */
fun groupIdFor(grade: Int, section: String): String =
    "grp-$grade${section.trim().lowercase()}"

/** Canonical display name, e.g. "3° B". */
fun groupNameFor(grade: Int, section: String): String =
    "$grade° ${section.trim().uppercase()}"
