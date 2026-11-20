package com.example.aulasegura.ui.screens.teacher

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
import androidx.compose.material.icons.filled.Edit
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
import com.example.aulasegura.ui.chat.chatInitials
import com.example.aulasegura.ui.components.TeacherBottomBar
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.PrimaryFixedDim
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.Tertiary
import com.example.aulasegura.ui.theme.TertiaryFixed

/**
 * Teacher — Mensajes. Ported from Stitch screen `1d7ea3e523ae4f6aa558cfdcbef64653`
 * ("Mensajes", project 7677408035297541086).
 *
 * The rows are real conversations (Firestore summaries, messages in RTDB); search
 * and the group chip are filtered in `TeacherChatViewModel`, and the "new
 * message" button opens the tutors of the teacher's own students.
 */

private val ChatHairline = OutlineVariant.copy(alpha = 0.6f)
private val ChatDivider = OutlineVariant.copy(alpha = 0.4f)

private data class TeacherConversation(
    val id: String,
    val name: String,
    val subtitle: String,
    val preview: String,
    val time: String,
    val unread: Int? = null,
    val initials: String? = null,
    val avatarColor: Color = PrimaryFixed,
    val initialsColor: Color = PrimaryContainer,
    val icon: ImageVector? = null,
    val group: String? = null,
)


@Composable
fun TeacherChatScreen(
    onOpenConversation: (String) -> Unit,
    onSelectTab: (TeacherTab) -> Unit,
    ui: TeacherChatUi = TeacherChatUi(),
    onNuevoMensaje: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onFilterChange: (String) -> Unit = {},
    onContactsDismiss: () -> Unit = {},
    onStartConversation: (String) -> Unit = {},
) {
    val conversations = ui.rows.mapIndexed { index, row -> row.toRender(index) }

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
                .padding(bottom = 112.dp),
        ) {
            Reveal(0) { ChatHeader(onNuevoMensaje = onNuevoMensaje) }
            Reveal(60) { ChatSearchField(query = ui.query, onQueryChange = onQueryChange) }
            Reveal(120) {
                GroupFilterRow(
                    filters = ui.filters,
                    activeFilter = ui.activeFilter,
                    onFilterChange = onFilterChange,
                )
            }
            Reveal(180) {
                when {
                    ui.loading -> EmptyConversations(message = "Cargando conversaciones…")
                    ui.notice != null && conversations.isEmpty() ->
                        EmptyConversations(message = ui.notice)
                    conversations.isEmpty() -> EmptyConversations()
                    else -> ConversationList(
                        conversations = conversations,
                        onOpen = onOpenConversation,
                    )
                }
            }
        }

        TeacherBottomBar(
            active = TeacherTab.Chat,
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

/**
 * The real rows, dressed with the colors the mockup gave each sample contact.
 *
 * Avatar colors are rotated by position instead of stored: a conversation has no
 * color in the schema, and inventing one per document would be a field nobody
 * reads.
 */
private fun TeacherChatRowUi.toRender(index: Int): TeacherConversation = TeacherConversation(
    id = id,
    name = name,
    subtitle = subtitle,
    preview = preview,
    time = time,
    unread = unread.takeIf { it > 0 },
    initials = initials,
    avatarColor = AVATAR_COLORS[index % AVATAR_COLORS.size],
    initialsColor = PrimaryContainer,
    group = group,
)

private val AVATAR_COLORS: List<Color> = listOf(
    PrimaryFixed,
    SecondaryFixed,
    TertiaryFixed,
    PrimaryFixedDim.copy(alpha = 0.6f),
)

/**
 * Tutor picker. A scrim plus a card instead of a Material sheet: the screen is a
 * plain `Box`, and the mockup has no sheet to port.
 */
@Composable
private fun ContactsSheet(
    ui: TeacherChatUi,
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
            text = "Tutores de los alumnos en tus grupos",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        when {
            ui.contactsLoading -> Text(
                text = "Buscando tutores…",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            ui.contacts.isEmpty() -> Text(
                text = ui.notice ?: "No hay tutores vinculados en tus grupos.",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            else -> ui.contacts.forEach { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStartConversation(contact.otherUid) }
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
                            text = chatInitials(contact.label),
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
                            color = Outline,
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
private fun ChatHeader(onNuevoMensaje: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mensajes",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.56).sp,
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
private fun ChatSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (focused) PrimaryContainer else ChatHairline,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
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
                            text = "Buscar familia o alumno",
                            color = Outline,
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

@Composable
private fun GroupFilterRow(
    filters: List<String>,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filters.forEach { filter ->
            GroupFilterChip(
                label = filter,
                selected = filter == activeFilter,
                onClick = { onFilterChange(filter) },
            )
        }
    }
}

@Composable
private fun GroupFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = pressModifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, OutlineVariant.copy(alpha = 0.7f), shape)
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) OnPrimary else PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<TeacherConversation>,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
    ) {
        conversations.forEachIndexed { index, conversation ->
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = ChatDivider)
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
    conversation: TeacherConversation,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(conversation.avatarColor)
                .border(1.dp, OutlineVariant.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (conversation.icon != null) {
                Icon(
                    imageVector = conversation.icon,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Text(
                    text = conversation.initials.orEmpty(),
                    color = conversation.initialsColor,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.name,
                    modifier = Modifier.weight(1f, fill = false),
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = conversation.time,
                    color = Outline,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            Text(
                text = conversation.subtitle,
                color = Outline,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.preview,
                    modifier = Modifier.weight(1f),
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                conversation.unread?.let { count ->
                    Spacer(Modifier.width(8.dp))
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
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversations(message: String = "Sin conversaciones") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}
