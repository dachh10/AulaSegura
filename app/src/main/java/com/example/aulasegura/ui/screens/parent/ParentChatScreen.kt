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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.aulasegura.ui.theme.PrimaryFixedDim
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixedDim

/** 2.9 — Mensajes. Ported from Stitch screen `c52d05bdfa5b42f494d9cc0d7062ff90`. */

private data class Conversation(
    val id: String,
    val name: String,
    val role: String,
    val preview: String,
    val time: String,
    val unread: Int? = null,
    val initials: String? = null,
    val avatarColor: Color = PrimaryFixed,
    val icon: ImageVector? = null,
    val online: Boolean = false,
)

private fun sampleConversations(): List<Conversation> = listOf(
    Conversation(
        id = "1",
        name = "Prof. Ricardo Salinas",
        role = "Docente · 3° B",
        preview = "Mariana tendrá examen el jueves, ¿pueden…",
        time = "10:42",
        unread = 2,
        initials = "RS",
        online = true,
    ),
    Conversation(
        id = "2",
        name = "Dirección escolar",
        role = "Dirección institucional",
        preview = "Gracias por confirmar su asistencia.",
        time = "Ayer",
        icon = Icons.Outlined.AccountBalance,
        avatarColor = SurfaceContainerHigh,
    ),
    Conversation(
        id = "3",
        name = "Profa. Laura Mendoza (Español)",
        role = "Docente · 3° B",
        preview = "Revisé la tarea, quedó muy bien.",
        time = "Lun",
        initials = "LM",
        avatarColor = PrimaryFixedDim.copy(alpha = 0.5f),
        online = true,
    ),
)

@Composable
fun ParentChatScreen(
    onOpenConversation: (String) -> Unit,
    onSelectTab: (ParentTab) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val conversations = remember { sampleConversations() }
    val visible = conversations.filter { conversation ->
        query.isBlank() ||
            conversation.name.contains(query, ignoreCase = true) ||
            conversation.role.contains(query, ignoreCase = true) ||
            conversation.preview.contains(query, ignoreCase = true)
    }

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
            Reveal(0) { ChatHeader() }
            Reveal(60) { SearchField(query = query, onQueryChange = { query = it }) }
            Reveal(120) {
                if (visible.isEmpty()) {
                    EmptyConversations()
                } else {
                    ConversationList(
                        conversations = visible,
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
    }
}

@Composable
private fun ChatHeader() {
    Text(
        text = "Mensajes",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        color = PrimaryContainer,
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.64).sp,
    )
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
    conversations: List<Conversation>,
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
    conversation: Conversation,
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
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(conversation.avatarColor),
                contentAlignment = Alignment.Center,
            ) {
                if (conversation.icon != null) {
                    Icon(
                        imageVector = conversation.icon,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    Text(
                        text = conversation.initials.orEmpty(),
                        color = PrimaryContainer,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
            }
            if (conversation.online) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(TertiaryFixedDim)
                        .border(2.dp, SurfaceContainerLowest, CircleShape),
                )
            }
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
                text = conversation.role,
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
private fun EmptyConversations() {
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
    }
}
