package com.example.aulasegura.ui.screens.teacher

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.aulasegura.domain.model.IncidentLocation
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.SchoolSite
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.location.haversineMeters
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import java.io.File
import java.util.Locale

/**
 * 3.9 — Registrar incidente (panel del docente).
 *
 * Reproduce el mockup de Stitch `2b5038f52ef64c8aa1f541755b2860e7`:
 * selector de alumno, gravedad, ubicación con mapa esquemático y geocerca,
 * evidencia (foto/video), descripción, fecha y pie fijo con el CTA coral.
 *
 * El mapa es un dibujo vectorial del mockup (no Google Maps) para que la pantalla
 * se vea idéntica sin depender de la API key. Tocar el mapa mueve el marcador y
 * actualiza las coordenadas; el botón de ubicación lo regresa al centro del plantel.
 */

private val Hairline = Color(0xFFE6E1D8)
private val Coral = Color(0xFFFF6B57)
private val CoralDeep = Color(0xFFAE3123)
private val MapPaper = Color(0xFFECE9E1)
private val Mint = Color(0xFF5ED3B0)
private val GreenChipBg = Color(0xFFECFDF5)
private val GreenChipText = Color(0xFF065F46)
private val GreenChipBorder = Color(0xFFA7F3D0)
private val GreenChipIcon = Color(0xFF059669)

private val CardShape = RoundedCornerShape(24.dp)
private val StudentShape = RoundedCornerShape(20.dp)
private val MapShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(20.dp)
private val TileShape = RoundedCornerShape(16.dp)

/** Fracción del mapa donde vive el marcador en el mockup (180/328, 82/180). */
private const val MARKER_X = 0.5488f
private const val MARKER_Y = 0.4556f

/** Coordenadas del patio central que muestra el mockup. */
private const val BASE_LAT = 19.292600
private const val BASE_LON = -99.655700

private sealed interface Evidence {
    val label: String

    data class Photo(val bitmap: Bitmap?, override val label: String) : Evidence
    data class Video(override val label: String) : Evidence
}

