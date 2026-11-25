package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.Subject
import com.example.aulasegura.domain.model.TeacherSubjectAssignment
import com.example.aulasegura.domain.model.subjectById
import com.example.aulasegura.domain.model.subjectsForGrade

/** Subjects of a grade that still have no teacher in this group. */
internal fun unassignedSubjectsForGrade(
    grade: Int,
    subjectTeachers: Map<String, String>,
): List<Subject> = subjectsForGrade(grade).filter { it.id !in subjectTeachers }

/**
 * The profile's subject list is a projection of the group slots, never a
 * separate source of truth: assigning a teacher to another group cannot change
 * what they teach in the first one.
 */
internal fun teacherSubjectProjection(
    subjectsByGroup: Map<String, Map<String, String>>,
    teacherUid: String,
): List<TeacherSubjectAssignment> = subjectsByGroup.flatMap { (groupId, slots) ->
    slots.filter { it.value == teacherUid }.map { (subjectId, _) ->
        TeacherSubjectAssignment(
            groupId = groupId,
            subjectId = subjectId,
            label = subjectById(subjectId)?.label.orEmpty(),
        )
    }
}.sortedWith(compareBy({ it.groupId }, { it.subjectId }))

/** Removes one teacher's slot, leaving the other subjects of the group intact. */
internal fun withoutTeacher(
    subjectTeachers: Map<String, String>,
    teacherUid: String,
): Map<String, String> = subjectTeachers.filterValues { it != teacherUid }
