package com.example.aulasegura.ui.screens.admin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.ui.components.EditableCampusMap
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

/** 4.7 — Configuración del plantel. Stitch layout kept; every value comes from Firestore. */

private val Hairline = Color(0xFFE6E1D8)
private val SchoolBadgeBg = Color(0xFFE8E9F7)
private val GpsPillBg = Color(0xFFEAF6F0)
private val GpsPillText = Color(0xFF1B7A50)
private val GpsDot = Color(0xFF2E9E6B)
private val SaveCoral = Color(0xFFFF6B57)
private val SaveShadow = Color(0x4DFF6B57)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun AdminCampusScreen(
    onBack: () -> Unit = {},
    onAyuda: () -> Unit = {},
) {
    val viewModel: AdminCampusViewModel = viewModel(
        factory = rememberAppContainer().viewModelFactory,
    )
    val uiState by viewModel.state.collectAsState()

    val onPinMove: (LatLng) -> Unit = remember(viewModel) { { latLng ->
        viewModel.setLocation(latLng.latitude, latLng.longitude)
    } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .imePadding(),
    ) {
        Reveal(0) { CampusTopBar(onBack = onBack, onAyuda = onAyuda) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .revealOnEnter(60)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SchoolIdentityCard(
                name = uiState.school?.name.orEmpty(),
                key = uiState.school?.key.orEmpty(),
                cycle = uiState.school?.cycle.orEmpty(),
            )
            LocationCard(
                latitudeText = uiState.latitudeText,
                longitudeText = uiState.longitudeText,
                storedLatitude = uiState.school?.latitude,
                storedLongitude = uiState.school?.longitude,
                radiusMeters = uiState.radiusMeters.toFloat(),
                coordinateError = uiState.coordinateError,
                onLatitudeChange = viewModel::setLatitude,
                onLongitudeChange = viewModel::setLongitude,
                onLocationChange = onPinMove,
            )
            RadiusCard(
                radius = uiState.radiusMeters,
                onRadiusChange = viewModel::setRadius,
                error = uiState.radiusError,
            )
            ScheduleCard(
                entryTime = uiState.entryTime,
                exitTime = uiState.exitTime,
                exigirUbicacion = uiState.requireLocation,
                onEntryTimeChange = viewModel::setEntryTime,
                onExitTimeChange = viewModel::setExitTime,
                onExigirUbicacionChange = viewModel::setRequireLocation,
                error = uiState.scheduleError,
            )
            CampusStatusSection(
                loading = uiState.loading,
                error = uiState.error,
                saveError = uiState.saveError,
                saved = uiState.saved,
                lastSaved = uiState.lastSaved,
                onRetry = viewModel::refresh,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest.copy(alpha = 0.95f))
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.30f)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SaveConfigurationButton(
                    onClick = viewModel::save,
                    enabled = uiState.canSave,
                    saving = uiState.saving,
                )
            }
        }
    }
}

@Composable
private fun CampusTopBar(onBack: () -> Unit, onAyuda: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Configuración del plantel",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onAyuda),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = "Ayuda",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SchoolIdentityCard(name: String, key: String, cycle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SchoolBadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nombre del plantel",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.24.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name.ifBlank { "Plantel sin nombre" },
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Clave:",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.33.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = key.ifBlank { "—" },
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.65.sp,
                )
            }
            if (cycle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ciclo $cycle",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.33.sp,
                )
            }
        }
    }
}