@Composable
fun TeacherIncidentNewScreen(
    ui: TeacherIncidentUi,
    school: SchoolSite? = null,
    onBack: () -> Unit = {},
    onSave: (IncidentLocation?) -> Unit = {},
    onSelectStudent: (String) -> Unit = {},
    onSeverityChange: (IncidentSeverity) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
) {
    val context = LocalContext.current

    var marker by remember { mutableStateOf(Offset(MARKER_X, MARKER_Y)) }
    var pickerOpen by remember { mutableStateOf(false) }
    var evidence by remember {
        mutableStateOf(listOf<Evidence>(Evidence.Photo(bitmap = null, label = "IMG_01")))
    }
    var nextIndex by remember { mutableStateOf(2) }

    fun addEvidence(item: Evidence) {
        if (evidence.size < 3) evidence = evidence + item
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            addEvidence(Evidence.Photo(bitmap, String.format(Locale.US, "IMG_%02d", nextIndex)))
            nextIndex += 1
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            addEvidence(Evidence.Video(String.format(Locale.US, "VID_%02d", nextIndex)))
            nextIndex += 1
        }
    }

    fun captureVideo() {
        val uri = newEvidenceVideoUri(context)
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("incidente", uri)
        }
        runCatching { videoLauncher.launch(intent) }
    }

    val latitude = String.format(Locale.US, "%.6f", BASE_LAT + (MARKER_Y - marker.y) * 0.002f)
    val longitude = String.format(Locale.US, "%.6f", BASE_LON + (marker.x - MARKER_X) * 0.002f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .statusBarsPadding(),
    ) {
        IncidentTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .revealOnEnter(60)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ui.message?.let { message ->
                IncidentMessageStrip(
                    message = message,
                    saved = ui.saved && ui.sync != SyncPhase.FAILED,
                )
            }

            StudentSelectorCard(
                selected = ui.selected,
                students = ui.students,
                expanded = pickerOpen,
                onToggle = { pickerOpen = !pickerOpen },
                onSelect = { student ->
                    pickerOpen = false
                    onSelectStudent(student.id)
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Gravedad", trailing = "Requerido")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IncidentSeverity.entries.forEach { level ->
                        SeverityPill(
                            level = level,
                            selected = level == ui.severity,
                            onClick = { onSeverityChange(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Ubicación del incidente")
                IncidentLocationCard(
                    marker = marker,
                    onMarkerChange = { marker = it },
                    onUseCurrentLocation = { marker = Offset(MARKER_X, MARKER_Y) },
                    latitude = latitude,
                    longitude = longitude,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Evidencia")
                EvidenceGrid(
                    evidence = evidence,
                    onTakePhoto = { photoLauncher.launch(null) },
                    onRecordVideo = { captureVideo() },
                    onRemove = { item ->
                        evidence = evidence.filterNot { it === item }
                    },
                )
                Text(
                    text = "${evidence.size} de 3 archivos · máx. 30 s de video",
                    color = Outline,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Descripción")
                DescriptionField(
                    value = ui.description,
                    onValueChange = onDescriptionChange,
                )
            }

            DateTimeCard()
        }

        val latitudeValue = BASE_LAT + (MARKER_Y - marker.y) * 0.002f
        val longitudeValue = BASE_LON + (marker.x - MARKER_X) * 0.002f
        val distance = school?.let {
            haversineMeters(latitudeValue, longitudeValue, it.latitude, it.longitude)
        }

        IncidentFooter(
            enabled = ui.canSave,
            saving = ui.saving,
            saved = ui.saved,
            onSave = {
                onSave(
                    // The pin is where the teacher says it happened, so that is
                    // what gets stored. `accuracy` stays null on purpose: a pin
                    // is not a GPS reading and must not pretend to be one.
                    IncidentLocation(
                        latitude = latitudeValue,
                        longitude = longitudeValue,
                        accuracyMeters = null,
                        distanceMeters = distance,
                        verified = if (school == null || distance == null) {
                            false
                        } else {
                            distance <= school.radiusMeters
                        },
                    ),
                )
            },
        )
    }
}

// ZONE 1 — Top app bar
@Composable
private fun IncidentTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (backInteraction, backPress) = rememberPressScale(0.94f)
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(backPress)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CircleShape)
                .clickable(
                    interactionSource = backInteraction,
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Registrar incidente",
            modifier = Modifier.weight(1f),
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.02).em,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val (helpInteraction, helpPress) = rememberPressScale(0.94f)
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(helpPress)
                .clip(CircleShape)
                .clickable(
                    interactionSource = helpInteraction,
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = "Ayuda",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ZONE 2 — Student selector
@Composable
private fun StudentSelectorCard(
    selected: IncidentStudentUi?,
    students: List<IncidentStudentUi>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (IncidentStudentUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = StudentShape)
            .clip(StudentShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), StudentShape),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The mockup uses a portrait photo; the rest of the app imitates
            // portraits with the student's monogram in the same ring.
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCFBF1))
                        .border(1.dp, OutlineVariant.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selected?.initials ?: "?",
                        color = Color(0xFF0F766E),
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF66DBB7))
                        .border(2.dp, Color.White, CircleShape),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ALUMNO",
                        color = Outline,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        letterSpacing = 0.05.em,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selected?.groupLabel ?: "—",
                        color = Primary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                Text(
                    text = selected?.fullName ?: "Elegir alumno",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val (interaction, pressModifier) = rememberPressScale(0.92f)
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(pressModifier)
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Cambiar alumno",
                tint = Outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }

        if (expanded) {
            HorizontalDivider(
                thickness = 1.dp,
                color = OutlineVariant.copy(alpha = 0.30f),
            )

            if (students.isEmpty()) {
                Text(
                    text = "Todavía no hay alumnos en tus grupos.",
                    color = Outline,
                    fontFamily = DmSans,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(14.dp),
                )
            } else {
                students.forEach { student ->
                    StudentOptionRow(
                        student = student,
                        selected = student.id == selected?.id,
                        onClick = { onSelect(student) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentOptionRow(
    student: IncidentStudentUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) SurfaceContainerHigh else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = student.fullName,
            color = Primary,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = student.groupLabel,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}

// Aviso del guardado: qué contestó el dispositivo y en qué fase va el envío.
@Composable
private fun IncidentMessageStrip(message: String, saved: Boolean) {
    val accent = if (saved) Color(0xFF0F766E) else Color(0xFF9E640B)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (saved) Icons.Filled.CheckCircle else Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = accent,
            fontFamily = DmSans,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

// ZONE 3 — Severity
@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
        Text(
            text = trailing,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Primary,
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(start = 2.dp),
    )
}

@Composable
private fun SeverityPill(
    level: IncidentSeverity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interaction, press) = rememberPressScale(0.95f)

    val accent = when (level) {
        IncidentSeverity.LEVE -> Color(0xFF9E640B)
        IncidentSeverity.MODERADO -> CoralDeep
        IncidentSeverity.GRAVE -> Color(0xFFB9252B)
    }
    val border = when (level) {
        IncidentSeverity.LEVE -> Color(0xFFF2A93B)
        IncidentSeverity.MODERADO -> Coral
        IncidentSeverity.GRAVE -> Color(0xFFE5484D)
    }
    val iconColor = when (level) {
        IncidentSeverity.LEVE -> Color(0xFFD88A1A)
        IncidentSeverity.MODERADO -> Coral
        IncidentSeverity.GRAVE -> Color(0xFFE5484D)
    }
    val icon: ImageVector = when (level) {
        IncidentSeverity.LEVE -> Icons.Outlined.Flag
        IncidentSeverity.MODERADO -> Icons.Outlined.CheckCircle
        IncidentSeverity.GRAVE -> Icons.Outlined.Warning
    }

    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .height(40.dp)
            .then(press)
            .shadow(
                elevation = if (selected) 3.dp else 0.dp,
                shape = shape,
                ambientColor = Coral.copy(alpha = 0.35f),
                spotColor = Coral.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(if (selected) Coral else border.copy(alpha = 0.10f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Coral.copy(alpha = 0.30f) else border.copy(alpha = 0.50f),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else icon,
            contentDescription = null,
            tint = if (selected) Color.White else iconColor,
            modifier = Modifier.size(if (selected) 18.dp else 17.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = level.label,
            color = if (selected) Color.White else accent,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            maxLines = 1,
        )
    }
}

// ZONE 4 — Location
@Composable
private fun IncidentLocationCard(
    marker: Offset,
    onMarkerChange: (Offset) -> Unit,
    onUseCurrentLocation: () -> Unit,
    latitude: String,
    longitude: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape)
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(MapShape)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), MapShape)
                .background(MapPaper)
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        if (size.width > 0 && size.height > 0) {
                            onMarkerChange(
                                Offset(
                                    x = (tap.x / size.width).coerceIn(0.08f, 0.92f),
                                    y = (tap.y / size.height).coerceIn(0.18f, 0.92f),
                                ),
                            )
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCampusMap()
            }

            // Campus label
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Patio Central · Secundaria",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }

            // Incident marker, anchored by the tip of the teardrop.
            Column(
                modifier = Modifier
                    .offset(x = maxWidth * marker.x, y = maxHeight * marker.y)
                    .graphicsLayer {
                        translationX = -size.width / 2f
                        translationY = -size.height
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val pulse = rememberInfiniteTransition(label = "incidentPulse")
                val dotAlpha by pulse.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "incidentDot",
                )

                Row(
                    modifier = Modifier
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(percent = 50))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { alpha = dotAlpha }
                            .clip(CircleShape)
                            .background(Coral),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Incidente",
                        color = Primary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                IncidentTeardrop()
            }

            // My location control
            val (interaction, pressModifier) = rememberPressScale(0.90f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(36.dp)
                    .then(pressModifier)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CircleShape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onUseCurrentLocation,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = "Centrar en mi ubicación",
                    tint = Primary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Toca el mapa o arrastra el marcador para cambiar el lugar",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CoordinateBox(
                label = "Latitud",
                value = latitude,
                modifier = Modifier.weight(1f),
            )
            CoordinateBox(
                label = "Longitud",
                value = longitude,
                modifier = Modifier.weight(1f),
            )
        }

        val (useInteraction, usePress) = rememberPressScale(0.97f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(usePress)
                .clip(RoundedCornerShape(percent = 50))
                .background(SurfaceContainerLowest)
                .border(1.dp, Primary.copy(alpha = 0.30f), RoundedCornerShape(percent = 50))
                .clickable(
                    interactionSource = useInteraction,
                    indication = null,
                    onClick = onUseCurrentLocation,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NearMe,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Usar mi ubicación actual",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.02.em,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .background(GreenChipBg)
                .border(1.dp, GreenChipBorder, RoundedCornerShape(percent = 50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = GreenChipIcon,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Dentro del plantel · Verificada",
                color = GreenChipText,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                letterSpacing = (-0.01).em,
            )
        }
    }
}

@Composable
private fun CoordinateBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label.uppercase(Locale.US),
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            lineHeight = 13.sp,
        )
        Text(
            text = value,
            color = Primary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.02).em,
        )
    }
}

