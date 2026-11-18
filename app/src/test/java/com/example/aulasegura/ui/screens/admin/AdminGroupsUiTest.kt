package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminScheduleSlot
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AdminGroupError
import com.example.aulasegura.domain.repository.AdminGroupFields
import com.example.aulasegura.domain.repository.AdminGroupResult
import com.example.aulasegura.domain.repository.AdminUserFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The grade filter, the form guard, the new-group document and the assignment
 * description are all pure functions. These tests are the regression guard for the
 * block's invariant: a teacher assignment must name BOTH sides of the link.
 */
class AdminGroupsUiTest {

    private val groups = listOf(
        group("g-1a", grade = 1),
        group("g-2c", grade = 2),
        group("g-3b", grade = 3),
        group("g-bad", grade = 0), // malformed document: still a row, never a crash
    )

    private fun group(id: String, grade: Int) = AdminGroup(
        id = id,
        name = id,
        grade = grade,
        section = "B",
        schoolId = "school-1",
        teacherIds = emptyList(),
        titularTeacherId = "",
        studentCount = 0,
        cycle = AdminGroupFields.DEFAULT_CYCLE,
    )

    @Test
    fun `grade filter returns every grade for null and selects one otherwise`() {
        assertEquals(4, filterGroupsByGrade(groups, null).size)
        assertEquals(listOf("g-1a"), filterGroupsByGrade(groups, 1).map { it.id })
        assertEquals(listOf("g-2c"), filterGroupsByGrade(groups, 2).map { it.id })
        assertEquals(listOf("g-3b"), filterGroupsByGrade(groups, 3).map { it.id })
        assertTrue(filterGroupsByGrade(groups, 9).isEmpty())
    }

    @Test
    fun `validation rejects an invalid grade and blank name or section`() {
        val valid = AdminGroupForm(name = "3° B", grade = "3", section = "B")
        assertNull(validationError(valid))
        assertTrue(isValid(valid))

        assertTrue(validationError(valid.copy(name = "  "))!!.contains("nombre"))
        assertTrue(validationError(valid.copy(section = ""))!!.contains("sección"))
        assertTrue(validationError(valid.copy(grade = "7"))!!.contains("grado"))
        assertTrue(validationError(valid.copy(grade = "abc"))!!.contains("grado"))
        assertTrue(validationError(valid.copy(grade = "0"))!!.contains("grado"))
        assertFalse(isValid(valid.copy(grade = "")))

        assertEquals(3, parseGrade(" 3 "))
        assertNull(parseGrade("7"))
    }

    @Test
    fun `new group document carries schoolId, zero studentCount and the default cycle`() {
        val form = AdminGroupForm(name = "  3° B  ", grade = "3", section = " B ")
        val doc = buildGroupDocument(form, "school-1")

        assertEquals("3° B", doc[AdminGroupFields.NAME])
        assertEquals(3, doc[AdminGroupFields.GRADE])
        assertEquals("B", doc[AdminGroupFields.SECTION])
        assertEquals("school-1", doc[AdminGroupFields.SCHOOL_ID])
        assertEquals(0, doc[AdminGroupFields.STUDENT_COUNT])
        assertEquals("2026-2027", doc[AdminGroupFields.CYCLE])
        assertEquals(emptyList<String>(), doc[AdminGroupFields.TEACHER_IDS])
        // The legacy titular field is no longer written on new groups.
        assertFalse(doc.containsKey("titularTeacherId"))
        assertFalse(doc.containsKey(AdminGroupFields.CREATED_AT))
    }

    @Test
    fun `group update map carries only editable fields and trims values`() {
        val map = buildGroupUpdateMap(
            AdminGroupForm(name = "  3° B  ", grade = " 3 ", section = " B "),
        )

        assertEquals(3, map.size)
        assertEquals("3° B", map[AdminGroupFields.NAME])
        assertEquals(3, map[AdminGroupFields.GRADE])
        assertEquals("B", map[AdminGroupFields.SECTION])
        assertFalse(map.containsKey(AdminGroupFields.SCHOOL_ID))
        assertFalse(map.containsKey(AdminGroupFields.STUDENT_COUNT))
        assertFalse(map.containsKey(AdminGroupFields.UPDATED_AT))
    }

