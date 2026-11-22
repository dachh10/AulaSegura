package com.example.aulasegura.ui.screens.admin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.AdminReportRangeSummary

import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.PillShape
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 4.1 — Panel Directivo. Stitch layout kept; every number now comes from Firestore. */

private val TileShape = RoundedCornerShape(20.dp)
private val HeroShape = RoundedCornerShape(24.dp)

private val PendingPillText = Color(0xFFD97706)
private val AttentionCardBg = Color(0xFFFFFBEB)
private val AttentionCardBorder = Color(0xFFFDE68A)
private val AttentionText = Color(0xFF92400E)

@Composable
fun AdminDashboardScreen(
    onNotificaciones: () -> Unit = {},
    onSolicitudes: () -> Unit = {},
    onAlumnosSinGrupo: () -> Unit = {},
    onEscanear: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    val viewModel: AdminDashboardViewModel = viewModel(
        factory = rememberAppContainer().viewModelFactory,
    )
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    var cameraDenied by remember { mutableStateOf(false) }
    // Ask exactly when the button is tapped: the system dialog appears right
    // away, with no extra screen between the button and the camera.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraDenied = !granted
        if (granted) onEscanear()
    }
    val activity = context as? Activity
    val cameraPermanentlyDenied = cameraDenied && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA,
        )

    val error = uiState.error
    val cards = if (uiState.loading) loadingCards() else dashboardCards(uiState.dashboard)
    val summary = if (uiState.loading) {
        "Cargando datos del plantel…"
    } else {
        summaryLabel(uiState.dashboard)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) { AdminTopBar(onNotificaciones = onNotificaciones) }
            Reveal(30) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionTile(
                        icon = Icons.Filled.Notifications,
                        count = uiState.pendingTeachers,
                        title = "Solicitudes de docentes",
                        subtitle = "${uiState.pendingTeachers} pendientes",
                        action = "Revisar",
                        tint = BrandCoral,
                        onClick = onSolicitudes,
                        modifier = Modifier.weight(1f),
                    )
                    ActionTile(
                        icon = Icons.Filled.PersonAdd,
                        count = uiState.ungroupedStudents,
                        title = "Alumnos sin grupo",
                        subtitle = if (uiState.ungroupedStudents == 0) {
                            "Sin asignar"
                        } else {
                            "${uiState.ungroupedStudents} sin asignar"
                        },
                        action = "Asignar",
                        tint = Primary,
                        onClick = onAlumnosSinGrupo,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Reveal(60) {
                AttendanceTodayCard(
                    message = todayAttendanceMessage(
                        summary = uiState.todaySummary,
                        loading = uiState.todayLoading,
                        error = uiState.todayError,
                    ),
                    summary = uiState.todaySummary,
                )
            }
            if (error != null) {
                Reveal(120) { DashboardErrorCard(message = error, onRetry = viewModel::refresh) }
            } else {
                Reveal(120) { CampusNumbers(cards = cards, summary = summary) }
                Reveal(180) {
                    TodayGroups(
                        groupCount = uiState.dashboard.groupCount,
                        onVerDetalles = onGrupos,
                    )
                }
            }
        }

        // Floating "Escanear alumno" button above the bottom bar (Stitch 07).
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 92.dp)
                .shadow(elevation = 6.dp, shape = PillShape)
                .clip(PillShape)
                .background(Primary)
                .clickable { cameraLauncher.launch(Manifest.permission.CAMERA) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = SurfaceContainerLowest,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Escanear alumno",
                    color = SurfaceContainerLowest,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }

        if (cameraDenied) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 152.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "Se necesita la cámara para escanear el QR",
                    color = Error,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                )
                if (cameraPermanentlyDenied) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Abrir ajustes",
                        color = BrandCoral,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            ).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                    )
                }
            }
        }

        AdminBottomBar(
            active = AdminTab.Panel,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> Unit
                    AdminTab.Usuarios -> onUsuarios()
                    AdminTab.Grupos -> onGrupos()
                    AdminTab.Reportes -> onReportes()
                    AdminTab.Mas -> onMas()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AdminTopBar(onNotificaciones: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "PR",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hola, directora Patricia",
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                )
                Text(
                    text = "Primaria Benito Juárez",
                    color = Outline,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onNotificaciones),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notificaciones",
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * The "attendance of today" card, fed by `reports.getTodaySummary()`.
 *
 * The message is computed by a pure helper: it states how many sessions were held
 * and the real attendance percentage, or says plainly that there are none. A failed
 * read shows its own error and never a fake 0%.
 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    count: Int,
    title: String,
    subtitle: String,
    action: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$count",
                color = tint,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(tint.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = OnSurface,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        )
        Text(
            text = subtitle,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = action,
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(tint)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AttendanceTodayCard(message: String, summary: AdminReportRangeSummary?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(PrimaryContainer)
            .border(1.dp, Primary.copy(alpha = 0.20f), HeroShape),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = OnPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = (-32).dp)
                .size(176.dp)
                .alpha(0.06f),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ASISTENCIA DE HOY",
                color = OnPrimary.copy(alpha = 0.70f),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (summary != null && !summary.empty) {
                // Circular ring + real legend, from the Stitch "InicioAdmin".
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AttendanceRing(percentage = summary.attendancePercentage)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val legend = attendanceLegend(summary)
                        val dotColors = listOf(BrandMint, Color(0xFFE5484D), Color(0xFFF59E0B))
                        legend.zip(dotColors).forEach { (label, dot) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dot),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = OnPrimary,
                                    fontFamily = DmSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = message,
                    color = OnPrimary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                )
            }
        }
    }
}

