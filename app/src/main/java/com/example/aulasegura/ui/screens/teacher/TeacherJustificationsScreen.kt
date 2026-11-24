package com.example.aulasegura.ui.screens.teacher

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** Teacher — the families' justifications for the groups this teacher teaches. */

private val Hairline = Color(0xFFE6E1D8)
private val CardShape = RoundedCornerShape(20.dp)
private val HintShape = RoundedCornerShape(percent = 50)

private val PendingTint = Color(0xFF92400E)
private val PendingBackground = Color(0xFFFEF3C7)
private val ApprovedTint = Color(0xFF166534)
private val ApprovedBackground = Color(0xFFDCFCE7)
private val RejectedTint = Color(0xFF991B1B)
private val RejectedBackground = Color(0xFFFEE2E2)

@Composable
fun TeacherJustificationsScreen(
    ui: TeacherJustificationsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
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

            when (ui) {
                TeacherJustificationsUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PrimaryContainer)
                }

                is TeacherJustificationsUiState.Failed -> FailureCard(ui = ui, onRetry = onRefresh)

                is TeacherJustificationsUiState.Loaded -> {
                    if (ui.isEmpty) {
                        EmptyCard()
                    } else {
                        if (ui.pending.isNotEmpty()) {
                            SectionTitle("Pendientes")
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
            text = "Cuando una familia justifique una falta de tus grupos, aparecerá aquí.",
            color = Outline,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun FailureCard(
    ui: TeacherJustificationsUiState.Failed,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "No se pudieron cargar",
            color = Error,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Text(
            text = ui.message,
            color = Outline,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Text(
            text = "Reintentar",
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(HintShape)
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun JustificationCard(card: JustificationCardUi) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profileInitials(card.studentName),
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.studentName,
                    color = OnboardingHeading,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                )
                Text(
                    text = card.groupLabel,
                    color = Outline,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            StatusChip(card)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = card.dateLabel,
                style = com.example.aulasegura.ui.theme.AulaTextStyles.dataMonoSm,
                color = Outline,
            )
            Text(
                text = "·",
                color = OutlineVariant,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
            Text(
                text = card.reasonLabel,
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }

        if (card.description.isNotBlank()) {
            Text(
                text = card.description,
                color = OnboardingHeading,
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        card.reviewNote?.let { note ->
            Text(
                text = "Nota de Dirección: $note",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        card.evidenceUrl?.let { url ->
            Row(
                modifier = Modifier
                    .clip(HintShape)
                    .background(PrimaryContainer.copy(alpha = 0.08f))
                    .clickable { runCatching { uriHandler.openUri(url) } }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (card.evidenceType == "image") {
                        "Ver comprobante"
                    } else {
                        "Abrir comprobante"
                    },
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(card: JustificationCardUi) {
    val (tint, background) = when (card.status) {
        JustificationStatus.PENDING -> PendingTint to PendingBackground
        JustificationStatus.APPROVED -> ApprovedTint to ApprovedBackground
        JustificationStatus.REJECTED -> RejectedTint to RejectedBackground
    }
    Text(
        text = card.statusLabel,
        color = tint,
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(HintShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
