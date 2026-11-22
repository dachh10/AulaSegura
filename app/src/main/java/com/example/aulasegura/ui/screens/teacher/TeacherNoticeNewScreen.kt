package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * 3.12 — Publicar aviso (panel del docente).
 *
 * Port visual 1:1 del mockup `bac6bd729b1f46129bcbc5092e0d5b2d`, ahora con los
 * grupos reales del docente y publicación offline-first: el aviso se guarda en
 * Room y se encola, y `NoticeSyncHandler` lo escribe en Firestore.
 *
 * La audiencia es siempre la de los tutores de los grupos elegidos: el preview
 * dice "ASÍ LO VERÁN LAS FAMILIAS" y un aviso que también fuera para docentes
 * necesitaría una bandeja de docente que la Fase 1 no tiene.
 *
 * Lo que la pantalla NO hace, y por eso se ve deshabilitado: enviar notificación
 * push (llega con FCM, bloque 10). Dejarlo activo prometería un envío que no
 * ocurre.
 */

private val Hairline = Color(0xFFE6E1D8)
private val FieldBorder = Color(0xFFEAE6DF)
private val ChipIconSurface = Color(0xFFE8E9F7)

private val NoticeCardShape = RoundedCornerShape(16.dp)
private val PreviewCardShape = RoundedCornerShape(20.dp)

@Composable
fun TeacherNoticeNewScreen(
    ui: TeacherNoticeNewUi,
    onBack: () -> Unit = {},
    onToggleGroup: (String) -> Unit = {},
    onToggleAllGroups: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onBodyChange: (String) -> Unit = {},
    onUrgentChange: (Boolean) -> Unit = {},
    onPublish: () -> Unit = {},
) {
    val allSelected = ui.groups.isNotEmpty() && ui.selectedGroupIds.size == ui.groups.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .statusBarsPadding(),
    ) {
        NoticeTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .revealOnEnter(60)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ZONE 2 — Destinatarios
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinatariosLabel()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AudiencePill(
                        label = "Todos mis grupos",
                        icon = Icons.Filled.GroupWork,
                        selected = allSelected,
                        enabled = !ui.published,
                        onClick = onToggleAllGroups,
                    )
                    ui.groups.forEach { group ->
                        AudiencePill(
                            label = group.label,
                            icon = Icons.Filled.School,
                            selected = group.id in ui.selectedGroupIds,
                            enabled = !ui.published,
                            onClick = { onToggleGroup(group.id) },
                        )
                    }
                }
            }

            // ZONE 3 — Form fields
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NoticeFieldCard(
                    icon = Icons.Outlined.Title,
                    label = "Título del aviso",
                    counter = "${ui.title.length}/${NOTICE_TITLE_MAX_CHARS}",
                ) {
                    BasicTextField(
                        value = ui.title,
                        onValueChange = onTitleChange,
                        enabled = !ui.published,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(PrimaryContainer),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (ui.title.isEmpty()) {
                                    Text(
                                        text = "Ej. Recordatorio de excursión",
                                        color = Outline.copy(alpha = 0.70f),
                                        fontFamily = DmSans,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }

                NoticeFieldCard(
                    icon = Icons.Outlined.EditNote,
                    label = "Mensaje",
                    counter = "${ui.body.length}/${NOTICE_BODY_MAX_CHARS}",
                ) {
                    BasicTextField(
                        value = ui.body,
                        onValueChange = onBodyChange,
                        enabled = !ui.published,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        textStyle = TextStyle(
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(PrimaryContainer),
                        maxLines = 5,
                        decorationBox = { innerTextField ->
                            Box {
                                if (ui.body.isEmpty()) {
                                    Text(
                                        text = "Escriba aquí los detalles...",
                                        color = Outline.copy(alpha = 0.70f),
                                        fontFamily = DmSans,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }

            // ZONE 4 — Prioridad
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel(text = "Prioridad", color = PrimaryContainer)
                PriorityCard(
                    urgent = ui.urgent,
                    enabled = !ui.published,
                    onUrgentChange = onUrgentChange,
                )
            }

            // ZONE 5 — Vista previa
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PreviewHeader()
                PreviewCard(
                    title = ui.title,
                    message = ui.body,
                    teacherName = ui.teacherName,
                    audienceLabel = ui.selectedGroupLabels.joinToString(", "),
                )
            }

            // ZONE 6 — Notification toggle (FCM arrives with block 10)
            NotifyToggleCard(
                checked = false,
                onToggle = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ZONE 7 — Sticky bottom bar
        NoticeFooter(
            message = ui.message,
            canPublish = ui.canPublish,
            publishing = ui.publishing,
            published = ui.published,
            onPublish = onPublish,
            onClose = onBack,
        )
    }
}

/** Must match the caps in `TeacherNoticeNewViewModel`. */
private const val NOTICE_TITLE_MAX_CHARS = 80
private const val NOTICE_BODY_MAX_CHARS = 300


// ZONE 1 — Top app bar
@Composable
private fun NoticeTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(BrandCream.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (backInteraction, backPress) = rememberPressScale(0.95f)
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(backPress)
                .shadow(elevation = 1.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, CircleShape)
                .clickable(
                    interactionSource = backInteraction,
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Publicar aviso",
            modifier = Modifier.weight(1f),
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.02).em,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = "Ayuda",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ZONE 2 — Destinatarios
@Composable
private fun DestinatariosLabel() {
    Text(
        text = "Destinatarios",
        modifier = Modifier.padding(horizontal = 18.dp),
        color = PrimaryContainer,
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
}

@Composable
private fun AudiencePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.97f)
    Row(
        modifier = Modifier
            .height(36.dp)
            .then(pressModifier)
            .then(
                if (selected) {
                    Modifier
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(percent = 50))
                        .background(PrimaryContainer)
                } else {
                    Modifier
                        .background(SurfaceContainerLowest)
                        .border(1.dp, Hairline, RoundedCornerShape(percent = 50))
                },
            )
            .alpha(if (enabled) 1f else 0.60f)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else PrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) Color.White else PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 1,
        )
    }
}

// ZONE 3 — Form fields
@Composable
private fun NoticeFieldCard(
    icon: ImageVector,
    label: String,
    counter: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoticeCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, FieldBorder, NoticeCardShape)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.02.em,
                )
            }
            Text(
                text = counter,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        content()
    }
}

