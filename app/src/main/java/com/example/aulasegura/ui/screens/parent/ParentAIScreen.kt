package com.example.aulasegura.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant
import com.example.aulasegura.ui.theme.TertiaryContainer
import com.example.aulasegura.ui.theme.TertiaryFixed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 2.11 — Asistente IA. Ported from Stitch screen `bd8b739044634567b469e6d2fbcafedb`. */

private data class AIMessage(
    val id: Int,
    val text: String,
    val sent: Boolean,
    val time: String,
    val showTaskCard: Boolean = false,
    val showFeedback: Boolean = false,
)

private fun sampleAIMessages(): List<AIMessage> = listOf(
    AIMessage(
        id = 1,
        text = "¡Hola, Carlos! Soy tu asistente escolar. Puedo ayudarte con tareas, faltas y avisos de Mariana.",
        sent = false,
        time = "10:14 AM",
    ),
    AIMessage(
        id = 2,
        text = "¿Qué tareas tiene Mariana para mañana?",
        sent = true,
        time = "10:14 AM",
    ),
    AIMessage(
        id = 3,
        text = "Mariana tiene 1 tarea para mañana: Matemáticas, fracciones, ejercicios 1 al 10.",
        sent = false,
        time = "10:14 AM",
        showTaskCard = true,
        showFeedback = true,
    ),
)

@Composable
fun ParentAIScreen(
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(sampleAIMessages()) }
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
        AITopBar(onBack = onBack)

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
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WelcomeBubble()
                ChipsRow(
                    onChipSelected = { chip ->
                        draft = chip
                    },
                )
                messages.forEach { message ->
                    AIMessageBubble(message = message)
                }
            }
        }

        AIComposer(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    messages = messages + AIMessage(
                        id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
                        text = text,
                        sent = true,
                        time = now,
                    )
                    draft = ""
                }
            },
        )
    }
}

@Composable
private fun AITopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (backInteraction, backPress) = rememberPressScale(0.9f)
        Box(
            modifier = backPress
                .size(36.dp)
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
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF6B57), Color(0xFF2B2F77)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Asistente AulaSegura",
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.18).sp,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TertiaryFixed)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "IA",
                        color = TertiaryContainer,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }

        val (moreInteraction, morePress) = rememberPressScale(0.9f)
        Box(
            modifier = morePress
                .size(36.dp)
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
}

@Composable
private fun WelcomeBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 306.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, SurfaceVariant, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "¡Hola, Carlos! Soy tu asistente escolar. Puedo ayudarte con tareas, faltas y avisos de Mariana.",
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun ChipsRow(
    onChipSelected: (String) -> Unit,
) {
    val chips = listOf(
        "¿Qué tareas tiene mañana?",
        "¿Cómo justifico una falta?",
        "Resumen de asistencia del mes",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Box(
                modifier = pressModifier
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onChipSelected(chip) },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = chip,
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.24.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AIMessageBubble(message: AIMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (message.sent) {
            SentBubble(message)
        } else {
            ReceivedBubble(message)
        }
    }
}

@Composable
private fun ReceivedBubble(message: AIMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 306.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, SurfaceVariant, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.text,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (message.showTaskCard) {
                TaskCard()
            }
            if (message.showFeedback) {
                FeedbackRow(time = message.time)
            }
        }
    }
}

@Composable
private fun SentBubble(message: AIMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 306.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                .background(PrimaryContainer),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = message.text,
                color = OnPrimary,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message.time,
                color = OnPrimary.copy(alpha = 0.7f),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun TaskCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ver tarea",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
            )
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FeedbackRow(time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ThumbUp,
            contentDescription = "Útil",
            tint = Outline,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.ThumbDown,
            contentDescription = "No útil",
            tint = Outline,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = time,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun AIComposer(
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
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Grabar audio",
                    tint = Outline,
                    modifier = Modifier.size(20.dp),
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
                                    text = "Pregunta algo...",
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
