package com.example.aulasegura.ui.screens.parent

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.chat.chatInitials
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

/** 2.10 — Conversación de chat. Ported from Stitch screen `889c39e23e6142c09cd11698a057830e`. */

@Composable
fun ParentConversationScreen(
    onBack: () -> Unit,
    ui: ParentConversationUi = ParentConversationUi(),
    onDraftChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(ui.messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
            .imePadding(),
    ) {
        ConversationTopBar(
            title = ui.title,
            subtitle = ui.subtitle,
            online = ui.online,
            onBack = onBack,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (ui.loading) {
                    CenterHint("Cargando mensajes…")
                } else if (ui.messages.isEmpty()) {
                    CenterHint("Todavía no hay mensajes. Escribe el primero.")
                }

                ui.messages.forEach { message ->
                    message.dayLabel?.let { DayChip(label = it) }
                    MessageBubble(message = message)
                }

                ui.notice?.let { CenterHint(it) }
            }
        }

        MessageComposer(
            draft = ui.draft,
            sending = ui.sending,
            onDraftChange = onDraftChange,
            onSend = onSend,
        )
    }
}

@Composable
private fun CenterHint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Outline,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ConversationTopBar(
    title: String,
    subtitle: String,
    online: Boolean,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(Surface)) {
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
                        text = chatInitials(title),
                        color = SurfaceContainerLowest,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.18).sp,
                    )
                }
                if (online) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(TertiaryFixedDim)
                            .border(2.dp, Surface, CircleShape),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.18).sp,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (online) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TertiaryFixedDim),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (online) "$subtitle · En línea" else subtitle,
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
private fun DayChip(label: String) {
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
                text = label,
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
private fun MessageBubble(message: ParentMessageUi) {
    val shape = if (message.mine) {
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
        contentAlignment = if (message.mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 292.dp)
                .clip(shape)
                .background(if (message.mine) PrimaryContainer else SurfaceContainerLowest)
                .then(
                    if (message.mine) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, SurfaceVariant, shape)
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.text,
                color = if (message.mine) OnPrimary else MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.time,
                    color = if (message.mine) {
                        OnPrimary.copy(alpha = 0.7f)
                    } else {
                        Outline
                    },
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                if (message.mine) {
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
    sending: Boolean,
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
                    .background(if (sending) SecondaryContainer.copy(alpha = 0.6f) else SecondaryContainer)
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
