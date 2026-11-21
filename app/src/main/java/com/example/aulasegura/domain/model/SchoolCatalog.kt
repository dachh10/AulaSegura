package com.example.aulasegura.domain.model

/**
 * The fixed school catalog. Grades and sections are a domain decision, not data
 * read from Firestore: with an empty database the director can still assign a
 * classroom, and the group document is created on demand.
 */
object SchoolCatalog {
    val GRADES: List<Int> = (1..6).toList()
    val SECTIONS: List<String> = listOf("A", "B", "C", "D")

    /** One subject per classroom teacher. The id is the stable key on a group. */
    val SUBJECTS: List<Subject> = listOf(
        Subject("espanol", "Español"),
        Subject("matematicas", "Matemáticas"),
        Subject("lectura", "Lectura"),
        Subject("historia", "Historia"),
        Subject("geografia", "Geografía"),
        Subject("ciencias", "Ciencias"),
    )
}

data class Subject(val id: String, val label: String)

fun subjectById(id: String): Subject? = SchoolCatalog.SUBJECTS.firstOrNull { it.id == id }

/** Accepts the canonical id or the displayed label, case-insensitively. */
fun subjectIdFromLabel(value: String): String? = SchoolCatalog.SUBJECTS.firstOrNull {
    it.id.equals(value.trim(), ignoreCase = true) ||
        it.label.equals(value.trim(), ignoreCase = true)
}?.id

val AvailableGrades: List<Int> get() = SchoolCatalog.GRADES
val AvailableSections: List<String> get() = SchoolCatalog.SECTIONS

fun isValidGrade(grade: Int): Boolean = grade in SchoolCatalog.GRADES

fun isValidSection(section: String): Boolean =
    section.trim().uppercase() in SchoolCatalog.SECTIONS

/** Canonical, idempotent id: the same grade+section is always the same document. */
fun groupIdFor(grade: Int, section: String): String =
    "grp-$grade${section.trim().lowercase()}"

/** Parses the canonical id back into grade + section, for group creation. */
fun gradeSectionFromGroupId(groupId: String): Pair<Int, String>? {
    val match = Regex("^grp-([1-6])([a-dA-D])$").matchEntire(groupId.trim()) ?: return null
    return match.groupValues[1].toInt() to match.groupValues[2].uppercase()
}

/** Canonical display name, e.g. "3° B". */
fun groupNameFor(grade: Int, section: String): String =
    "$grade° ${section.trim().uppercase()}"

/** Whether a teacher may take a subject slot in a group. */
enum class SubjectAvailability { FREE, TAKEN_BY_OTHER, INVALID }

/**
 * `subjectTeachers` maps a canonical subject id to the uid that owns it.
 * A teacher re-taking their own slot is free; another teacher's slot is not.
 */
fun subjectAvailability(
    subjectTeachers: Map<String, String>,
    subjectId: String,
    currentUid: String,
): SubjectAvailability = when {
    subjectById(subjectId) == null -> SubjectAvailability.INVALID
    else -> when (val owner = subjectTeachers[subjectId]) {
        null, currentUid -> SubjectAvailability.FREE
        else -> SubjectAvailability.TAKEN_BY_OTHER
    }
}

/** `null` means the grade + section + subject selection is complete. */
fun teacherAssignmentError(grade: Int?, section: String, subjectId: String): String? = when {
    grade == null || !isValidGrade(grade) -> "Elige el grado del grupo."
    !isValidSection(section) -> "Elige la sección del grupo."
    !isValidSubject(subjectId) -> "Elige la materia que impartes."
    else -> null
}

fun isValidSubject(subjectId: String): Boolean = subjectById(subjectId) != null
