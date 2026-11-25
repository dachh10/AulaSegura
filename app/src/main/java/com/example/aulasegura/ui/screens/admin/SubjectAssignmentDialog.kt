package com.example.aulasegura.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.Subject
import com.example.aulasegura.domain.repository.unassignedSubjectsForGrade
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans

/** The director picks WHICH subject a teacher will teach in this group. */
@Composable
internal fun SubjectAssignmentDialog(
    teacherName: String,
    groupName: String,
    subjects: List<Subject>,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Materia en $groupName",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Elige la materia que impartirá $teacherName. Las que ya tienen docente no aparecen.",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                )
                subjects.forEach { subject ->
                    TextButton(
                        onClick = { onChoose(subject.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(subject.label, fontFamily = DmSans, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (subjects.isEmpty()) {
                    Text(
                        text = "Todas las materias de este grado ya tienen docente.",
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
