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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant
import com.example.aulasegura.ui.theme.TertiaryContainer
import com.example.aulasegura.ui.theme.TertiaryFixed

/**
 * 2.11 — Asistente IA. Ported from Stitch screen `bd8b739044634567b469e6d2fbcafedb`.
 *
 * The screen is a render of [ParentAIUi]: the greeting, the history and the
 * failures come from `ParentAIViewModel`. Nothing here invents a message — the
 * sample conversation the visual block used is gone, and the suggestions answer
 * with real data when the AI is off (`CONTEXT.md` §9.2).
 */
@Composable
fun ParentAIScreen(
    ui: ParentAIUi,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAsk: (ParentAIQuestion) -> Unit,
    onRetry: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(ui.messages.size, ui.sending, ui.notice) {
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

        if (ui.faqOnly) {
            AssistantBanner(
                text = "El asistente no está disponible en este momento. " +
                    "Las sugerencias responden con los datos de tus hijos.",
            )
        }

        if (ui.contextError != null) {
            AssistantBanner(
                text = ui.contextError,
                action = "Reintentar",
                onAction = onRetry,
            )
        }

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
                Reveal(delayMillis = 0) {
                    WelcomeBubble(text = ui.welcome.ifBlank {
                        "¡Hola! Soy tu asistente escolar."
                    })
                }

                ChipsRow(
                    enabled = !ui.sending && !ui.loading,
                    onChipSelected = onAsk,
                )

                if (ui.loading) {
                    Reveal(delayMillis = 80) {
                        ReceivedBubble(
                            text = "Un momento: estoy leyendo los datos de tus hijos.",
                        )
                    }
                }

                ui.messages.forEachIndexed { index, message ->
                    key(message.id) {
                        Reveal(delayMillis = (index.coerceAtMost(MAX_STAGGERED) * 60)) {
                            if (message.mine) {
                                SentBubble(message = message)
                            } else {
                                ReceivedBubble(
                                    text = message.text,
                                    time = message.time,
                                    showFeedback = true,
                                    tasks = message.tasks,
                                    onOpenTask = onOpenTask,
                                )
                            }
                        }
                    }
                }

                if (ui.sending) {
                    ReceivedBubble(text = "Escribiendo…")
                }

                if (ui.notice != null) {
                    NoticeRow(text = ui.notice)
                }
            }
        }

        AIComposer(
            draft = ui.draft,
            enabled = !ui.sending && !ui.faqOnly && !ui.loading,
            blocked = ui.faqOnly,
            onDraftChange = onDraftChange,
            onSend = onSend,
        )
    }
}

/**
 * One line above the conversation: the kill switch, a read failure with its retry,
 * or nothing.
 */
@Composable
private fun AssistantBanner(
    text: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PrimaryFixed)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = Primary,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        if (action != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Box(
                modifier = pressModifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = action,
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun NoticeRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
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

    }
}

@Composable
private fun WelcomeBubble(text: String) {
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
                text = text,
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
    enabled: Boolean,
    onChipSelected: (ParentAIQuestion) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ParentAIQuestion.entries.forEach { chip ->
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Box(
                modifier = pressModifier
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .clickable(
                        enabled = enabled,
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onChipSelected(chip) },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = chip.label,
                    color = Primary.copy(alpha = if (enabled) 1f else 0.5f),
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
private fun ReceivedBubble(
    text: String,
    time: String = "",
    showFeedback: Boolean = false,
    tasks: List<ParentAITaskUi> = emptyList(),
    onOpenTask: (String) -> Unit = {},
) {
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
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            tasks.forEach { task ->
                TaskCard(task = task, onOpenTask = onOpenTask)
            }

            if (showFeedback) {
                FeedbackRow(time = time)
            }
        }
    }
}

@Composable
private fun SentBubble(message: ParentAIMessageUi) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 306.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                .background(PrimaryContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = message.text,
                color = OnPrimary,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (message.time.isNotBlank()) {
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
}

/**
 * "Ver tarea", drawn only when the app knows which task the answer is about.
 *
 * The design paints it inside the sample answer; a free-text answer from the model
 * cannot produce a task id, so it never gets a card. The suggestion about
 * tomorrow's tasks does, because those rows come from the same data the screen
 * loaded and the id is real.
 */
@Composable
private fun TaskCard(
    task: ParentAITaskUi,
    onOpenTask: (String) -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.97f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onOpenTask(task.taskId) },
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            Text(
                text = "${task.subject} — ${task.title}",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
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
        if (time.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = time,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun AIComposer(
    draft: String,
    enabled: Boolean,
    blocked: Boolean,
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
                        color = if (focused && enabled) PrimaryContainer else SurfaceVariant,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
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
                                    text = if (blocked) {
                                        "Usa una sugerencia"
                                    } else {
                                        "Pregunta algo..."
                                    },
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
                    .background(SecondaryContainer.copy(alpha = if (enabled) 1f else 0.4f))
                    .clickable(
                        enabled = enabled,
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

/** How many bubbles keep the entrance delay; later ones appear at once. */
private const val MAX_STAGGERED = 5
