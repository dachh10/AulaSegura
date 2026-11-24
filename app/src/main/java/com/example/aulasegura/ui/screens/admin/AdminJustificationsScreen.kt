package com.example.aulasegura.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.justifications.JustificationCard
import com.example.aulasegura.ui.screens.justifications.JustificationCardUi
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** Principal — reads the families' reports and records the school's decision. */
private val Hairline = Color(0xFFE6E1D8)
private val CardShape = RoundedCornerShape(20.dp)
private val HintShape = RoundedCornerShape(percent = 50)

private data class PendingDecision(
    val card: JustificationCardUi,
    val approve: Boolean,
)

@Composable
fun AdminJustificationsScreen(
    ui: AdminJustificationsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNoticeShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Reveal(0) {
                JustificationsHeader(onBack = onBack, onRefresh = onRefresh)
            }

            ui.notice?.let { text ->
                NoticeCard(text = text, tint = PrimaryContainer, onClick = onNoticeShown)
            }
            ui.error?.let { text ->
                NoticeCard(text = text, tint = Error, onClick = onErrorShown)
            }

            when {
                ui.loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PrimaryContainer)
                }

                ui.isEmpty -> EmptyCard()

                else -> {
                    if (ui.pending.isNotEmpty()) {
                        SectionTitle("Pendientes (${ui.pendingCount})")
                        // The principal keeps a supervisory read, but the teacher
                        // who marked the absence is the only one who decides.
                        ui.pending.forEach { card -> JustificationCard(card = card) }
                    }
                    if (ui.reviewed.isNotEmpty()) {
                        SectionTitle("Revisadas")
                        ui.reviewed.forEach { card -> JustificationCard(card = card) }
                    }
                }
            }
        }
    }

}

@Composable
private fun JustificationsHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Volver",
            onClick = onBack,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Justificaciones",
                color = OnboardingHeading,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
            )
            Text(
                text = "Faltas que las familias reportaron",
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        RoundIconButton(
            icon = Icons.Filled.Refresh,
            contentDescription = "Actualizar",
            onClick = onRefresh,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = OnboardingHeading,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = OnboardingHeading,
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
internal fun NoticeCard(text: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = tint,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "OK",
            color = tint,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun EmptyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "No hay justificaciones",
            color = OnboardingHeading,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Text(
            text = "Cuando una familia justifique una falta, aparecerá aquí para que la escuela la revise.",
            color = Outline,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun DecisionActions(
    busy: Boolean,
    onDecide: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(
            onClick = { onDecide(false) },
            enabled = !busy,
            modifier = Modifier
                .weight(1f)
                .clip(HintShape)
                .background(Error.copy(alpha = 0.10f)),
        ) {
            Text(
                text = "Rechazar",
                color = Error,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Button(
            onClick = { onDecide(true) },
            enabled = !busy,
            modifier = Modifier
                .weight(1f)
                .clip(HintShape)
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
}

@Composable
private fun DecisionDialog(
    decision: PendingDecision,
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (decision.approve) {
                    "Aprobar justificación"
                } else {
                    "Rechazar justificación"
                },
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${decision.card.studentName} · ${decision.card.dateLabel}",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("Nota para la familia (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (decision.approve) "Aprobar" else "Rechazar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
