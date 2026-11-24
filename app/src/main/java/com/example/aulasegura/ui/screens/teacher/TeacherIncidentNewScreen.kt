package com.example.aulasegura.ui.screens.teacher

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.aulasegura.ui.profile.rememberCameraPermissionGate
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentLocation
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.SchoolSite
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.location.haversineMeters
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.ui.components.CampusGeo
import com.example.aulasegura.ui.components.CampusMap
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
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.util.Locale

/**
 * 3.9 — Registrar incidente (panel del docente).
 *
 * Reproduce el mockup de Stitch `2b5038f52ef64c8aa1f541755b2860e7`:
 * selector de alumno, gravedad, ubicación con mapa real y geocerca, evidencia
 * (foto/video), descripción, fecha y pie fijo con el CTA coral.
 *
 * El mapa es Google Maps ([CampusMap]): el marcador es una coordenada real que el
 * docente coloca tocando el mapa, y es exactamente lo que se guarda en
 * `IncidentLocation`. Antes las coordenadas salían de la fracción del dibujo, así
 * que el documento apuntaba siempre al mismo patio aunque el incidente hubiera
 * pasado en otro lado.
 */

private val Hairline = Color(0xFFE6E1D8)
private val Coral = Color(0xFFFF6B57)
private val CoralDeep = Color(0xFFAE3123)
private val MapPaper = Color(0xFFECE9E1)
private val GreenChipBg = Color(0xFFECFDF5)
private val GreenChipText = Color(0xFF065F46)
private val GreenChipBorder = Color(0xFFA7F3D0)
private val GreenChipIcon = Color(0xFF059669)