/** Coral teardrop of the mockup (30 × 36), with the white inner dot. */
@Composable
private fun IncidentTeardrop() {
    Canvas(modifier = Modifier.size(width = 30.dp, height = 36.dp)) {
        val w = size.width
        val h = size.height

        fun drop(offsetY: Float): Path = Path().apply {
            moveTo(15f * w / 30f, offsetY)
            cubicTo(
                6.71573f * w / 30f, offsetY,
                0f * w / 30f, 6.71573f * h / 36f + offsetY,
                0f * w / 30f, 15f * h / 36f + offsetY,
            )
            cubicTo(
                0f * w / 30f, 24.5f * h / 36f + offsetY,
                15f * w / 30f, 36f * h / 36f + offsetY,
                15f * w / 30f, 36f * h / 36f + offsetY,
            )
            cubicTo(
                15f * w / 30f, 36f * h / 36f + offsetY,
                30f * w / 30f, 24.5f * h / 36f + offsetY,
                30f * w / 30f, 15f * h / 36f + offsetY,
            )
            cubicTo(
                30f * w / 30f, 6.71573f * h / 36f + offsetY,
                23.2843f * w / 30f, offsetY,
                15f * w / 30f, offsetY,
            )
            close()
        }

        drawPath(drop(1.5.dp.toPx()), Color.Black.copy(alpha = 0.16f))
        drawPath(drop(0f), Coral)
        drawCircle(
            color = Color.White,
            radius = 5f * w / 30f,
            center = Offset(15f * w / 30f, 14f * h / 36f),
        )
    }
}

