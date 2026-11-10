package com.example.aulasegura.ui.screens.parent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.PrimaryContainer

/** 2.x — Avisos para Familias. Ported from Stitch screen `a60924077dd1436e895010e33504e0c1`. */

private val InkPrimary = Color(0xFF1B1D3A)
private val InkSecondary = Color(0xFF6B6E8F)
private val InkMuted = Color(0xFF8A8EA8)
private val WarmBorder = Color(0xFFE6E1D8)
private val IndigoTint = Color(0xFFE8E9F7)
private val CoralTint = Color(0xFFFFEBE8)
private val MintTint = Color(0xFFE6FAF4)
private val MintDark = Color(0xFF15803D)

private enum class NoticeFilter(val label: String) {
    Todos("Todos"),
    Escuela("Escuela"),
    MiGrupo("Mi grupo"),
    Urgentes("Urgentes"),
}

private enum class NoticeSource { School, Teacher }

private data class Notice(
    val id: Int,
    val sourceName: String,
    val time: String,
    val title: String,
    val preview: String,
    val source: NoticeSource,
    val category: NoticeFilter,
    val urgent: Boolean,
    val section: String,
    val initiallyUnread: Boolean,
)

private fun sampleNotices(): List<Notice> = listOf(
    Notice(
        id = 1,
        sourceName = "Dirección",
        time = "Hoy, 09:10",
        title = "Suspensión de clases el 17 de noviembre",
        preview = "Por junta de consejo técnico no habrá clases este día. " +
            "Las actividades se reanudan el martes 18.",
        source = NoticeSource.School,
        category = NoticeFilter.Escuela,
        urgent = true,
        section = "HOY",
        initiallyUnread = true,
    ),
    Notice(
        id = 2,
        sourceName = "Prof. Salinas · 3° B",
        time = "Hoy, 08:05",
        title = "Material para la feria de ciencias",
        preview = "Traer cartulina, plumones y un objeto reciclado para el proyecto del viernes.",
        source = NoticeSource.Teacher,
        category = NoticeFilter.MiGrupo,
        urgent = false,
        section = "HOY",
        initiallyUnread = true,
    ),
    Notice(
        id = 3,
        sourceName = "Dirección",
        time = "Lun 3 nov",
        title = "Reunión de padres de familia",
        preview = "Se llevará a cabo el viernes 14 a las 17:00 en el patio principal de la escuela.",
        source = NoticeSource.School,
        category = NoticeFilter.Escuela,
        urgent = false,
        section = "ESTA SEMANA",
        initiallyUnread = false,
    ),
    Notice(
        id = 4,
        sourceName = "Profa. Laura Mendoza",
        time = "Dom 2 nov",
        title = "Lectura del mes",
        preview = "Los alumnos deberán leer el cuento asignado y comentarlo en clase.",
        source = NoticeSource.Teacher,
        category = NoticeFilter.MiGrupo,
        urgent = false,
        section = "ESTA SEMANA",
        initiallyUnread = false,
    ),
    Notice(
        id = 5,
        sourceName = "Dirección",
        time = "Vie 31 oct",
        title = "Festival del Día de Muertos",
        preview = "Invitamos a toda la comunidad escolar a participar en nuestra ofrenda tradicional...",
        source = NoticeSource.School,
        category = NoticeFilter.Escuela,
        urgent = false,
        section = "ESTA SEMANA",
        initiallyUnread = false,
    ),
)

@Composable
fun ParentNoticesScreen(onBack: () -> Unit) {
    var filter by remember { mutableStateOf(NoticeFilter.Todos) }
    var readIds by remember { mutableStateOf(emptySet<Int>()) }
    val notices = remember { sampleNotices() }

    val unreadCount = notices.count { it.initiallyUnread && it.id !in readIds }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f,
            color = PrimaryContainer,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Reveal(0) { NoticesTopBar(onBack = onBack) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Reveal(60) { UnreadSummaryCard(unreadCount = unreadCount) }
                Reveal(120) {
                    NoticeFilters(
                        selected = filter,
                        onSelect = { filter = it },
                    )
                }
                Reveal(180) {
                    NoticeList(
                        filter = filter,
                        notices = notices,
                        readIds = readIds,
                        onOpen = { id -> readIds = readIds + id },
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NoticesTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (interaction, pressModifier) = rememberPressScale(0.9f)
        Box(
            modifier = pressModifier
                .size(44.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, WarmBorder, CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Regresar",
                tint = InkPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Avisos",
            color = InkPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun UnreadSummaryCard(unreadCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PrimaryContainer),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.07f,
            color = Color.White,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandCoral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = if (unreadCount == 1) {
                        "1 aviso sin leer"
                    } else {
                        "$unreadCount avisos sin leer"
                    },
                    color = Color.White,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (unreadCount == 0) "Estás al día" else "Último: hoy, 09:10",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun NoticeFilters(
    selected: NoticeFilter,
    onSelect: (NoticeFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NoticeFilter.entries.forEach { filter ->
            NoticeFilterChip(
                label = filter.label,
                selected = filter == selected,
                onClick = { onSelect(filter) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NoticeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    val background by animateColorAsState(
        targetValue = if (selected) PrimaryContainer else Color.White,
        animationSpec = tween(200),
        label = "noticeFilterBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) Color.White else InkPrimary,
        animationSpec = tween(200),
        label = "noticeFilterForeground",
    )

    Box(
        modifier = modifier
            .then(pressModifier)
            .height(36.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, WarmBorder, CircleShape)
                },
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun NoticeList(
    filter: NoticeFilter,
    notices: List<Notice>,
    readIds: Set<Int>,
    onOpen: (Int) -> Unit,
) {
    val visible = when (filter) {
        NoticeFilter.Todos -> notices
        NoticeFilter.Escuela -> notices.filter { it.category == NoticeFilter.Escuela }
        NoticeFilter.MiGrupo -> notices.filter { it.category == NoticeFilter.MiGrupo }
        NoticeFilter.Urgentes -> notices.filter { it.urgent }
    }

    if (visible.isEmpty()) {
        EmptyNotices()
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf("HOY", "ESTA SEMANA").forEach { section ->
            val sectionNotices = visible.filter { it.section == section }
            if (sectionNotices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = section,
                        color = InkSecondary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    sectionNotices.forEach { notice ->
                        NoticeCard(
                            notice = notice,
                            unread = notice.initiallyUnread && notice.id !in readIds,
                            onClick = { onOpen(notice.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(
    notice: Notice,
    unread: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (notice.source == NoticeSource.School) IndigoTint else MintTint,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (notice.source == NoticeSource.School) {
                    Icons.Filled.AccountBalance
                } else {
                    Icons.Filled.Person
                },
                contentDescription = null,
                tint = if (notice.source == NoticeSource.School) PrimaryContainer else MintDark,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notice.sourceName,
                    color = InkSecondary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notice.time,
                        color = if (unread) InkSecondary else InkMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                    if (unread) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandCoral),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = notice.title,
                color = InkPrimary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notice.preview,
                color = InkSecondary,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (notice.urgent) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CoralTint)
                        .border(1.dp, BrandCoral.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = BrandCoral,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Importante",
                        color = BrandCoral,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotices() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(IndigoTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sin avisos en este filtro",
            color = InkPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Cuando haya novedades aparecerán aquí.",
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}
