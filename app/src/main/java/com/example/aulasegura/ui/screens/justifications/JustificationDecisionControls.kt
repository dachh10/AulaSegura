package com.example.aulasegura.ui.screens.justifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.PrimaryContainer

/** The teacher's approval controls for one family report. */
@Composable
fun JustificationDecisionControls(
    busy: Boolean,
    onDecide: (approve: Boolean, note: String) -> Unit,
) {
    var pendingNoteFor by remember { mutableStateOf<Boolean?>(null) }
    var note by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(
            onClick = { note = ""; pendingNoteFor = false },
            enabled = !busy,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(percent = 50))
                .background(Error.copy(alpha = 0.10f)),
        ) {
            Text("Rechazar", color = Error, fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Button(
            onClick = { note = ""; pendingNoteFor = true },
            enabled = !busy,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(percent = 50))
                .background(PrimaryContainer),
        ) {
            Text(
                text = if (busy) "Guardando…" else "Aprobar",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }

    pendingNoteFor?.let { approve ->
        AlertDialog(
            onDismissRequest = { pendingNoteFor = null },
            title = {
                Text(
                    text = if (approve) "Aprobar justificación" else "Rechazar justificación",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Nota para la familia (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDecide(approve, note)
                    pendingNoteFor = null
                }) {
                    Text(if (approve) "Aprobar" else "Rechazar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingNoteFor = null }) { Text("Cancelar") }
            },
        )
    }
}
