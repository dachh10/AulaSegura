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
 * Visual shell only: search, group filters and navigation are UI state.
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

private fun sampleTeacherConversations(): List<TeacherConversation> = listOf(
    TeacherConversation(
        id = "1",
        name = "Carlos Gómez Pérez",
        subtitle = "Padre de Mariana · 3° B",
        preview = "Gracias, profesor. ¿Qué temas incluye?",
        time = "10:41",
        unread = 1,
        initials = "CG",
        avatarColor = PrimaryFixed,
        initialsColor = PrimaryContainer,
        group = "3° B",
    ),
    TeacherConversation(
        id = "2",
        name = "Laura Ríos Mena",
        subtitle = "Madre de Bruno · 3° B",
        preview = "Buenos días, ¿puedo justificar la falta de ayer?",
        time = "09:20",
        unread = 2,
        initials = "LR",
        avatarColor = SecondaryFixed,
        initialsColor = Secondary,
        group = "3° B",
    ),
    TeacherConversation(
        id = "3",
        name = "Jorge Domínguez",
        subtitle = "Padre de Camila · 3° B",
        preview = "Entendido, muchas gracias.",
        time = "Ayer",
        initials = "JD",
        avatarColor = TertiaryFixed,
        initialsColor = Tertiary,
        group = "3° B",
    ),
    TeacherConversation(
        id = "4",
        name = "Dirección escolar",
        subtitle = "Administración",
        preview = "Recuerden entregar las listas antes de las 10:00.",
        time = "Lun",
        icon = Icons.Outlined.AccountBalance,
        avatarColor = SurfaceContainerHigh,
        initialsColor = PrimaryContainer,
    ),
    TeacherConversation(
        id = "5",
        name = "Marta Escobar",
        subtitle = "Madre de Diego · 3° B",
        preview = "Perfecto, lo revisamos en casa.",
        time = "Vie",
        initials = "ME",
        avatarColor = PrimaryFixedDim.copy(alpha = 0.6f),
        initialsColor = PrimaryContainer,
        group = "3° B",
    ),
)

private val teacherGroupFilters = listOf("Todos", "3° B", "3° A", "2° C")

@Composable
fun TeacherChatScreen(
    onOpenConversation: (String) -> Unit,
    onSelectTab: (TeacherTab) -> Unit,
    onNuevoMensaje: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("Todos") }
    val conversations = remember { sampleTeacherConversations() }

    val visible = conversations.filter { conversation ->
        val matchesGroup =
            activeFilter == "Todos" || conversation.group == activeFilter
        val matchesQuery =
            query.isBlank() ||
                conversation.name.contains(query, ignoreCase = true) ||
                conversation.subtitle.contains(query, ignoreCase = true) ||
                conversation.preview.contains(query, ignoreCase = true)
        matchesGroup && matchesQuery
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
                .padding(bottom = 112.dp),
        ) {
            Reveal(0) { ChatHeader(onNuevoMensaje = onNuevoMensaje) }
            Reveal(60) { ChatSearchField(query = query, onQueryChange = { query = it }) }
            Reveal(120) {
                GroupFilterRow(
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it },
                )
            }
            Reveal(180) {
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

        TeacherBottomBar(
            active = TeacherTab.Chat,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
        teacherGroupFilters.forEach { filter ->
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
private fun EmptyConversations() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sin conversaciones",
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}
