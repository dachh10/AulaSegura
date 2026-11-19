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
import androidx.compose.material3.CircularProgressIndicator
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
private val AmberTint = Color(0xFFFEF3C7)
private val AmberText = Color(0xFF78350F)

/**
 * Every notice the school addressed to this family's groups.
 *
 * Tapping a card opens it: the body unfolds and the read mark is written to
 * `users/{uid}/noticeReads/{noticeId}`. The notice document itself is never
 * written from this screen — the school owns it.
 */
@Composable
fun ParentNoticesScreen(
    ui: ParentNoticesUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectFilter: (ParentNoticeFilter) -> Unit,
    onMarkRead: (String) -> Unit,
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

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
                when (ui) {
                    is ParentNoticesUiState.Loaded -> {
                        Reveal(60) {
                            UnreadSummaryCard(
                                unreadCount = ui.unreadCount,
                                latestLabel = ui.latestLabel,
                            )
                        }
                        Reveal(120) {
                            NoticeFilters(
                                selected = ui.filter,
                                onSelect = onSelectFilter,
                            )
                        }
                        if (ui.error != null) {
                            Reveal(150) { ReadMarkError(message = ui.error) }
                        }
                        Reveal(180) {
                            NoticeList(
                                sections = ui.sections,
                                emptyMessage = ui.emptyMessage,
                                expandedId = expandedId,
                                onOpen = { id ->
                                    expandedId = if (expandedId == id) null else id
                                    onMarkRead(id)
                                },
                            )
                        }
                    }

                    ParentNoticesUiState.Loading -> Reveal(60) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = PrimaryContainer)
                        }
                    }

                    is ParentNoticesUiState.Failed -> Reveal(60) {
                        NoticesFailure(
                            message = ui.message,
                            onRetry = onRetry,
                        )
                    }
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
private fun UnreadSummaryCard(unreadCount: Int, latestLabel: String) {
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
                    text = latestLabel,
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
    selected: ParentNoticeFilter,
    onSelect: (ParentNoticeFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ParentNoticeFilter.entries.forEach { filter ->
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
    sections: List<ParentNoticeSectionUi>,
    emptyMessage: String,
    expandedId: String?,
    onOpen: (String) -> Unit,
) {
    if (sections.isEmpty()) {
        EmptyNotices(message = emptyMessage)
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = section.label,
                    color = InkSecondary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                section.notices.forEach { notice ->
                    NoticeCard(
                        notice = notice,
                        expanded = expandedId == notice.noticeId,
                        onClick = { onOpen(notice.noticeId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(
    notice: ParentNoticeRowUi,
    expanded: Boolean,
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
                .background(if (notice.fromSchool) IndigoTint else MintTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (notice.fromSchool) {
                    Icons.Filled.AccountBalance
                } else {
                    Icons.Filled.Person
                },
                contentDescription = null,
                tint = if (notice.fromSchool) PrimaryContainer else MintDark,
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
                        text = notice.timeLabel,
                        color = if (notice.unread) InkSecondary else InkMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                    if (notice.unread) {
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
                text = notice.body,
                color = InkSecondary,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
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
private fun ReadMarkError(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmberTint)
            .border(1.dp, AmberText.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = AmberText,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = message,
            color = AmberText,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun EmptyNotices(message: String) {
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
            text = message,
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun NoticesFailure(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "No se pudieron cargar",
            color = InkPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(14.dp))
        val (interaction, pressModifier) = rememberPressScale(0.95f)
        Box(
            modifier = pressModifier
                .clip(CircleShape)
                .background(PrimaryContainer)
                .clickable(interactionSource = interaction, indication = null, onClick = onRetry)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Reintentar",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}
