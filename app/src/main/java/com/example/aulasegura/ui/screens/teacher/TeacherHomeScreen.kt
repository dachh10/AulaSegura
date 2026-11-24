package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import coil.compose.AsyncImage
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.TeacherBottomBar
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.AulaTextStyles
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.CardShape
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed

/** 3.1 — Inicio Docente. Ported 1:1 from the Stitch markup of the teacher dashboard. */

/** Hairline borders used across every card in the mockup (#E6E1D8). */
private val Hairline = Color(0xFFE6E1D8)

/** Mockup accents that are not part of the shared M3 palette. */
private val DangerRed = Color(0xFFE5484D)
private val SyncAmber = Color(0xFFD97706)
private val SuccessGreen = Color(0xFF16A34A)

private val StatShape = RoundedCornerShape(20.dp)
private val RowShape = RoundedCornerShape(16.dp)

@Composable
fun TeacherHomeScreen(
    ui: TeacherHomeUi = TeacherHomeUi(),
    fullName: String = "",
    subjects: List<String> = emptyList(),
    photoUrl: String? = null,
    onIrAGrupos: () -> Unit = {},
    /** The group and subject of the next class; `null` group when there is none. */
    onIrAPaseLista: (String?, String) -> Unit = { _, _ -> },
    onIrARegistrarIncidente: () -> Unit = {},
    onIrAPublicarTarea: () -> Unit = {},
    onIrAPublicarAviso: () -> Unit = {},
    onIrAJustificaciones: () -> Unit = {},
    /** Re-reads the report backlog; called every time the home resumes. */
    onRefreshJustifications: () -> Unit = {},
    onIrAChat: () -> Unit = {},
    onIrAHistorial: () -> Unit = {},
    onIrANotificaciones: () -> Unit = {},
    onIrAPerfil: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val refreshJustifications = rememberUpdatedState(onRefreshJustifications)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshJustifications.value()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        // The mockup has a single scrollable column: the recent activity list is
        // rendered inline (never as a LazyColumn) so no infinite constraints are
        // passed to a vertically scrollable child.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) {
                TeacherTopBar(
                    fullName = fullName,
                    subjects = subjects,
                    photoUrl = photoUrl,
                    unread = ui.unreadNotifications,
                    onNotifications = onIrANotificaciones,
                )
            }
            Reveal(60) {
                TeacherHeroCard(
                    nextClass = ui.nextClass,
                    notice = ui.scheduleNotice,
                    onPasarLista = {
                        onIrAPaseLista(ui.nextClass?.groupId, ui.nextClass?.subjectId.orEmpty())
                    },
                )
            }
            Reveal(120) { TeacherTodaySummary(summary = ui.summary) }
            Reveal(160) {
                PendingJustificationsCard(
                    count = ui.pendingJustifications,
                    onClick = onIrAJustificaciones,
                )
            }
            Reveal(180) {
                TeacherQuickActions(
                    onRegistrarIncidente = onIrARegistrarIncidente,
                    onPublicarTarea = onIrAPublicarTarea,
                    onPublicarAviso = onIrAPublicarAviso,
                    onMisGrupos = onIrAGrupos,
                )
            }
            Reveal(240) { TeacherRecentActivity(activity = ui.activity) }
        }

        TeacherBottomBar(
            active = TeacherTab.Inicio,
            onSelect = { tab ->
                when (tab) {
                    TeacherTab.Inicio -> Unit
                    TeacherTab.Grupos -> onIrAGrupos()
                    TeacherTab.Chat -> onIrAChat()
                    TeacherTab.Historial -> onIrAHistorial()
                    TeacherTab.Perfil -> onIrAPerfil()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ZONE 1 — Top bar
@Composable
private fun TeacherTopBar(
    fullName: String,
    subjects: List<String>,
    photoUrl: String?,
    unread: Int,
    onNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val avatarModifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.5.dp, Hairline, CircleShape)

        if (photoUrl == null) {
            Box(modifier = avatarModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = profileInitials(fullName),
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                )
            }
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil",
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = teacherGreeting(fullName),
                color = OnboardingHeading,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.01).em,
            )
            Text(
                text = profileSubtitle(subjects),
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.02.em,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        NotificationBell(unread = unread, onClick = onNotifications)
    }
}

@Composable
private fun NotificationBell(unread: Int, onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(pressModifier)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notificaciones",
                tint = OnboardingHeading,
                modifier = Modifier.size(22.dp),
            )
        }
        // A zero is not a badge: the bell stays clean until there is something to
        // read, and goes back to clean as soon as the list is opened.
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(if (unread > 9) 20.dp else 16.dp)
                    .clip(CircleShape)
                    .background(BrandCoral)
                    .border(2.dp, BrandCream, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unread > 99) "99+" else unread.toString(),
                    color = OnPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}

// ZONE 2 — Next class hero card
@Composable
private fun TeacherHeroCard(
    nextClass: NextClassUi?,
    notice: String?,
    onPasarLista: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CardShape)
            .background(PrimaryContainer),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(160.dp)
                .alpha(0.07f),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = nextClass?.let { heroLabel(it) } ?: "HORARIO",
                    style = AulaTextStyles.dataMonoSm,
                    color = Color.White.copy(alpha = 0.70f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                LocationChip()
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = nextClass?.headline?.ifBlank { "Clase de hoy" } ?: "No hay clases próximas",
                color = Color.White,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.015).em,
            )
            Text(
                text = nextClass?.let { "${it.studentCount} alumnos" } ?: notice.orEmpty(),
                color = Color.White.copy(alpha = 0.75f),
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            PassListButton(onClick = onPasarLista)
        }
    }
}