// ZONE 5 — Evidence
private sealed interface EvidenceCell {
    data object AddPhoto : EvidenceCell
    data object AddVideo : EvidenceCell
    data class Thumbnail(val item: Evidence) : EvidenceCell
}

@Composable
private fun EvidenceGrid(
    evidence: List<Evidence>,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onRemove: (Evidence) -> Unit,
) {
    val cells = buildList {
        if (evidence.size < 3) {
            add(EvidenceCell.AddPhoto)
            add(EvidenceCell.AddVideo)
        }
        evidence.forEach { add(EvidenceCell.Thumbnail(it)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(3).forEach { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowCells.forEach { cell ->
                    when (cell) {
                        EvidenceCell.AddPhoto -> AddEvidenceTile(
                            icon = Icons.Outlined.PhotoCamera,
                            label = "Foto",
                            onClick = onTakePhoto,
                            modifier = Modifier.weight(1f),
                        )

                        EvidenceCell.AddVideo -> AddEvidenceTile(
                            icon = Icons.Outlined.Videocam,
                            label = "Video",
                            onClick = onRecordVideo,
                            modifier = Modifier.weight(1f),
                        )

                        is EvidenceCell.Thumbnail -> EvidenceThumbnail(
                            item = cell.item,
                            onRemove = { onRemove(cell.item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(3 - rowCells.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AddEvidenceTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)

    Column(
        modifier = modifier
            .height(80.dp)
            .then(pressModifier)
            .clip(TileShape)
            .background(Color.White.copy(alpha = 0.70f))
            .dashedBorder(
                color = OutlineVariant.copy(alpha = 0.70f),
                cornerRadius = 16.dp,
                strokeWidth = 2.dp,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun EvidenceThumbnail(
    item: Evidence,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .shadow(elevation = 2.dp, shape = TileShape)
            .clip(TileShape)
            .background(Color(0xFFDCE5E1))
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), TileShape),
    ) {
        when (item) {
            is Evidence.Photo -> {
                val bitmap = item.bitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Evidencia fotográfica",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // The mockup shows a real photograph of the courtyard; it is
                    // imitated with a small vector scene instead of a stock asset.
                    CourtyardSnapshot(modifier = Modifier.fillMaxSize())
                }
            }

            is Evidence.Video -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4A4B57), Color(0xFF1F2027)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video de evidencia",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        val (closeInteraction, closePress) = rememberPressScale(0.88f)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .then(closePress)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = closeInteraction,
                    indication = null,
                    onClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Eliminar archivo",
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }

        Text(
            text = item.label,
            color = Color.White,
            fontFamily = JetBrainsMono,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.50f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** Vector imitation of the courtyard evidence photo in the mockup. */
@Composable
private fun CourtyardSnapshot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFFDCE6E3), size = size)
        drawRect(
            color = Color(0xFFD8D2C6),
            topLeft = Offset(0f, h * 0.30f),
            size = Size(w, h * 0.30f),
        )
        drawRect(
            color = Color(0xFFCFC9BF),
            topLeft = Offset(0f, h * 0.60f),
            size = Size(w, h * 0.40f),
        )
        drawRect(
            color = Color(0xFFC3BCAC),
            topLeft = Offset(w * 0.66f, h * 0.34f),
            size = Size(w * 0.34f, h * 0.36f),
        )

        // Distant trees.
        drawCircle(
            color = Color(0xFFA9C6B4),
            radius = w * 0.13f,
            center = Offset(w * 0.12f, h * 0.44f),
        )
        drawCircle(
            color = Color(0xFFB8D2C0),
            radius = w * 0.09f,
            center = Offset(w * 0.28f, h * 0.46f),
        )

        // Concrete bench.
        drawRoundRect(
            color = Color(0xFFB6AE9E),
            topLeft = Offset(w * 0.08f, h * 0.72f),
            size = Size(w * 0.52f, h * 0.10f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        drawRect(
            color = Color(0xFFA8A090),
            topLeft = Offset(w * 0.14f, h * 0.82f),
            size = Size(w * 0.04f, h * 0.12f),
        )
        drawRect(
            color = Color(0xFFA8A090),
            topLeft = Offset(w * 0.50f, h * 0.82f),
            size = Size(w * 0.04f, h * 0.12f),
        )

        // Faint daylight wash, like the documentary-style photo of the mockup.
        drawRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset.Zero,
            size = Size(w, h * 0.45f),
        )
    }
}

// ZONE 6 — Description
@Composable
private fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = FieldShape)
            .clip(FieldShape)
            .background(SurfaceContainerLowest)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) Primary else OutlineVariant.copy(alpha = 0.40f),
                shape = FieldShape,
            )
            .heightIn(min = 96.dp)
            .padding(12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 300) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            textStyle = TextStyle(
                color = Color(0xFF1C1C19),
                fontFamily = DmSans,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary),
            maxLines = 5,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Describe lo ocurrido con claridad",
                            color = Outline.copy(alpha = 0.70f),
                            fontFamily = DmSans,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${value.length}/300",
            modifier = Modifier.fillMaxWidth(),
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.End,
        )
    }
}

// ZONE 7 — Date & time
@Composable
private fun DateTimeCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Primary.copy(alpha = 0.05f))
            .border(1.dp, Primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fecha y hora",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Mar 3 nov 2026 · 10:42",
            color = Primary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.02).em,
            maxLines = 1,
        )
    }
}

