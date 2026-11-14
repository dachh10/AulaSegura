package com.example.aulasegura.data.local.mapper

import com.example.aulasegura.data.local.entity.GroupEntity
import com.example.aulasegura.data.local.entity.StudentEntity
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.Student
import com.google.firebase.Timestamp

/**
 * The only place where the Firestore wire format meets Room and the domain.
 *
 * The functions take a raw `Map<String, Any?>` instead of a `DocumentSnapshot`
 * because a snapshot cannot be built in a unit test. With plain maps the mapping
 * — which is where the real bugs live: a number that arrives as `Double`, a
 * missing timestamp, a list of the wrong type — is testable without Firebase.
 *
 * A document that cannot be identified is skipped (`null`) rather than cached
 * with empty fields: a group without a name would render as a blank card and
 * nobody would know which document to fix.
 */

fun groupEntityFrom(id: String, data: Map<String, Any?>): GroupEntity? {
    val name = data["name"] as? String ?: return null
    val schoolId = data["schoolId"] as? String ?: return null

    return GroupEntity(
        groupId = id,
        name = name,
        grade = (data["grade"] as? Number)?.toInt() ?: 0,
        section = data["section"] as? String ?: "",
        schoolId = schoolId,
        titularTeacherId = data["titularTeacherId"] as? String,
        teacherIds = stringList(data["teacherIds"]),
        studentCount = (data["studentCount"] as? Number)?.toInt() ?: 0,
        cycle = data["cycle"] as? String ?: "",
        createdAt = millis(data["createdAt"]),
        updatedAt = millis(data["updatedAt"]),
    )
}

fun studentEntityFrom(id: String, data: Map<String, Any?>): StudentEntity? {
    val fullName = data["fullName"] as? String ?: return null
    val groupId = data["groupId"] as? String ?: return null
    val schoolId = data["schoolId"] as? String ?: return null

    return StudentEntity(
        studentId = id,
        firstName = data["firstName"] as? String ?: fullName.substringBefore(' '),
        lastName = data["lastName"] as? String ?: fullName.substringAfter(' ', ""),
        fullName = fullName,
        curp = data["curp"] as? String,
        listNumber = (data["listNumber"] as? Number)?.toInt() ?: 0,
        groupId = groupId,
        schoolId = schoolId,
        tutorIds = stringList(data["tutorIds"]),
        photoUrl = data["photoUrl"] as? String,
        photoPublicId = data["photoPublicId"] as? String,
        active = data["active"] as? Boolean ?: true,
        createdAt = millis(data["createdAt"]),
        updatedAt = millis(data["updatedAt"]),
    )
}

fun GroupEntity.toDomain(): Group = Group(
    id = groupId,
    name = name,
    grade = grade,
    section = section,
    schoolId = schoolId,
    titularTeacherId = titularTeacherId,
    teacherIds = teacherIds,
    studentCount = studentCount,
    cycle = cycle,
)

fun StudentEntity.toDomain(): Student = Student(
    id = studentId,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    listNumber = listNumber,
    groupId = groupId,
    schoolId = schoolId,
    active = active,
    photoUrl = photoUrl,
)

/**
 * A missing timestamp maps to `0`, never to "now".
 *
 * "Now" would make a document with no `updatedAt` look fresher than one that was
 * just edited, which is exactly backwards for a cache.
 */
private fun millis(value: Any?): Long =
    (value as? Timestamp)?.toEpochMillis() ?: 0L

private fun stringList(value: Any?): List<String> =
    (value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