private val CardShape = RoundedCornerShape(24.dp)
private val StudentShape = RoundedCornerShape(20.dp)
private val MapShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(20.dp)
private val TileShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherIncidentNewScreen(
    ui: TeacherIncidentUi,
    school: SchoolSite? = null,
    onBack: () -> Unit = {},
    onSave: (IncidentLocation?) -> Unit = {},
    onSelectStudent: (String) -> Unit = {},
    onSeverityChange: (IncidentSeverity) -> Unit = {},
    onCategoryChange: (IncidentCategory) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onPhotoTaken: (File) -> Unit = {},
    onRemoveEvidence: (String) -> Unit = {},
) {
    val context = LocalContext.current

    // Lectura real del GPS. Si el permiso ya está concedido, llega la primera
    // lectura sola y el formulario abre con el pin puesto.
    val location = rememberCurrentLocation()
    val locationButton = locationButtonUi(
        permissionGranted = location.permissionGranted,
        hasFix = location.fix != null,
        locating = location.locating,
        failed = location.failed,
    )

    var pin by remember { mutableStateOf<LatLng?>(null) }
    var placedByTeacher by remember { mutableStateOf(false) }
    var pinAccuracy by remember { mutableStateOf<Double?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }

    // El mapa necesita un centro real. El GPS es la verdad; el centro del plantel
    // es la siguiente coordenada real; sin ninguna de las dos no hay pin, porque
    // inventar una coordenada es peor que dejar el mapa sin marcar.
    LaunchedEffect(location.fix, school) {
        if (placedByTeacher || pin != null) return@LaunchedEffect
        val fix = location.fix
        val seeded = when {
            fix != null -> LatLng(fix.latitude, fix.longitude)
            school != null -> LatLng(school.latitude, school.longitude)
            else -> null
        } ?: return@LaunchedEffect

        pin = seeded
        pinAccuracy = fix?.accuracyMeters?.toDouble()
    }

    fun placePin(point: LatLng) {
        pin = point
        placedByTeacher = true
        // Un punto tocado es un lugar elegido, no una lectura: no lleva precisión.
        pinAccuracy = null
    }

    fun useCurrentLocation() {
        when (locationButton.action) {
            LocationButtonAction.REQUEST_PERMISSION -> location.requestPermission()
            LocationButtonAction.USE_FIX -> {
                val fix = location.fix ?: return
                pin = LatLng(fix.latitude, fix.longitude)
                pinAccuracy = fix.accuracyMeters?.toDouble()
                placedByTeacher = true
            }
            // La lectura ya está en curso: el botón está deshabilitado y un toque
            // extra no debe apilar tentativas.
            LocationButtonAction.WAIT -> Unit
            LocationButtonAction.RETRY -> location.retry()
        }
    }

    val verdict = incidentPinVerdict(pin?.latitude, pin?.longitude, school)

    // The camera app writes into this file; the ViewModel compresses it and keeps
    // the result. `TakePicture` and not `TakePicturePreview`: the preview hands
    // back a thumbnail, and evidence has to be the real photo.
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val captured = pendingCapture
        pendingCapture = null
        if (saved && captured != null) {
            onPhotoTaken(captured)
        } else {
            captured?.delete()
            if (captured != null) {
                Toast.makeText(
                    context,
                    "La cámara no guardó la evidencia. Intenta de nuevo.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val requireCameraPermission = rememberCameraPermissionGate(
        onDenied = {
            Toast.makeText(
                context,
                "Permite el acceso a la cámara para adjuntar evidencia.",
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    fun capturePhoto() {
        requireCameraPermission {
            val file = newEvidencePhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCapture = file
            runCatching { photoLauncher.launch(uri) }
                .onFailure {
                    pendingCapture = null
                    file.delete()
                    Toast.makeText(
                        context,
                        "No se pudo abrir la cámara. Intenta de nuevo.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

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
                SectionHeader(
                    title = "Gravedad",
                    trailing = if (ui.severity == null) "Requerido" else null,
                )
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
                SectionLabel(text = "Categoría")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IncidentCategory.entries.forEach { option ->
                        CategoryChip(
                            label = option.label,
                            selected = option == ui.category,
                            onClick = { onCategoryChange(option) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Ubicación del incidente")
                IncidentLocationCard(
                    school = school,
                    pin = pin,
                    verdict = verdict,
                    locating = location.locating,
                    permissionGranted = location.permissionGranted,
                    button = locationButton,
                    onPinChange = ::placePin,
                    onUseCurrentLocation = { useCurrentLocation() },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Evidencia")
                EvidenceGrid(
                    evidence = ui.evidence,
                    limit = ui.mediaLimit,
                    onTakePhoto = { capturePhoto() },
                    onRemove = { item -> onRemoveEvidence(item.localPath) },
                )
                Text(
                    text = incidentMediaCountLabel(ui.evidence.size, ui.mediaLimit) +
                        if (ui.preparing) " · preparando…" else "",
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

            DateTimeCard(timestamp = incidentTimestampLabel(ui.occurredAt))
        }

        IncidentFooter(
            enabled = ui.canSave,
            saving = ui.saving,
            saved = ui.saved,
            onSave = {
                onSave(
                    // The pin is where the teacher says it happened, so that is
                    // what gets stored. `accuracy` only travels when the pin came
                    // from a real GPS reading: a tapped point must not pretend to
                    // be one.
                    pin?.let { point ->
                        val site = school
                        val distance = site?.let {
                            haversineMeters(
                                point.latitude,
                                point.longitude,
                                it.latitude,
                                it.longitude,
                            )
                        }
                        IncidentLocation(
                            latitude = point.latitude,
                            longitude = point.longitude,
                            accuracyMeters = pinAccuracy,
                            distanceMeters = distance,
                            verified = if (site == null || distance == null) {
                                false
                            } else {
                                distance <= site.radiusMeters
                            },
                        )
                    },
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
private fun SectionHeader(title: String, trailing: String?) {
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
        if (trailing != null) {
            Text(
                text = trailing,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
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

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, press) = rememberPressScale(0.95f)
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = Modifier
            .height(36.dp)
            .then(press)
            .clip(shape)
            .background(if (selected) Primary else SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (selected) Primary else OutlineVariant.copy(alpha = 0.50f),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = label,
            color = if (selected) Color.White else Primary,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            maxLines = 1,
        )
    }
}

// ZONE 4 — Location
@Composable
private fun IncidentLocationCard(
    school: SchoolSite?,
    pin: LatLng?,
    verdict: IncidentPinVerdict,
    locating: Boolean,
    permissionGranted: Boolean,
    button: LocationButtonUi,
    onPinChange: (LatLng) -> Unit,
    onUseCurrentLocation: () -> Unit,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MapShape)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), MapShape)
                .background(MapPaper),
        ) {
            if (pin == null) {
                NoPinMapPlaceholder(
                    locating = locating,
                    permissionGranted = permissionGranted,
                )
            } else {
                CampusMap(
                    teacherFix = null,
                    modifier = Modifier.fillMaxSize(),
                    // La geocerca se pinta roja con el pin fuera, igual que el
                    // veredicto del chip: una sola respuesta visual.
                    outsideRadius = verdict.distanceMeters != null && !verdict.inside,
                    campus = school?.let { LatLng(it.latitude, it.longitude) },
                    radiusMeters = school?.radiusMeters?.toFloat() ?: CampusGeo.RadiusMeters,
                    schoolName = school?.name,
                    pin = pin,
                    onPinChange = onPinChange,
                    // El formulario tiene scroll: un mapa que se quede con los
                    // arrastres verticales encierra al docente en la tarjeta. El
                    // toque sí mueve el pin.
                    scrollGesturesEnabled = false,
                )
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
                    // El nombre real del plantel cacheado, nunca "Patio Central".
                    text = school?.name ?: "Plantel",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

        if (pin != null) {
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
                    text = "Toca el mapa para mover el marcador a donde ocurrió",
                    color = Outline,
                    fontFamily = DmSans,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CoordinateBox(
                label = "Latitud",
                value = pin?.let { formatCoordinate(it.latitude) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            CoordinateBox(
                label = "Longitud",
                value = pin?.let { formatCoordinate(it.longitude) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        val (useInteraction, usePress) = rememberPressScale(0.97f)
        val useLocationModifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainerLowest)
            .border(
                1.dp,
                if (button.enabled) Primary.copy(alpha = 0.30f)
                else OutlineVariant.copy(alpha = 0.50f),
                RoundedCornerShape(percent = 50),
            )
            .let { base ->
                if (button.enabled) {
                    base.then(usePress).clickable(
                        interactionSource = useInteraction,
                        indication = null,
                        onClick = onUseCurrentLocation,
                    )
                } else {
                    base
                }
            }
        Row(
            modifier = useLocationModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NearMe,
                contentDescription = null,
                tint = if (button.enabled) Primary else Outline,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = button.label,
                color = if (button.enabled) Primary else Outline,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.02.em,
            )
        }

        button.message?.let { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF8E1))
                    .border(1.dp, Color(0xFFF2C94C), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = Color(0xFF9E640B),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message,
                    color = Color(0xFF9E640B),
                    fontFamily = DmSans,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        PinVerdictChip(verdict = verdict)
    }
}

/**
 * El chip cierra la tarjeta con el veredicto **real** del pin: dentro, fuera o sin
 * geocerca. El mockup lo pinta siempre verde con "Verificada"; guardar un incidente
 * con un pin a 3 km como si estuviera dentro sería mentirle al tutor.
 */
@Composable
private fun PinVerdictChip(verdict: IncidentPinVerdict) {
    val accent = when {
        verdict.inside -> GreenChipText
        verdict.distanceMeters != null -> Color(0xFF9E640B)
        else -> Outline
    }
    val background = when {
        verdict.inside -> GreenChipBg
        verdict.distanceMeters != null -> Color(0xFFFFF8E1)
        else -> SurfaceContainerLow
    }
    val border = when {
        verdict.inside -> GreenChipBorder
        verdict.distanceMeters != null -> Color(0xFFF2C94C)
        else -> OutlineVariant.copy(alpha = 0.50f)
    }
    val icon = when {
        verdict.inside -> Icons.Filled.Verified
        verdict.distanceMeters != null -> Icons.Outlined.Warning
        else -> Icons.Outlined.MyLocation
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(percent = 50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (verdict.inside) GreenChipIcon else accent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = incidentPinChipLabel(verdict),
            color = accent,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.01).em,
        )
    }
}

/**
 * Sin coordenada real no se inventa un mapa: se explica por qué y se deja el botón
 * de ubicación (abajo en la tarjeta) como salida.
 */
@Composable
private fun NoPinMapPlaceholder(
    locating: Boolean,
    permissionGranted: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.MyLocation,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                locating -> "Buscando tu ubicación…"
                !permissionGranted ->
                    "Sin ubicación todavía. Activa el permiso para que el mapa se centre en ti."
                else ->
                    "Sin coordenada real todavía. Usa tu ubicación actual para marcar el lugar."
            },
            color = Outline,
            fontFamily = DmSans,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** `"19.292600"` — six decimals, punto decimal, sin importar el idioma del teléfono. */
private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

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

// ZONE 5 — Evidence
private sealed interface EvidenceCell {
    data object AddPhoto : EvidenceCell
    data class Thumbnail(val item: IncidentEvidenceUi) : EvidenceCell
}

@Composable
private fun EvidenceGrid(
    evidence: List<IncidentEvidenceUi>,
    limit: Int,
    onTakePhoto: () -> Unit,
    onRemove: (IncidentEvidenceUi) -> Unit,
) {
    val cells = buildList {
        // The limit is the one Remote Config delivers (`incident_media_max`), not
        // a number written here: the school changes it from the console.
        if (evidence.size < limit) add(EvidenceCell.AddPhoto)
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
    item: IncidentEvidenceUi,
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
        val bitmap = item.thumbnail
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Evidencia fotográfica",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // A file the system already reclaimed, or a decode that failed: the
            // tile still shows where the photo was instead of a blank square.
            CourtyardSnapshot(modifier = Modifier.fillMaxSize())
        }

        // Only while the incident is still on this screen: once it is queued the
        // row belongs to the worker and removing the tile would be a lie.
        if (item.phase == null) {
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
        }

        // 7.4: the state of *this* file, or nothing while the incident is unsaved.
        mediaPhaseLabel(item.phase)?.let { label ->
            Text(
                text = label,
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
private fun DateTimeCard(timestamp: String) {
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
            text = timestamp,
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

private fun newEvidencePhotoFile(context: Context): File {
    val directory = File(context.cacheDir, "incidentes").apply { mkdirs() }
    return File(directory, "incidente_${System.currentTimeMillis()}.jpg")
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

