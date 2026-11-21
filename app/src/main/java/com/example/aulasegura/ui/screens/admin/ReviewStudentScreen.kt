package com.example.aulasegura.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Primary

/**
 * Reads the payload handed over by the scanner. T4 replaces this with the full
 * review form (grade, group, duplicate check, offline approval).
 */
@Composable
fun ReviewStudentScreen(
    uid: String,
    alumno: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Text(
            text = "Revisar alumno",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(text = "Alumno: $alumno", fontFamily = DmSans, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(text = "Tutor: $uid", fontFamily = DmSans, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "El formulario de revisión y aprobación se implementa en el siguiente bloque.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(24.dp))
        PillButton(text = "Regresar", onClick = onBack)
    }
}
