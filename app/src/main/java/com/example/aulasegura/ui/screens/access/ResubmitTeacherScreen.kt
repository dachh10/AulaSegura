package com.example.aulasegura.ui.screens.access

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer

/**
 * A rejected teacher chooses the classroom and subject again and reopens the
 * application. No new account is created: the same profile goes back to pending.
 */
@Composable
fun ResubmitTeacherScreen(
    ui: RegisterUiState,
    onGradeChange: (Int) -> Unit,
    onSectionChange: (String) -> Unit,
    onSubjectIdChange: (String) -> Unit,
    onRefreshTakenSubjects: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Volver a enviar solicitud",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Elige de nuevo el grado, el grupo y la materia. La directora revisará la solicitud como si fuera la primera vez.",
            color = PrimaryContainer,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(24.dp))

        TeacherAssignmentFields(
            grade = ui.grade,
            section = ui.section,
            subjectId = ui.subjectId,
            takenSubjects = ui.takenSubjects,
            onGradeChange = onGradeChange,
            onSectionChange = onSectionChange,
            onSubjectIdChange = onSubjectIdChange,
            onSelectionChanged = onRefreshTakenSubjects,
        )

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, color = Error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onSubmit,
            enabled = !isLoading && ui.grade != null && ui.section.isNotBlank() && ui.subjectId.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Enviando…" else "Enviar solicitud")
        }
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }
}