    @Test
    fun `group edit validation rejects blank fields and grades outside 1 to 6`() {
        assertTrue(validationError(AdminGroupForm("  ", "3", "B"))!!.contains("nombre"))
        assertTrue(validationError(AdminGroupForm("3° B", "7", "B"))!!.contains("grado"))
        assertTrue(validationError(AdminGroupForm("3° B", "0", "B"))!!.contains("grado"))
        assertTrue(validationError(AdminGroupForm("3° B", "3", " "))!!.contains("sección"))
        assertNull(validationError(AdminGroupForm("3° B", "6", "B")))
        assertEquals(
            "El grupo todavía tiene alumnos.",
            AdminGroupError.HAS_STUDENTS.toMessage(),
        )
    }

    @Test
    fun `schedule mapping is deterministic and driven by weekday plus start time`() {
        val first = AdminScheduleSlot(weekday = 1, startTime = "08:00", endTime = "08:45", subject = "Matemáticas", teacherId = "u-1")
        val sameKey = first.copy(subject = "Español")
        val otherTime = first.copy(startTime = "09:00")

        val docs = buildSlotDocuments(listOf(first, sameKey))
        assertEquals("1-0800", docs[0].first)
        assertEquals("1-0800", docs[1].first)
        assertEquals(docs, buildSlotDocuments(listOf(first, sameKey)))
        assertEquals("Matemáticas", docs[0].second[AdminGroupFields.SUBJECT])
        assertEquals("08:45", docs[0].second[AdminGroupFields.END_TIME])
        assertEquals("u-1", docs[0].second[AdminGroupFields.TEACHER_ID])
        assertEquals("1-0900", AdminGroupFields.slotId(1, otherTime.startTime))
        assertEquals("2-0800", AdminGroupFields.slotId(2, first.startTime))
    }

    @Test
    fun `assignment description names BOTH the group and the user path, never a titular`() {
        val assign = buildAssignmentDescription("g-3b", "u-1", assign = true)

        assertEquals("groups/g-3b", assign.groupPath)
        assertEquals("users/u-1", assign.userPath)
        assertEquals(AdminGroupFields.TEACHER_IDS, assign.groupField)
        assertEquals(AdminUserFields.GROUP_IDS, assign.userField)
        assertEquals("arrayUnion", assign.operation)
        assertTrue(assign.summary.contains("groups/g-3b.teacherIds"))
        assertTrue(assign.summary.contains("users/u-1.groupIds"))
        assertFalse(assign.summary.contains("titular"))

        val unassign = buildAssignmentDescription("g-3b", "u-1", assign = false)
        assertEquals("arrayRemove", unassign.operation)
        assertEquals("groups/g-3b", unassign.groupPath)
        assertEquals("users/u-1", unassign.userPath)
        assertFalse(unassign.summary.contains("titular"))
    }

    @Test
    fun `delete verdict splits empty groups from groups that need a destination`() {
        assertTrue(canDeleteGroupDirectly(0))
        assertTrue(canDeleteGroupDirectly(-1))
        assertFalse(needsDestinationGroup(0))

        assertTrue(needsDestinationGroup(1))
        assertTrue(needsDestinationGroup(27))
        assertFalse(canDeleteGroupDirectly(27))
    }

    @Test
    fun `deletion description moves students, both counters and the schedule in one batch`() {
        val plan = describeGroupDeletion(
            sourceGroupId = "g-3b",
            destinationGroupId = "g-3c",
            studentIds = listOf("stu-3b-01", "stu-3b-02"),
            scheduleSlotIds = listOf("1-0800", "3-1000"),
        )

        assertTrue(plan.movesStudents)
        assertFalse(plan.canDeleteDirectly)
        assertEquals(listOf("students/stu-3b-01", "students/stu-3b-02"), plan.studentPaths)
        assertEquals(
            listOf("groups/g-3b/schedule/1-0800", "groups/g-3b/schedule/3-1000"),
            plan.schedulePaths,
        )
        assertEquals(-2, plan.sourceCountDelta)
        assertEquals(2, plan.destinationCountDelta)

        val summary = plan.summary
        assertTrue(summary.contains("update students/stu-3b-01.groupId = g-3c"))
        assertTrue(summary.contains("groups/g-3b.studentCount -2"))
        assertTrue(summary.contains("groups/g-3c.studentCount +2"))
        assertTrue(summary.contains("delete groups/g-3b/schedule/1-0800"))
        assertTrue(summary.endsWith("delete groups/g-3b"))
    }