/** `SIGUIENTE CLASE · 10:00` or `EN CURSO · 10:00 – 10:45`. */
private fun heroLabel(nextClass: NextClassUi): String = when (nextClass.status) {
    ClassStatus.IN_PROGRESS -> "EN CURSO · ${nextClass.timeLabel}"
    ClassStatus.NEXT -> "SIGUIENTE CLASE · ${nextClass.timeLabel}"
}

@Composable
private fun LocationChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(BrandMint.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = TertiaryFixed,
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Dentro de la escuela",
            color = TertiaryFixed,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.03.em,
        )
    }
}

@Composable
private fun PassListButton(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(pressModifier)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(percent = 50),
                ambientColor = BrandCoral.copy(alpha = 0.35f),
                spotColor = BrandCoral.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(percent = 50))
            .background(BrandCoral)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.FactCheck,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Pasar lista",
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

// ZONE 3 — Today summary
@Composable
private fun TeacherTodaySummary(summary: TodaySummaryUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle("Resumen de hoy")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TeacherStatCard(
                value = "${summary.listsPassed}/${summary.groupsTotal}",
                label = "Listas pasadas",
                valueColor = OnboardingHeading,
                valueSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            TeacherStatCard(
                value = summary.absences.toString(),
                label = "Faltas hoy",
                valueColor = DangerRed,
                valueSize = 22.sp,
                modifier = Modifier.weight(1f),
            )
            TeacherSyncCard(
                value = summary.pendingSync.toString(),
                label = "Por sincronizar",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeacherStatCard(
    value: String,
    label: String,
    valueColor: Color,
    valueSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(StatShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, StatShape)
            .padding(10.dp),
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = valueSize,
            lineHeight = valueSize,
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatLabel(label)
    }
}

@Composable
private fun TeacherSyncCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(StatShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, StatShape)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = SyncAmber,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.CloudQueue,
                contentDescription = null,
                tint = SyncAmber,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        StatLabel(label)
    }
}

@Composable
private fun StatLabel(label: String) {
    Text(
        text = label,
        color = OnboardingMuted,
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.03.em,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = OnboardingHeading,
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
}

/**
 * The report backlog and the way into the inbox.
 *
 * It stays visible at zero: the reviewed reports are still worth reading, and a
 * card that disappears would leave the teacher without a door to them.
 */
@Composable
private fun PendingJustificationsCard(count: Int, onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)
    val tint = if (count > 0) DangerRed else PrimaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(pressModifier)
            .clip(StatShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, StatShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Justificaciones pendientes",
                color = OnboardingHeading,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            Text(
                text = when (count) {
                    0 -> "Sin reportes por revisar"
                    1 -> "1 reporte por revisar"
                    else -> "$count reportes por revisar"
                },
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = OutlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ZONE 4 — Quick actions
@Composable
private fun TeacherQuickActions(
    onRegistrarIncidente: () -> Unit,
    onPublicarTarea: () -> Unit,
    onPublicarAviso: () -> Unit,
    onMisGrupos: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle("Acciones rápidas")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TeacherQuickCard(
                    icon = Icons.Filled.Shield,
                    title = "Registrar incidente",
                    subtitle = "Foto o video",
                    onClick = onRegistrarIncidente,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                TeacherQuickCard(
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    title = "Publicar tarea",
                    subtitle = "Para tu grupo",
                    onClick = onPublicarTarea,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TeacherQuickCard(
                    icon = Icons.Filled.Campaign,
                    title = "Publicar aviso",
                    subtitle = "Avisar a las familias",
                    onClick = onPublicarAviso,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                TeacherQuickCard(
                    icon = Icons.Filled.Groups,
                    title = "Mis grupos",
                    subtitle = "3 grupos",
                    onClick = onMisGrupos,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun TeacherQuickCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Column(
        modifier = modifier
            .then(pressModifier)
            .clip(StatShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, StatShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryContainer.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            color = OnboardingHeading,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
        Text(
            text = subtitle,
            color = OnboardingMuted,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.02.em,
        )
    }
}

// ZONE 5 — Recent activity
@Composable
private fun TeacherRecentActivity(activity: List<ActivityRowUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle("Actividad reciente")

        if (activity.isEmpty()) {
            Text(
                text = "Todavía no hay actividad. Lo que hagas hoy aparece aquí.",
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activity.forEach { row ->
                TeacherActivityRow(
                    icon = row.kind.icon,
                    iconColor = row.kind.iconColor,
                    title = row.title,
                    time = row.timeLabel,
                )
            }
        }
    }
}

/** The mockup's palette, one icon per source of activity. */
private val ActivityKind.icon: ImageVector
    get() = when (this) {
        ActivityKind.ROLL_CALL -> Icons.Filled.CheckCircle
        ActivityKind.INCIDENT -> Icons.Filled.Report
        ActivityKind.TASK -> Icons.AutoMirrored.Outlined.Assignment
        ActivityKind.NOTICE -> Icons.Filled.Campaign
    }

private val ActivityKind.iconColor: Color
    get() = when (this) {
        ActivityKind.ROLL_CALL -> SuccessGreen
        ActivityKind.INCIDENT -> SyncAmber
        ActivityKind.TASK -> PrimaryContainer
        ActivityKind.NOTICE -> PrimaryContainer
    }

@Composable
private fun TeacherActivityRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    time: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color(0xFF1C1C19),
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = time,
            style = AulaTextStyles.dataMonoSm,
            color = OnboardingMuted,
        )
    }
}