// ZONE 8 — Sticky footer
@Composable
private fun IncidentFooter(
    enabled: Boolean,
    saving: Boolean,
    saved: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = OutlineVariant.copy(alpha = 0.30f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Si no hay internet, se guardará y se enviará después.",
                    color = Outline,
                    fontFamily = DmSans,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val (interaction, pressModifier) = rememberPressScale(0.98f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(pressModifier)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(percent = 50),
                        ambientColor = Coral.copy(alpha = 0.32f),
                        spotColor = Coral.copy(alpha = 0.32f),
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (enabled) Coral else Coral.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onSave,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        saved -> "Incidente guardado"
                        saving -> "Guardando…"
                        else -> "Guardar incidente"
                    },
                    color = Color.White,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF1C1C19).copy(alpha = 0.20f)),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Helpers

private fun newEvidenceVideoUri(context: Context): Uri {
    val directory = File(context.cacheDir, "incidentes").apply { mkdirs() }
    val file = File(directory, "incidente_${System.currentTimeMillis()}.mp4")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
            ),
        ),
    )
}

/**
 * Vector replica of the schematic campus map in the mockup (viewBox 328×180).
 * Drawn at 1 dp per SVG unit and centred horizontally, so it crops exactly like
 * the mockup's `preserveAspectRatio="slice"` on a 360 dp phone.
 */
