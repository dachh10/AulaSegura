package com.example.aulasegura.data.remote

import com.example.aulasegura.domain.repository.AdminGroupFields
import com.example.aulasegura.domain.repository.ApprovedGroup
import com.google.firebase.firestore.FieldValue

/**
 * The `groups/{id}` document written when a QR approval assigns a grade+section.
 * Shared by the online approval and the offline replay so both create the group
 * on demand and increment its counter instead of resetting it.
 */
fun approvedGroupDocument(group: ApprovedGroup, schoolId: String): Map<String, Any> =
    AdminGroupFields.newGroupDocument(
        name = group.name,
        grade = group.grade,
        section = group.section,
        schoolId = schoolId,
    ).toMutableMap().apply {
        put(AdminGroupFields.STUDENT_COUNT, FieldValue.increment(1L))
        put(AdminGroupFields.UPDATED_AT, FieldValue.serverTimestamp())
    }
