package com.example.aulasegura.ui.screens.parent

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.PrimaryContainer

/** 2.4 — Detalle de tarea. Ported from Stitch screen `29ac02e4480a4828a0fe2ba1351e3014`. */

private val IndigoTint = Color(0xFFE8E9F7)
private val IndigoBorder = Color(0xFFD5D7EE)
private val BorderWarm = Color(0xFFE6E1D8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val MintSurface = Color(0xFFF0FAF7)
private val MintBorder = Color(0xFFDCF4EC)
private val MintText = Color(0xFF2E9E6B)
private val PdfSurface = Color(0xFFFEECEB)
private val PdfBorder = Color(0xFFFCD5D2)
private val PdfText = Color(0xFFAE3123)
private val AmberSurface = Color(0xFFFEF3C7)
private val AmberText = Color(0xFF78350F)

/**
 * The task as the school published it.
 *
 * There is no "Marcar como vista" footer any more: `tasks` has no per-student
 * state and no read collection, so a button that only changed its own colour
 * would be the one lie left on this screen. The read mark lives in the notices
 * inbox, where `noticeReads` backs it.
 */
@Composable
fun ParentTaskDetailScreen(
    ui: ParentTaskDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .statusBarsPadding(),
    ) {
        DetailTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (ui) {
                is ParentTaskDetailUiState.Loaded -> {
                    Reveal(0) { TaskHeaderCard(ui) }
                    Reveal(60) { DueDateCard(ui) }
                    Reveal(120) { InstructionsSection(ui.description) }
                    if (ui.attachments.isNotEmpty()) {
                        Reveal(180) { AttachmentsSection(ui.attachments) }
                    }
                }

                ParentTaskDetailUiState.Loading -> Reveal(0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = PrimaryContainer)
                    }
                }

                ParentTaskDetailUiState.NotFound -> Reveal(0) {
                    DetailMessage(
                        title = "Esta tarea ya no está disponible",
                        message = "La escuela la retiró o el enlace está incompleto.",
                    )
                }

                is ParentTaskDetailUiState.Failed -> Reveal(0) {
                    DetailMessage(
                        title = "No se pudo cargar la tarea",
                        message = ui.message,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                onClick = onBack,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Detalle de tarea",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = BorderWarm)
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.9f)
    Box(
        modifier = pressModifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TaskHeaderCard(ui: ParentTaskDetailUiState.Loaded) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoTint)
            .border(1.dp, IndigoBorder, RoundedCornerShape(24.dp)),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = PrimaryContainer.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(130.dp),
        )

        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ui.subject.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandCoral)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = ui.subject,
                            color = OnPrimary,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (ui.groupLabel.isNotBlank()) {
                    Text(
                        text = ui.groupLabel,
                        color = Slate500,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = ui.title,
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.3).sp,
            )

            if (ui.teacherName.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = IndigoBorder.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainer)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ui.teacherInitials,
                            color = OnPrimary,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = ui.teacherName,
                            color = PrimaryContainer,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        )
                        if (ui.publishedLabel != null) {
                            Text(
                                text = ui.publishedLabel,
                                color = Slate600,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DueDateCard(ui: ParentTaskDetailUiState.Loaded) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MintSurface)
                    .border(1.dp, MintBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = MintText,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Fecha de entrega",
                    color = Slate500,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                )
                Text(
                    text = ui.dueLabel ?: "Sin fecha registrada",
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }

        if (ui.dueSoon || ui.overdue) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFF4EFE7))
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (ui.overdue) PdfSurface else AmberSurface)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (ui.overdue) {
                            Icons.Outlined.CalendarToday
                        } else {
                            Icons.Filled.Schedule
                        },
                        contentDescription = null,
                        tint = if (ui.overdue) PdfText else AmberText,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (ui.overdue) "La fecha ya pasó" else "Vence pronto",
                        color = if (ui.overdue) PdfText else AmberText,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionsSection(description: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Instrucciones",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Text(
                text = description.ifBlank { "La escuela no agregó instrucciones a esta tarea." },
                color = Slate700,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun AttachmentsSection(attachments: List<ParentAttachmentUi>) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Archivos adjuntos",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Text(
                text = if (attachments.size == 1) "1 recurso" else "${attachments.size} recursos",
                color = Slate500,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            attachments.forEach { attachment ->
                AttachmentRow(attachment = attachment)
            }
        }
    }
}

@Composable
private fun AttachmentRow(attachment: ParentAttachmentUi) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (attachment.isPdf) PdfSurface else IndigoTint)
                    .border(
                        1.dp,
                        if (attachment.isPdf) PdfBorder else IndigoBorder,
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (attachment.isPdf) {
                        Icons.Filled.PictureAsPdf
                    } else {
                        Icons.Filled.Image
                    },
                    contentDescription = null,
                    tint = if (attachment.isPdf) PdfText else PrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    color = Color(0xFF1C1C19),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = attachment.meta,
                    color = Slate500,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val (interaction, pressModifier) = rememberPressScale(0.9f)
        Box(
            modifier = pressModifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = null) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url)),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "Abrir ${attachment.name}",
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DetailMessage(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = title,
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = Slate600,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Box(
                modifier = pressModifier
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .clickable(interactionSource = interaction, indication = null, onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Reintentar",
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
