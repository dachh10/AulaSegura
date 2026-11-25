package com.example.aulasegura.ui.screens.justifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** One report, rendered the same way for the teacher who reads it and the principal who decides. */
private val Hairline = Color(0xFFE6E1D8)
private val CardShape = RoundedCornerShape(20.dp)
private val HintShape = RoundedCornerShape(percent = 50)

private val PendingTint = Color(0xFF92400E)
private val PendingBackground = Color(0xFFFEF3C7)
private val ApprovedTint = Color(0xFF166534)
private val ApprovedBackground = Color(0xFFDCFCE7)
private val RejectedTint = Color(0xFF991B1B)
private val RejectedBackground = Color(0xFFFEE2E2)

/**
 * [actions] is the principal's slot for the decision controls. The teacher
 * passes nothing, so their card stays exactly as it was before the inbox was
 * shared.
 */
@Composable
fun JustificationCard(
    card: JustificationCardUi,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
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
                    text = listOf(card.groupLabel, card.subject)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
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

        actions()
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