    @Test
    fun `deletion description of an empty group has no student moves`() {
        val plan = describeGroupDeletion("g-3b", null, emptyList(), listOf("1-0800"))

        assertTrue(plan.canDeleteDirectly)
        assertFalse(plan.movesStudents)
        assertEquals(0, plan.sourceCountDelta)
        assertEquals(0, plan.destinationCountDelta)
        assertFalse(plan.summary.contains("students/"))
        assertEquals("delete groups/g-3b/schedule/1-0800; delete groups/g-3b", plan.summary)
    }

    @Test
    fun `group failure detail is appended when present and omitted otherwise`() {
        val detail = "permission-denied: Missing or insufficient permissions."
        val withDetail = AdminGroupResult.Failure(AdminGroupError.DENIED, detail)

        assertEquals(
            "No tienes permiso para realizar esta acción. (Firestore: $detail)",
            withDetail.toMessage(),
        )

        val withoutDetail = AdminGroupResult.Failure(AdminGroupError.DENIED)
        assertEquals("No tienes permiso para realizar esta acción.", withoutDetail.toMessage())
    }

    @Test
    fun `destination failures have explicit Spanish copy`() {
        assertTrue(AdminGroupError.DESTINATION_REQUIRED.toMessage().contains("grupo de destino"))
        assertTrue(AdminGroupError.DESTINATION_SAME_AS_SOURCE.toMessage().contains("distinto"))
        assertTrue(AdminGroupError.DESTINATION_NOT_FOUND.toMessage().contains("escuela"))
    }

    @Test
    fun `malformed Firestore documents fall back instead of crashing the list`() {
        val group = AdminGroup.fromDocument("g-1", mapOf("name" to 123, "grade" to "one", "teacherIds" to "not-a-list"))
        assertEquals("g-1", group.name)
        assertEquals(0, group.grade)
        assertTrue(group.teacherIds.isEmpty())
        assertEquals("", group.schoolId)

        val slot = AdminScheduleSlot.fromDocument("1-0800", mapOf("weekday" to null, "subject" to 7))
        assertEquals(1, slot.weekday)
        assertEquals("", slot.subject)
        assertEquals("1-0800", slot.id)
    }

    private fun user(
        uid: String,
        role: UserRole,
        subjects: List<String> = emptyList(),
    ) = AdminUser(
        uid = uid,
        email = "$uid@escuela.mx",
        role = role,
        firstName = "Nombre",
        lastName = "Apellido",
        fullName = "Nombre Apellido",
        phone = "",
        schoolId = "school-1",
        subjects = subjects,
        groupIds = emptyList(),
        studentIds = emptyList(),
        active = true,
    )

    @Test
    fun `subject options come only from the selected teacher, trimmed and distinct`() {
        val users = listOf(
            user("u-1", UserRole.TEACHER, listOf(" Matemáticas ", "Álgebra", "Matemáticas", " ")),
            user("u-2", UserRole.TEACHER, listOf("Español")),
            user("u-3", UserRole.ADMIN, listOf("Dirección")),
        )

        assertEquals(listOf("Matemáticas", "Álgebra"), subjectOptionsForTeacher("u-1", users))
        assertEquals(listOf("Español"), subjectOptionsForTeacher("u-2", users))
    }

    @Test
    fun `unknown or unassigned teacher yields no subject options`() {
        val users = listOf(user("u-1", UserRole.TEACHER, listOf("Matemáticas")))

        assertTrue(subjectOptionsForTeacher("missing", users).isEmpty())
        assertTrue(subjectOptionsForTeacher("", users).isEmpty())
    }

    @Test
    fun `teacher picker offers only assigned teachers and keeps the uid as value`() {
        val users = listOf(
            user("u-1", UserRole.TEACHER, listOf("Matemáticas")),
            user("u-2", UserRole.ADMIN, listOf("Dirección")),
            user("u-3", UserRole.TUTOR),
            user("u-4", UserRole.TEACHER),
        )

        val options = teacherPickerOptions(users, setOf("u-1", "u-2", "u-4"))
        assertEquals(listOf("u-1", "u-4"), options.map { it.first })
        assertTrue(options[0].second.contains("Matemáticas"))
    }