@Composable
private fun LocationCard(
    latitudeText: String,
    longitudeText: String,
    storedLatitude: Double?,
    storedLongitude: Double?,
    radiusMeters: Float,
    coordinateError: String?,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onLocationChange: (LatLng) -> Unit,
) {
    val draft = validateCoordinates(latitudeText, longitudeText)
    val draftLatitude = (draft as? CoordinateValidation.Valid)?.latitude
    val draftLongitude = (draft as? CoordinateValidation.Valid)?.longitude

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ubicación de la escuela",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            GpsActivePill()
        }

        EditableCampusMap(
            latitude = draftLatitude,
            longitude = draftLongitude,
            fallbackLatitude = storedLatitude,
            fallbackLongitude = storedLongitude,
            radiusMeters = radiusMeters,
            onLocationChange = onLocationChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp)),
        )

        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Arrastra el marcador o toca el mapa para mover el plantel. " +
                    "El radio se mide desde este punto.",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.24.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoordinateField(
                label = "Latitud",
                value = latitudeText,
                placeholder = formatCoordinate(19.2826),
                onValueChange = onLatitudeChange,
                isError = coordinateError != null,
                modifier = Modifier.weight(1f),
            )
            CoordinateField(
                label = "Longitud",
                value = longitudeText,
                placeholder = formatCoordinate(-99.6557),
                onValueChange = onLongitudeChange,
                isError = coordinateError != null,
                modifier = Modifier.weight(1f),
            )
        }

        coordinateError?.let {
            Text(
                text = it,
                color = Error,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun GpsActivePill() {
    val transition = rememberInfiniteTransition(label = "gpsPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gpsDotAlpha",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(GpsPillBg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(GpsDot.copy(alpha = pulse)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Centro guardado",
            color = GpsPillText,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun CoordinateField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = DmSans) },
        placeholder = { Text(placeholder, fontFamily = JetBrainsMono) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        ),
        modifier = modifier,
    )
}
@Composable
private fun RadiusCard(radius: Int, onRadiusChange: (Int) -> Unit, error: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Radio permitido",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = "$radius m",
                color = PrimaryContainer,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 22.sp,
            )
        }

        RadiusSlider(radius = radius, onRadiusChange = onRadiusChange)

        error?.let {
            Text(
                text = it,
                color = Error,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }

        Column(modifier = Modifier.padding(top = 4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.20f)),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = Color(0xFF004031),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TEACHER_REFRESH_HINT,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.24.sp,
                )
            }
        }
    }
}

@Composable
private fun RadiusSlider(radius: Int, onRadiusChange: (Int) -> Unit) {
    var trackWidth by remember { mutableStateOf(0) }
    val fraction = fractionFromRadius(radius)
    val knobRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }

    fun update(x: Float) {
        if (trackWidth > 0) {
            onRadiusChange(radiusFromFraction(x / trackWidth))
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .onSizeChanged { trackWidth = it.width }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> update(offset.x) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ -> update(change.position.x) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(SurfaceContainerHigh),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.001f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PrimaryContainer),
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (trackWidth * fraction).roundToInt() - knobRadiusPx.roundToInt(),
                            y = 0,
                        )
                    }
                    .size(20.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .border(2.dp, Color.White, CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$MIN_RADIUS_METERS m",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            )
            Text(
                text = "$MAX_RADIUS_METERS m",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    entryTime: String,
    exitTime: String,
    exigirUbicacion: Boolean,
    onEntryTimeChange: (String) -> Unit,
    onExitTimeChange: (String) -> Unit,
    onExigirUbicacionChange: (Boolean) -> Unit,
    error: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Horario de pase de lista",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeBox(
                label = "Desde",
                value = entryTime,
                onValueChange = onEntryTimeChange,
                icon = { Icon(Icons.Filled.Schedule, null, tint = PrimaryContainer, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
            TimeBox(
                label = "Hasta",
                value = exitTime,
                onValueChange = onExitTimeChange,
                icon = { Icon(Icons.Filled.AlarmOn, null, tint = PrimaryContainer, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
        }

        error?.let {
            Text(
                text = it,
                color = Error,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OutlineVariant.copy(alpha = 0.25f)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Exigir ubicación para pasar lista",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AulaSwitch(checked = exigirUbicacion, onCheckedChange = onExigirUbicacionChange)
        }
    }
}

@Composable
private fun TimeBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.80f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label.uppercase(),
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.30.sp,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(PrimaryContainer),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

@Composable
private fun CampusStatusSection(
    loading: Boolean,
    error: String?,
    saveError: String?,
    saved: Boolean,
    lastSaved: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.70f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            loading -> Text(
                text = "Cargando configuración del plantel…",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )

            error != null -> {
                Text(
                    text = error,
                    color = Error,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
                Text(
                    text = "Reintentar",
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onRetry() },
                )
            }

            else -> {
                if (saved) {
                    Text(
                        text = "Configuración guardada.",
                        color = GpsPillText,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                saveError?.let {
                    Text(
                        text = it,
                        color = Error,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = lastSaved,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun AulaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val offset by animateDpAsState(
        targetValue = if (checked) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "switchOffset",
    )

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 28.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) PrimaryContainer else OutlineVariant)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SaveConfigurationButton(
    onClick: () -> Unit,
    enabled: Boolean,
    saving: Boolean,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "saveCampusScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, RoundedCornerShape(percent = 50), ambientColor = SaveShadow, spotColor = SaveShadow)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) SaveCoral else OutlineVariant)
            .clickable(enabled = enabled, interactionSource = source, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Save,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (saving) "Guardando…" else "Guardar configuración",
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
    }
}