@Composable
private fun AttendanceRing(percentage: Int) {
    val stroke = with(LocalDensity.current) { 7.5.dp.toPx() }
    val radius = with(LocalDensity.current) { 36.dp.toPx() }
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(
                color = OnPrimary.copy(alpha = 0.20f),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
            // Progress arc: start at top, sweep the percentage of the circle.
            drawArc(
                color = BrandMint,
                startAngle = -90f,
                sweepAngle = percentage.coerceIn(0, 100) * 3.6f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percentage%",
                color = OnPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                text = "TOTAL",
                color = OnPrimary.copy(alpha = 0.70f),
                fontFamily = DmSans,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun DashboardErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(AttentionCardBg)
            .border(1.dp, AttentionCardBorder, TileShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            color = AttentionText,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
        TextButton(onClick = onRetry) {
            Text(text = "Reintentar", color = Primary, fontFamily = DmSans)
        }
    }
}

@Composable
private fun CampusNumbers(cards: List<DashboardCardUi>, summary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Plantel en números",
            color = OnSurface,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        Text(
            text = summary,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                card = cards[0],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StatTile(
                card = cards[1],
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
            StatTile(
                card = cards[2],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StatTile(
                card = cards[3],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StatTile(
    card: DashboardCardUi,
    modifier: Modifier = Modifier,
) {
    val isIncident = card.key == DashboardCardKey.INCIDENTS
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), TileShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isIncident) SecondaryFixed.copy(alpha = 0.5f)
                    else PrimaryFixed.copy(alpha = 0.6f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(card.key),
                contentDescription = null,
                tint = if (isIncident) SecondaryContainer else Primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = card.value,
            color = if (isIncident) Color(0xFFFF6B57) else OnSurface,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
        )
        Text(
            text = card.label,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private fun iconFor(key: DashboardCardKey): ImageVector = when (key) {
    DashboardCardKey.STUDENTS -> Icons.Filled.School
    DashboardCardKey.TEACHERS -> Icons.Filled.Badge
    DashboardCardKey.GROUPS -> Icons.Filled.Groups
    DashboardCardKey.INCIDENTS -> Icons.Filled.Warning
}

/** Real group count only: no invented per-group teacher names or attendance bars. */
@Composable
private fun TodayGroups(groupCount: Int, onVerDetalles: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Grupos hoy",
                color = OnSurface,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = "Ver detalles",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onVerDetalles),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HeroShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.6f), HeroShape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Grupos registrados",
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "En el plantel",
                            color = Outline,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                        )
                    }
                }
                Text(
                    text = formatCount(groupCount),
                    color = OnSurface,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }
        }
    }
}

