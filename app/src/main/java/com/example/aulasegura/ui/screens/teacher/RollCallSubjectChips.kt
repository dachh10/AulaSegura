package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.ClassSlot
import com.example.aulasegura.domain.model.subjectById
import com.example.aulasegura.domain.model.subjectIdFromLabel
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.PrimaryContainer

/** One subject the signed-in teacher can take attendance for in a group. */
data class RollCallSubjectUi(val id: String, val label: String)

/**
 * Subjects come from the timetable, not from the profile: the same teacher can
 * cover two groups with different subjects. A slot without an owner is kept
 * because older schedule documents may not carry `teacherId`; a slot owned by
 * another teacher is never offered here.
 */
internal fun rollCallSubjectsOf(
    slots: List<ClassSlot>,
    groupId: String,
    grade: Int,
    currentUid: String,
): List<RollCallSubjectUi> = slots
    .filter { slot ->
        slot.groupId == groupId &&
            (slot.teacherId.isNullOrBlank() || slot.teacherId == currentUid)
    }
    .mapNotNull { slot ->
        subjectIdFromLabel(slot.subject, grade)?.let { id ->
            RollCallSubjectUi(id = id, label = subjectById(id)?.label ?: slot.subject)
        }
    }
    .distinctBy { it.id }

/** One button per subject, so two teachers of the same group never collide. */
@Composable
fun RollCallSubjectChips(
    subjects: List<RollCallSubjectUi>,
    onSelect: (String) -> Unit,
) {
    if (subjects.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        subjects.forEach { subject ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PrimaryContainer)
                    .clickable { onSelect(subject.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subject.label,
                    color = Color.White,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
