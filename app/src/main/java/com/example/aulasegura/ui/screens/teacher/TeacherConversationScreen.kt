package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant
import com.example.aulasegura.ui.theme.TertiaryFixedDim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Teacher — Conversación de chat. Ported from Stitch screen
 * `889c39e23e6142c09cd11698a057830e` ("AulaSegura - Chat", project 7677408035297541086).
 *
 * The Stitch mockup is role-agnostic, so the only adaptation is the sender side:
 * on the teacher shell the contact is the parent (Carlos Gómez Pérez) and the
 * teacher's own messages sit on the right. Bubbles, colors, spacing, day chip,
 * pending-sync bubble and composer are the mockup's.
 *
 * Visual shell only: sending appends a local bubble, no backend.
 */

private data class TeacherChatMessage(
    val id: Int,
    val text: String,
    val sent: Boolean,
    val time: String,
    val pending: Boolean = false,
)

private fun sampleTeacherMessages(): List<TeacherChatMessage> = listOf(
    TeacherChatMessage(
        id = 1,
        text = "Buenos días, Carlos. Mariana tendrá examen de Matemáticas el jueves.",
        sent = true,
        time = "10:40",
    ),
    TeacherChatMessage(
        id = 2,
        text = "Gracias, profesor. ¿Qué temas incluye?",
        sent = false,
        time = "10:41",
    ),
    TeacherChatMessage(
        id = 3,
        text = "Fracciones y números decimales. Le comparto la guía en la sección de tareas.",
        sent = true,
        time = "10:42",
    ),
    TeacherChatMessage(
        id = 4,
        text = "Perfecto, lo repasamos hoy.",
        sent = false,
        time = "10:42",
    ),
    TeacherChatMessage(
        id = 5,
        text = "Perfecto, mañana le comparto la guía impresa.",
        sent = true,
        time = "10:43",
        pending = true,
    ),
)

@Composable
fun TeacherConversationScreen(onBack: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(sampleTeacherMessages()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
            .imePadding(),
    ) {
        ConversationTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ShieldPattern(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.04f,
                color = PrimaryContainer,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DayChip()
                messages.forEach { message ->
                    MessageBubble(message = message)
                }
            }
        }

        MessageComposer(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    messages = messages + TeacherChatMessage(
                        id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
                        text = text,
                        sent = true,
                        time = time,
                    )
                    draft = ""
                }
            },
        )
    }
}

@Composable
private fun ConversationTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (backInteraction, backPress) = rememberPressScale(0.9f)
            Box(
                modifier = backPress
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "CG",
                        color = SurfaceContainerLowest,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.18).sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(TertiaryFixedDim)
                        .border(2.dp, Surface, CircleShape),
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Carlos Gómez Pérez",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.18).sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TertiaryFixedDim),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Padre de familia · 3° B · En línea",
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.33.sp,
                    )
                }
            }

            val (moreInteraction, morePress) = rememberPressScale(0.9f)
            Box(
                modifier = morePress
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = moreInteraction,
                        indication = null,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Opciones",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SurfaceVariant)
    }
}

@Composable
private fun DayChip() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
                .border(1.dp, SurfaceVariant, CircleShape)
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Text(
                text = "Hoy",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.24.sp,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: TeacherChatMessage) {
    val shape = if (message.sent) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomEnd = 6.dp,
            bottomStart = 20.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 6.dp,
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.sent) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 292.dp)
                .clip(shape)
                .background(
                    if (message.sent) {
                        if (message.pending) PrimaryContainer.copy(alpha = 0.95f)
                        else PrimaryContainer
                    } else {
                        SurfaceContainerLowest
                    },
                )
                .then(
                    if (message.sent) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, SurfaceVariant, shape)
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.text,
                color = if (message.sent) OnPrimary else MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (message.pending) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = OnPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Pendiente de enviar",
                        color = OnPrimary.copy(alpha = 0.8f),
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = message.time,
                    color = if (message.sent) {
                        OnPrimary.copy(alpha = 0.7f)
                    } else {
                        Outline
                    },
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                if (message.sent && !message.pending) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Entregado",
                        tint = TertiaryFixedDim,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = SurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = if (focused) PrimaryContainer else SurfaceVariant,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Adjuntar foto",
                    tint = Outline,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(PrimaryContainer),
                    interactionSource = interactionSource,
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        Box {
                            if (draft.isEmpty()) {
                                Text(
                                    text = "Escribe un mensaje",
                                    color = Outline,
                                    fontFamily = DmSans,
                                    fontSize = 14.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            Spacer(Modifier.width(8.dp))

            val (interaction, pressModifier) = rememberPressScale(0.93f)
            Box(
                modifier = pressModifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onSend,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar mensaje",
                    tint = OnPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