private fun DrawScope.drawCampusMap() {
    val u = 1.dp.toPx()
    val offsetX = (size.width - 328f * u) / 2f

    fun x(value: Float) = offsetX + value * u
    fun y(value: Float) = value * u

    drawRect(color = Color(0xFFF4F2EC), size = size)

    // Green areas
    val lowerLawn = Path().apply {
        moveTo(x(-10f), y(130f))
        cubicTo(x(30f), y(120f), x(80f), y(140f), x(110f), y(185f))
        lineTo(x(-10f), y(185f))
        close()
    }
    drawPath(lowerLawn, Color(0xFFE2F5EE))

    val upperLawn = Path().apply {
        moveTo(x(210f), y(-10f))
        cubicTo(x(240f), y(20f), x(270f), y(30f), x(335f), y(15f))
        lineTo(x(335f), y(-10f))
        close()
    }
    drawPath(upperLawn, Color(0xFFE2F5EE))

    // Sports pitch
    drawRoundRect(
        color = Color(0xFFD6F0E6),
        topLeft = Offset(x(230f), y(85f)),
        size = Size(75f * u, 50f * u),
        cornerRadius = CornerRadius(6f * u),
    )
    drawRoundRect(
        color = Color(0xFFBCE3D5),
        topLeft = Offset(x(230f), y(85f)),
        size = Size(75f * u, 50f * u),
        cornerRadius = CornerRadius(6f * u),
        style = Stroke(width = 1.5f * u),
    )
    drawCircle(
        color = Color(0xFFBCE3D5),
        radius = 10f * u,
        center = Offset(x(267f), y(110f)),
        style = Stroke(width = 1f * u),
    )

    // Campus roads
    fun road(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, round: Boolean) {
        drawLine(
            color = Color.White,
            start = Offset(x(x1), y(y1)),
            end = Offset(x(x2), y(y2)),
            strokeWidth = width * u,
            cap = if (round) StrokeCap.Round else StrokeCap.Butt,
        )
    }

    road(-20f, 65f, 348f, 65f, 12f, round = true)
    road(140f, -10f, 140f, 190f, 14f, round = true)
    road(40f, 65f, 40f, 190f, 8f, round = false)
    road(140f, 125f, 340f, 125f, 9f, round = false)
    road(-10f, 30f, 140f, 30f, 7f, round = false)

    // Walkway
    val walkway = Path().apply {
        moveTo(x(90f), y(65f))
        cubicTo(x(90f), y(95f), x(120f), y(110f), x(140f), y(110f))
    }
    drawPath(
        path = walkway,
        color = Color(0xFFDDD7CB),
        style = Stroke(
            width = 1.5f * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 3f * u)),
        ),
    )

    // Buildings
    fun building(left: Float, top: Float, width: Float, height: Float) {
        val topLeft = Offset(x(left), y(top))
        val blockSize = Size(width * u, height * u)
        val corner = CornerRadius(4f * u)
        drawRoundRect(
            color = Color(0xFFE8E4DA),
            topLeft = topLeft,
            size = blockSize,
            cornerRadius = corner,
        )
        drawRoundRect(
            color = Color(0xFFD3CDC0),
            topLeft = topLeft,
            size = blockSize,
            cornerRadius = corner,
            style = Stroke(width = 1f * u),
        )
    }

    building(55f, 15f, 60f, 35f)
    building(165f, 15f, 55f, 35f)
    building(70f, 85f, 50f, 35f)

    // School geofence
    val campusCenter = Offset(x(140f), y(85f))
    drawCircle(
        color = Mint.copy(alpha = 0.14f),
        radius = 62f * u,
        center = campusCenter,
    )
    drawCircle(
        color = Mint,
        radius = 62f * u,
        center = campusCenter,
        style = Stroke(
            width = 2f * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u, 3f * u)),
        ),
    )

    // Campus pin with graduation cap
    drawCircle(
        color = Color(0x332B2F77),
        radius = 13f * u,
        center = Offset(x(140f), y(86f)),
    )
    drawCircle(
        color = Color(0xFF2B2F77),
        radius = 11f * u,
        center = campusCenter,
    )

    val capX = 130f
    val capY = 68f
    val cap = Path().apply {
        moveTo(x(capX + 5f), y(capY + 8f))
        lineTo(x(capX + 10f), y(capY + 5.5f))
        lineTo(x(capX + 15f), y(capY + 8f))
        lineTo(x(capX + 10f), y(capY + 10.5f))
        close()
    }
    drawPath(cap, Color.White)

    val strap = Path().apply {
        moveTo(x(capX + 7.5f), y(capY + 10.2f))
        lineTo(x(capX + 7.5f), y(capY + 12.5f))
        cubicTo(
            x(capX + 7.5f), y(capY + 13.5f),
            x(capX + 12.5f), y(capY + 13.5f),
            x(capX + 12.5f), y(capY + 12.5f),
        )
        lineTo(x(capX + 12.5f), y(capY + 10.2f))
    }
    drawPath(
        path = strap,
        color = Color.White,
        style = Stroke(width = 1.1f * u, cap = StrokeCap.Round),
    )
}
