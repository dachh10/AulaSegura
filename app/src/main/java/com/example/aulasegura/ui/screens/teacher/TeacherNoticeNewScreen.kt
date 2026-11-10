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
 * Port visual 1:1 del mockup `bac6bd729b1f46129bcbc5092e0d5b2d`. Es solo el
 * cascarón: los destinatarios y la prioridad están fijos como en el mockup, y
 * no hay envío, programación ni persistencia. Los campos de texto y el
 * interruptor conservan únicamente su estado visual local.
 */

private val Hairline = Color(0xFFE6E1D8)
private val FieldBorder = Color(0xFFEAE6DF)
private val ChipIconSurface = Color(0xFFE8E9F7)

private val NoticeCardShape = RoundedCornerShape(16.dp)
private val PreviewCardShape = RoundedCornerShape(20.dp)

private data class AudienceChip(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
)

private val audienceChips = listOf(
    AudienceChip("Mi grupo 3° B", Icons.Filled.Groups, selected = true),
    AudienceChip("3° A", Icons.Filled.School),
    AudienceChip("2° C", Icons.Filled.School),
    AudienceChip("Todos mis grupos", Icons.Filled.GroupWork),
)

@Composable
fun TeacherNoticeNewScreen(
    onBack: () -> Unit = {},
) {
    var title by remember { mutableStateOf("Material para la feria de ciencias") }
    var message by remember {
        mutableStateOf(
            "Traer cartulina, plumones y un objeto reciclado para el proyecto del viernes.",
        )
    }
    var notifyFamilies by remember { mutableStateOf(true) }

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
                    audienceChips.forEach { chip ->
                        AudiencePill(chip = chip)
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
                    counter = "${title.length}/80",
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
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
                                if (title.isEmpty()) {
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
                    counter = "${message.length}/300",
                ) {
                    BasicTextField(
                        value = message,
                        onValueChange = { if (it.length <= 300) message = it },
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
                                if (message.isEmpty()) {
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
                PriorityCard()
            }

            // ZONE 5 — Vista previa
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PreviewHeader()
                PreviewCard(title = title, message = message)
            }

            // ZONE 6 — Notification toggle
            NotifyToggleCard(
                checked = notifyFamilies,
                onToggle = { notifyFamilies = !notifyFamilies },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ZONE 7 — Sticky bottom bar
        NoticeFooter()
    }
}

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
private fun AudiencePill(chip: AudienceChip) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .then(
                if (chip.selected) {
                    Modifier
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(percent = 50))
                        .background(PrimaryContainer)
                } else {
                    Modifier
                        .background(SurfaceContainerLowest)
                        .border(1.dp, Hairline, RoundedCornerShape(percent = 50))
                },
            )
            .clip(RoundedCornerShape(percent = 50))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = chip.icon,
            contentDescription = null,
            tint = if (chip.selected) Color.White else PrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = chip.label,
            color = if (chip.selected) Color.White else PrimaryContainer,
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
@Composable
private fun PriorityCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoticeCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, FieldBorder, NoticeCardShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Normal is selected in the mockup.
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryContainer),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Normal",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SecondaryFixed, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = BrandCoral,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Importante",
                color = BrandCoral,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
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
private fun PreviewCard(title: String, message: String) {
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
                            text = "Prof. Salinas · 3° B",
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                        )
                        Text(
                            text = "Primaria Matutina",
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
@Composable
private fun NotifyToggleCard(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
                text = "Avisar inmediatamente en sus teléfonos",
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
@Composable
private fun NoticeFooter() {
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
            val (interaction, pressModifier) = rememberPressScale(0.98f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(pressModifier)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(percent = 50),
                        ambientColor = BrandCoral.copy(alpha = 0.30f),
                        spotColor = BrandCoral.copy(alpha = 0.30f),
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .background(BrandCoral)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {},
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Publicar aviso",
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
