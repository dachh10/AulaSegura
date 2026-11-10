package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.ErrorContainer
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant

/**
 * 3.11 — Publicar tarea (panel del docente).
 *
 * Port visual 1:1 del mockup `8acb44e85be44ad5801bd1532a7ab798`. Es solo el
 * cascarón de la pantalla: no hay selección de grupo, adjuntos, fecha, hora ni
 * publicación real. Los campos de texto y el interruptor guardan únicamente su
 * estado visual local.
 */

private val Hairline = Color(0xFFE5E2DD)
private val FieldIconSurface = Color(0xFFF6F3EE)
private val UploadTint = Color(0xFFE8E9F7)
private val ToggleOn = Color(0xFF2B2F77)

private val FormCardShape = RoundedCornerShape(16.dp)

@Composable
fun TeacherTaskNewScreen(
    onBack: () -> Unit = {},
) {
    var title by remember { mutableStateOf("Fracciones, ejercicios 1-10") }
    var instructions by remember { mutableStateOf("") }
    var notifyFamilies by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .statusBarsPadding(),
    ) {
        TaskTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .revealOnEnter(60)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ZONE 2 — Form fields
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GroupField()
                TitleField(
                    value = title,
                    onValueChange = { title = it },
                )
                InstructionsField(
                    value = instructions,
                    onValueChange = { if (it.length <= 500) instructions = it },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DateTimeField(
                        icon = Icons.Outlined.CalendarToday,
                        label = "Fecha entrega",
                        value = "Mié 5 nov 2026",
                        mono = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    DateTimeField(
                        icon = Icons.Outlined.Schedule,
                        label = "Hora",
                        value = "08:00",
                        mono = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }

            // ZONE 3 — Attachments
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Archivos adjuntos",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.01).em,
                )

                UploadDropZone()

                AttachedFileRow(fileName = "Guia_fracciones.pdf", fileSize = "240 KB")
            }

            // ZONE 4 — Notification toggle
            NotificationCard(
                checked = notifyFamilies,
                onToggle = { notifyFamilies = !notifyFamilies },
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // ZONE 5 — Sticky bottom bar
        TaskFooter()
    }
}

// ZONE 1 — Top app bar
@Composable
private fun TaskTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandCream.copy(alpha = 0.95f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (backInteraction, backPress) = rememberPressScale(0.95f)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .then(backPress)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, SurfaceVariant, CircleShape)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver atrás",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Publicar tarea",
                modifier = Modifier.weight(1f),
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.02).em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Más opciones",
                    tint = Outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline.copy(alpha = 0.80f)),
        )
    }
}

// ZONE 2 — Form fields
@Composable
private fun GroupField() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FormCardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldIcon(icon = Icons.Outlined.Groups)

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            FieldLabel(text = "Grupo")
            Text(
                text = "3° B · Matemáticas",
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = "Cambiar grupo",
            tint = Outline,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FormCardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldIcon(icon = Icons.Outlined.Edit)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            FieldLabel(text = "Título")
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
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
                        if (value.isEmpty()) {
                            Text(
                                text = "Escribe el título",
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
    }
}

@Composable
private fun InstructionsField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FormCardShape)
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            FieldIcon(icon = Icons.Outlined.Description)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                FieldLabel(text = "Instrucciones", modifier = Modifier.padding(bottom = 2.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    textStyle = TextStyle(
                        color = OnSurface,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(PrimaryContainer),
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "Indica los pasos a seguir, detalles de entrega y " +
                                        "recomendaciones para los alumnos...",
                                    color = Outline.copy(alpha = 0.70f),
                                    fontFamily = DmSans,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        Text(
            text = "${value.length}/500",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = (-0.02).em,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DateTimeField(
    icon: ImageVector,
    label: String,
    value: String,
    mono: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(FormCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FormCardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(FieldIconSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = label.uppercase(),
                color = Outline,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                letterSpacing = 0.05.em,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = OnSurface,
                fontFamily = if (mono) JetBrainsMono else DmSans,
                fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = if (mono) 18.sp else 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ZONE 3 — Attachments
@Composable
private fun UploadDropZone() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormCardShape)
            .background(UploadTint.copy(alpha = 0.40f))
            .dashedUploadBorder(
                color = PrimaryContainer.copy(alpha = 0.30f),
                cornerRadius = 16.dp,
                strokeWidth = 2.dp,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(elevation = 1.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(SurfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AttachFile,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Arrastra o selecciona documentos de apoyo",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "PDF, DOCX o PNG (Máx 25 MB)",
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        val (interaction, pressModifier) = rememberPressScale(0.95f)
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
                .then(pressModifier)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(percent = 50))
                .clip(RoundedCornerShape(percent = 50))
                .background(PrimaryContainer)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Adjuntar archivo",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.02.em,
            )
        }
    }
}

@Composable
private fun AttachedFileRow(fileName: String, fileSize: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FormCardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ErrorContainer.copy(alpha = 0.40f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = null,
                tint = Error,
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
                text = fileName,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = fileSize,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Eliminar $fileName",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ZONE 4 — Notification toggle
@Composable
private fun NotificationCard(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FormCardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(UploadTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
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
                text = "Notificar a las familias",
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Se enviará una notificación al publicar",
                color = Outline,
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
                .background(if (checked) ToggleOn else SurfaceContainerHigh)
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

// ZONE 5 — Sticky bottom bar
@Composable
private fun TaskFooter() {
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
                .background(SurfaceVariant.copy(alpha = 0.80f)),
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
                    modifier = Modifier
                        .rotate(-12f)
                        .size(19.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Publicar tarea",
                    color = Color.White,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.03.em,
                    maxLines = 1,
                )
            }

            Text(
                text = "Guardar borrador",
                modifier = Modifier
                    .padding(top = 10.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF1C1C19).copy(alpha = 0.30f)),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Shared bits

@Composable
private fun FieldIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(FieldIconSurface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = Outline,
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.em,
    )
}

private fun Modifier.dashedUploadBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
            ),
        ),
    )
}