    @Test
    fun `teacher picker drops other groups, non-teacher ids and unknown roles safely`() {
        val users = listOf(
            user("u-own", UserRole.TEACHER, listOf("Historia")),
            user("u-other-group", UserRole.TEACHER, listOf("Física")),
            user("u-admin", UserRole.ADMIN),
            user("u-tutor", UserRole.TUTOR),
            user("u-unknown", UserRole.UNKNOWN),
        )
        // u-other-group teaches a different group; u-admin/u-unknown are listed in
        // this group's assigned ids but are not teachers; u-missing has no user.
        val assigned = setOf("u-own", "u-admin", "u-unknown", "u-missing")

        val options = teacherPickerOptions(users, assigned)
        assertEquals(listOf("u-own"), options.map { it.first })
        assertTrue(options[0].second.contains("Historia"))
    }

    @Test
    fun `teacher picker stays safe with no assignments, unknown roles and blank uids`() {
        val users = listOf(
            user("u-1", UserRole.TEACHER),
            user(" ", UserRole.TEACHER),
            user("u-unknown", UserRole.UNKNOWN),
        )

        assertTrue(teacherPickerOptions(users, emptySet()).isEmpty())
        assertEquals(
            listOf("u-1"),
            teacherPickerOptions(users, setOf("u-1", " ", "u-unknown")).map { it.first },
        )
    }

    @Test
    fun `slot picker keeps a saved teacher that is no longer assigned to the group`() {
        val users = listOf(
            user("u-own", UserRole.TEACHER, listOf("Historia")),
            user("u-stale", UserRole.TEACHER, listOf("Física")),
        )
        val assignedOptions = teacherPickerOptions(users, setOf("u-own"))

        val current = slotTeacherPickerOptions("u-own", assignedOptions, users)
        assertEquals(listOf("", "u-own"), current.map { it.first })
        assertEquals("Sin asignar", current.first().second)

        val stale = slotTeacherPickerOptions("u-stale", assignedOptions, users)
        assertEquals(listOf("", "u-own", "u-stale"), stale.map { it.first })
        assertTrue(stale.last().second.contains("Física"))

        val blank = slotTeacherPickerOptions("", assignedOptions, users)
        assertEquals(listOf("", "u-own"), blank.map { it.first })

        val gone = slotTeacherPickerOptions("u-deleted", assignedOptions, users)
        assertEquals("u-deleted", gone.last().second)
    }

    @Test
    fun `schedule validation accepts a well formed slot`() {
        val slot = AdminScheduleSlot(
            weekday = 1,
            startTime = "08:00",
            endTime = "08:45",
            subject = "Matemáticas",
            teacherId = "u-1",
        )

        assertNull(scheduleValidationError(listOf(slot)))
        assertNull(scheduleValidationError(emptyList()))
    }

    @Test
    fun `schedule validation rejects bad format and end not after start`() {
        val base = AdminScheduleSlot(
            weekday = 1,
            startTime = "08:00",
            endTime = "08:45",
            subject = "Matemáticas",
            teacherId = "u-1",
        )

        val badStart = scheduleValidationError(listOf(base.copy(startTime = "8:00")))
        assertTrue(badStart != null && badStart.contains("formato"))

        val badEnd = scheduleValidationError(listOf(base.copy(endTime = "25:00")))
        assertTrue(badEnd != null && badEnd.contains("formato"))

        val equal = scheduleValidationError(listOf(base.copy(endTime = "08:00")))
        assertTrue(equal != null && equal.contains("posterior"))

        val reversed = scheduleValidationError(listOf(base.copy(startTime = "09:00", endTime = "08:45")))
        assertTrue(reversed != null && reversed.contains("posterior"))
    }

    @Test
    fun `student filter keeps only the requested group`() {
        fun student(id: String, groupId: String) = AdminStudent(
            id = id,
            firstName = "Nombre",
            lastName = "Apellido",
            fullName = "Nombre Apellido",
            curp = "",
            listNumber = 1,
            groupId = groupId,
            schoolId = "school-1",
            tutorIds = emptyList(),
            active = true,
        )

        val students = listOf(
            student("s-1", "g-3b"),
            student("s-2", "g-3c"),
            student("s-3", "g-3b"),
        )

        assertEquals(listOf("s-1", "s-3"), studentsForGroup(students, "g-3b").map { it.id })
        assertTrue(studentsForGroup(students, "missing").isEmpty())
    }
}
