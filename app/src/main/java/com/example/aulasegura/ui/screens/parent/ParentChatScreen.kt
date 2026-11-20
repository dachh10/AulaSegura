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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 2.9 — Mensajes. Ported from Stitch screen `c52d05bdfa5b42f494d9cc0d7062ff90`. */

@Composable
fun ParentChatScreen(
    onOpenConversation: (String) -> Unit,
    onSelectTab: (ParentTab) -> Unit,
    ui: ParentChatUi = ParentChatUi(),
    onNuevoMensaje: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onContactsDismiss: () -> Unit = {},
    onStartConversation: (String) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 104.dp),
        ) {
            Reveal(0) { ChatHeader(onNuevoMensaje = onNuevoMensaje) }
            Reveal(60) { SearchField(query = ui.query, onQueryChange = onQueryChange) }
            Reveal(120) {
                when {
                    ui.loading -> LoadingConversations()
                    ui.rows.isEmpty() -> EmptyConversations(notice = ui.notice)
                    else -> ConversationList(
                        conversations = ui.rows,
                        onOpen = onOpenConversation,
                    )
                }
            }
        }

        ParentBottomBar(
            active = ParentTab.Chat,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (ui.contactsOpen) {
            ContactsSheet(
                ui = ui,
                onDismiss = onContactsDismiss,
                onStartConversation = onStartConversation,
            )
        }
    }
}

@Composable
private fun ChatHeader(onNuevoMensaje: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mensajes",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.64).sp,
        )
        val (interaction, pressModifier) = rememberPressScale(0.95f)
        Box(
            modifier = pressModifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PrimaryContainer)
                .clickable(interactionSource = interaction, indication = null, onClick = onNuevoMensaje),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Nuevo mensaje",
                tint = OnPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (focused) PrimaryContainer else OutlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(PrimaryContainer),
            interactionSource = interactionSource,
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar conversación",
                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<ParentChatRowUi>,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = OutlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        conversations.forEachIndexed { index, conversation ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = OutlineVariant.copy(alpha = 0.2f),
                )
            }
            ConversationRow(
                conversation = conversation,
                onClick = { onOpen(conversation.id) },
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ParentChatRowUi,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PrimaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = conversation.initials,
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.18).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = conversation.subtitle,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.33.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = conversation.preview,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(6.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = conversation.time,
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
            conversation.unread?.let { count ->
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(SecondaryContainer)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = count.toString(),
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingConversations() {
    EmptyCard { 
        Text(
            text = "Cargando conversaciones…",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun EmptyConversations(notice: String?) {
    EmptyCard {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            tint = OutlineVariant,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sin conversaciones",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = notice ?: "Toca el lápiz para escribirle al docente.",
            color = OutlineVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun EmptyCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/**
 * Teacher picker. A scrim plus a card instead of a Material sheet: the screen is
 * a plain `Box`, and the mockup has no sheet to port.
 */
@Composable
private fun ContactsSheet(
    ui: ParentChatUi,
    onDismiss: () -> Unit,
    onStartConversation: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceContainerLowest)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Nuevo mensaje",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = "Docentes de los grupos de tus hijos",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )

            when {
                ui.contactsLoading -> Text(
                    text = "Buscando docentes…",
                    color = OutlineVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                ui.contacts.isEmpty() -> Text(
                    text = ui.notice ?: "No hay docentes asignados al grupo de tus hijos.",
                    color = OutlineVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                else -> ui.contacts.forEach { contact ->
                    TeacherRow(contact = contact, onClick = { onStartConversation(contact.otherUid) })
                }
            }
        }
    }
}

@Composable
private fun TeacherRow(contact: ParentTeacherUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.initials,
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.label,
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = contact.groupLabel.ifBlank { "Sin grupo" },
                color = OutlineVariant,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
    }
}
