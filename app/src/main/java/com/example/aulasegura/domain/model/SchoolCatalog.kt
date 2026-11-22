package com.example.aulasegura.domain.model

/**
 * The fixed school catalog. Grades and sections are a domain decision, not data
 * read from Firestore: with an empty database the director can still assign a
 * classroom, and the group document is created on demand.
 */
object SchoolCatalog {
    val GRADES: List<Int> = (1..6).toList()
    val SECTIONS: List<String> = listOf("A", "B", "C", "D")

    /**
     * Each grade has its own six subjects. The slug is the stable Firestore key
     * inside `groups/{id}.subjectTeachers`; a group has one grade, so the six
     * keys are unique there even though two grades share a subject name.
     */
    val SUBJECTS_BY_GRADE: Map<Int, List<Subject>> = mapOf(
        1 to listOf(
            Subject("g1-espanol-i", "Español I", 1),
            Subject("g1-matematicas-i", "Matemáticas I", 1),
            Subject("g1-exploracion-naturaleza", "Exploración de la Naturaleza", 1),
            Subject("g1-convivencia-valores", "Convivencia y Valores (Cívica I)", 1),
            Subject("g1-educacion-fisica-i", "Educación Física I", 1),
            Subject("g1-artes-i", "Artes I", 1),
        ),
        2 to listOf(
            Subject("g2-espanol-ii", "Español II", 2),
            Subject("g2-matematicas-ii", "Matemáticas II", 2),
            Subject("g2-conocimiento-medio", "Conocimiento del Medio", 2),
            Subject("g2-formacion-civica-ii", "Formación Cívica y Ética II", 2),
            Subject("g2-ingles-basico", "Inglés Básico", 2),
            Subject("g2-educacion-socioemocional", "Educación Socioemocional", 2),
        ),
        3 to listOf(
            Subject("g3-espanol-iii", "Español III", 3),
            Subject("g3-matematicas-iii", "Matemáticas III", 3),
            Subject("g3-ciencias-naturales", "Ciencias Naturales", 3),
            Subject("g3-entidad-donde-vivo", "La Entidad donde Vivo", 3),
            Subject("g3-formacion-civica-iii", "Formación Cívica y Ética III", 3),
            Subject("g3-educacion-fisica-deporte", "Educación Física y Deporte", 3),
        ),
        4 to listOf(
            Subject("g4-espanol-iv", "Español IV", 4),
            Subject("g4-matematicas-iv", "Matemáticas IV", 4),
            Subject("g4-ciencias-naturales-iv", "Ciencias Naturales IV", 4),
            Subject("g4-historia-mexico-i", "Historia de México I", 4),
            Subject("g4-geografia-mexico", "Geografía de México", 4),
            Subject("g4-formacion-civica-iv", "Formación Cívica y Ética IV", 4),
        ),
        5 to listOf(
            Subject("g5-espanol-v", "Español V", 5),
            Subject("g5-matematicas-v", "Matemáticas V", 5),
            Subject("g5-ciencias-naturales-v", "Ciencias Naturales V", 5),
            Subject("g5-historia-mexico-ii", "Historia de México II", 5),
            Subject("g5-geografia-universal-i", "Geografía Universal I", 5),
            Subject("g5-tecnologia-computacion", "Tecnología / Computación", 5),
        ),
        6 to listOf(
            Subject("g6-espanol-vi", "Español VI", 6),
            Subject("g6-matematicas-vi", "Matemáticas VI", 6),
            Subject("g6-ciencias-naturales-vi", "Ciencias Naturales VI", 6),
            Subject("g6-historia-universal", "Historia Universal", 6),
            Subject("g6-geografia-universal-ii", "Geografía Universal II", 6),
            Subject("g6-formacion-civica-vi", "Formación Cívica y Ética VI", 6),
        ),
    )

    val SUBJECTS: List<Subject> = GRADES.flatMap { SUBJECTS_BY_GRADE.getValue(it) }
}

data class Subject(val id: String, val label: String, val grade: Int = 0)

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

/** The six subjects a grade actually teaches. */
fun subjectsForGrade(grade: Int): List<Subject> =
    SchoolCatalog.SUBJECTS_BY_GRADE[grade] ?: emptyList()

fun subjectById(id: String): Subject? = SchoolCatalog.SUBJECTS.firstOrNull { it.id == id }

/**
 * Accepts the canonical id, the full label, or a short label such as "Español"
 * for that grade's "Español III". A grade is required to disambiguate.
 */
fun subjectIdFromLabel(value: String, grade: Int? = null): String? {
    val text = value.trim()
    val candidates = grade?.let { subjectsForGrade(it) } ?: SchoolCatalog.SUBJECTS
    return candidates.firstOrNull { subject ->
        subject.id.equals(text, ignoreCase = true) ||
            subject.label.equals(text, ignoreCase = true) ||
            subject.label.startsWith("$text ", ignoreCase = true)
    }?.id
}

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
    subjectId.isBlank() || subjectId !in subjectsForGrade(grade).map { it.id } ->
        "Elige la materia que impartes."
    else -> null
}

fun isValidSubject(subjectId: String): Boolean = subjectById(subjectId) != null