// ZONE 4 — Prioridad
/**
 * Two mutually exclusive chips, driven by [urgent].
 *
 * The document stores only `urgent`; `category` (`urgente` / `grupo`) is derived
 * in the repository so the chip the teacher picks and the value the tutor filters
 * by cannot disagree.
 */
@Composable
private fun PriorityCard(
    urgent: Boolean,
    enabled: Boolean,
    onUrgentChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoticeCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, FieldBorder, NoticeCardShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PriorityOption(
            label = noticePriorityLabel(urgent = false),
            icon = Icons.Filled.CheckCircle,
            selected = !urgent,
            accent = PrimaryContainer,
            enabled = enabled,
            onClick = { onUrgentChange(false) },
            modifier = Modifier.weight(1f),
        )
        PriorityOption(
            label = noticePriorityLabel(urgent = true),
            icon = Icons.Outlined.ErrorOutline,
            selected = urgent,
            accent = BrandCoral,
            enabled = enabled,
            onClick = { onUrgentChange(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PriorityOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interaction, pressModifier) = rememberPressScale(0.97f)
    Row(
        modifier = modifier
            .height(40.dp)
            .then(pressModifier)
            .then(
                if (selected) {
                    Modifier
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent)
                } else {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SecondaryFixed, RoundedCornerShape(12.dp))
                },
            )
            .alpha(if (enabled) 1f else 0.60f)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) Color.White else accent,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

// ZONE 5 — Vista previa
@Composable
private fun PreviewHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Vista previa",
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )

        Text(
            text = "ASÍ LO VERÁN LAS FAMILIAS",
            color = OnSurfaceVariant.copy(alpha = 0.80f),
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.05.em,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(SurfaceContainer)
                .border(1.dp, Hairline, RoundedCornerShape(percent = 50))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PreviewCard(
    title: String,
    message: String,
    teacherName: String,
    audienceLabel: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PreviewCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, FieldBorder, PreviewCardShape),
    ) {
        // Decorative mint wash in the top-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(96.dp)
                .clip(RoundedCornerShape(bottomStart = 48.dp))
                .drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BrandMint.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                            start = Offset(size.width, 0f),
                            end = Offset(0f, size.height),
                        ),
                    )
                },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ChipIconSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = PrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (teacherName.isNotBlank()) "Prof. $teacherName" else "Docente",
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                        )
                        Text(
                            text = audienceLabel.ifBlank { "Sin grupos seleccionados" },
                            color = OnSurfaceVariant,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
                Text(
                    text = "Ahora",
                    color = Outline,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title.ifBlank { "Título del aviso" },
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.ifBlank { "Aquí aparecerá el mensaje que escribas." },
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ZONE 6 — Notification toggle
/**
 * El interruptor de push se muestra deshabilitado a propósito: el envío real
 * llega con el bloque 10 (FCM), y en Spark el push se manda desde la consola.
 * Un interruptor que se puede tocar y no manda nada es peor que uno apagado que
 * dice por qué.
 */
@Composable
private fun NotifyToggleCard(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.60f)
            .clip(NoticeCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, FieldBorder, NoticeCardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ChipIconSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            Text(
                text = "Enviar notificación",
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (enabled) {
                    "Avisar inmediatamente en sus teléfonos"
                } else {
                    "Todavía no se envían notificaciones push."
                },
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }

        val (interaction, pressModifier) = rememberPressScale(0.96f)
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .then(pressModifier)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (checked) PrimaryContainer else SurfaceContainerHigh)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onToggle,
                )
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

// ZONE 7 — Sticky bottom bar
/**
 * Footer que también cuenta lo que pasó.
 *
 * - [message] dice por qué no se pudo publicar, en las palabras del docente.
 * - El botón solo se habilita con [canPublish]: un aviso mandado al grupo
 *   equivocado llega a treinta familias.
 * - Con [published] el botón se vuelve la confirmación y cierra la pantalla: el
 *   aviso ya quedó encolado y no hay nada que editar en este formulario.
 */
@Composable
private fun NoticeFooter(
    message: String?,
    canPublish: Boolean,
    publishing: Boolean,
    published: Boolean,
    onPublish: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FieldBorder),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (message != null) {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    color = Error,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            val (interaction, pressModifier) = rememberPressScale(0.98f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(pressModifier)
                    .shadow(
                        elevation = if (canPublish) 8.dp else 0.dp,
                        shape = RoundedCornerShape(percent = 50),
                        ambientColor = BrandCoral.copy(alpha = 0.30f),
                        spotColor = BrandCoral.copy(alpha = 0.30f),
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (canPublish) BrandCoral else BrandCoral.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = canPublish,
                        onClick = if (published) onClose else onPublish,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (published) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.AutoMirrored.Filled.Send
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        published -> "Aviso guardado"
                        publishing -> "Guardando…"
                        else -> "Publicar aviso"
                    },
                    color = Color.White,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.03.em,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF1C1C19).copy(alpha = 0.20f)),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 2.dp),
        color = color,
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
}
