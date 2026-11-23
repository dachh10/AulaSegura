package com.example.aulasegura.ui.screens.access

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryContainer
import com.example.aulasegura.ui.theme.TertiaryFixed
import com.example.aulasegura.ui.theme.TertiaryFixedDim

/** Permission state for each individual card. */
private enum class PermState { PENDING, GRANTED, DENIED }

/**
 * 1.8 — Permisos de la Aplicación.
 * Ported 1:1 from Stitch screen `48ae43854c954d2c8764f1a608ab69a7`.
 *
 * Layout:
 *  - Sticky TopAppBar ("Antes de empezar" + back + "Saltar")
 *  - "ÚLTIMO PASO" badge (pulsing coral dot)
 *  - Headline + subtitle
 *  - 3 permission cards: Ubicación y Notificaciones read the real runtime state;
 *    Cámara needs no permission because the app uses the system camera app
 *  - Trust assurance box
 *  - Fixed bottom: "Continuar" coral pill + "Configurar más tarde" text + lock note
 */
@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val asked = remember { mutableSetOf<String>() }

    // The manifest declares CAMERA because the QR scanners use CameraX, and
    // Android refuses the system camera intent to an app that declares the
    // permission without holding it. This card is the honest place to ask for it;
    // the photo pickers also ask on first use if the tutor skipped this screen.
    var cameraState by remember { mutableStateOf(PermState.PENDING) }
    var locationState by remember { mutableStateOf(PermState.PENDING) }
    var notifState by remember { mutableStateOf(PermState.PENDING) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        asked += Manifest.permission.CAMERA
        cameraState = if (granted) PermState.GRANTED else PermState.DENIED
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        asked += Manifest.permission.ACCESS_FINE_LOCATION
        locationState = if (granted) PermState.GRANTED else PermState.DENIED
    }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        asked += Manifest.permission.POST_NOTIFICATIONS
        notifState = if (granted) PermState.GRANTED else PermState.DENIED
    }

    fun isGranted(permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) return true
        // A coarse fix is enough for the school-presence check; accept it as granted
        // instead of asking the teacher to upgrade an already sufficient grant.
        return permission == Manifest.permission.ACCESS_FINE_LOCATION &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun stateOf(permission: String): PermState {
        // Below Android 13 the runtime notification permission does not exist; the
        // OS-wide switch is the equivalent source of truth on those versions.
        if (permission == Manifest.permission.POST_NOTIFICATIONS &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            return if (notificationsGranted(context)) PermState.GRANTED else PermState.DENIED
        }
        if (isGranted(permission)) return PermState.GRANTED
        if (permission !in asked) return PermState.PENDING
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } == true
        return if (canAskAgain) PermState.PENDING else PermState.DENIED
    }

    fun refreshStates() {
        cameraState = stateOf(Manifest.permission.CAMERA)
        locationState = stateOf(Manifest.permission.ACCESS_FINE_LOCATION)
        notifState = stateOf(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun request(permission: String) {
        asked += permission
        when (permission) {
            Manifest.permission.CAMERA -> cameraLauncher.launch(permission)
            Manifest.permission.ACCESS_FINE_LOCATION -> locationLauncher.launch(permission)
            Manifest.permission.POST_NOTIFICATIONS -> notifLauncher.launch(permission)
        }
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    // A granted card routes to Settings; a denied one asks, falling back to
    // Settings when the launcher cannot be used (e.g. no foreground activity).
    fun onLocationClick() {
        if (stateOf(Manifest.permission.ACCESS_FINE_LOCATION) == PermState.GRANTED) {
            openSettings()
        } else {
            runCatching { request(Manifest.permission.ACCESS_FINE_LOCATION) }
                .onFailure { openSettings() }
        }
    }

    fun onCameraClick() {
        if (stateOf(Manifest.permission.CAMERA) == PermState.GRANTED) {
            openSettings()
        } else {
            runCatching { request(Manifest.permission.CAMERA) }
                .onFailure { openSettings() }
        }
    }

    fun onNotificationsClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            stateOf(Manifest.permission.POST_NOTIFICATIONS) != PermState.GRANTED
        ) {
            runCatching { request(Manifest.permission.POST_NOTIFICATIONS) }
                .onFailure { openSettings() }
        } else {
            openSettings()
        }
    }

    LaunchedEffect(Unit) { refreshStates() }

    // Coming back from Settings must refresh the cards instead of showing a stale badge.
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStates()
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        // Dot watermark background
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f,
            color = PrimaryContainer,
        )
        // Large decorative shield outline — right side, partially off-screen
        PermShieldWatermark()

        Column(modifier = Modifier.fillMaxSize()) {
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Spacer(Modifier.height(statusBarHeight))

            // ---- Sticky header ----
            PermTopBar(onBack = onBack, onSkip = onSkip)

            // ---- Scrollable content ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Step badge + headline
                Reveal(delayMillis = 60) {
                    PermHeader()
                }

                Spacer(Modifier.height(16.dp))

                // Permission cards — state comes from the OS, not from a local flag.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Reveal(delayMillis = 100) {
                        PermissionCard(
                            icon = Icons.Outlined.LocationOn,
                            iconTint = TertiaryContainer,
                            iconBg = TertiaryFixed.copy(alpha = 0.3f),
                            title = "Ubicación",
                            description = "Para confirmar que la asistencia se registra dentro de la escuela.",
                            state = locationState,
                            onGrant = ::onLocationClick,
                            onOpenSettings = ::openSettings,
                        )
                    }
                    Reveal(delayMillis = 150) {
                        PermissionCard(
                            icon = Icons.Outlined.CameraAlt,
                            iconTint = PrimaryContainer,
                            iconBg = PrimaryFixed,
                            title = "Cámara",
                            description = "Para escanear códigos QR y tomar fotos de incidentes y comprobantes.",
                            state = cameraState,
                            onGrant = ::onCameraClick,
                            onOpenSettings = ::openSettings,
                        )
                    }
                    Reveal(delayMillis = 200) {
                        PermissionCard(
                            icon = Icons.Outlined.Notifications,
                            iconTint = PrimaryContainer,
                            iconBg = PrimaryFixed,
                            title = "Notificaciones",
                            description = "Para avisarte de faltas, tareas y mensajes nuevos.",
                            state = notifState,
                            onGrant = ::onNotificationsClick,
                            onOpenSettings = ::openSettings,
                        )
                    }
                    Reveal(delayMillis = 200) {
                        PermissionNote()
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Trust assurance box
                Reveal(delayMillis = 250) {
                    TrustBox()
                }

                Spacer(Modifier.height(8.dp))
            }

            // ---- Fixed bottom bar ----
            PermBottomBar(
                onContinue = onContinue,
                onLater = onSkip,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top App Bar
// ---------------------------------------------------------------------------

@Composable
private fun PermTopBar(onBack: () -> Unit, onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }

        // Screen title
        Text(
            text = "Antes de empezar",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.2).sp,
            color = Primary,
        )

        // Skip button
        Text(
            text = "Saltar",
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onSkip)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Header: step badge + headline + subtitle
// ---------------------------------------------------------------------------

@Composable
private fun PermHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // "ÚLTIMO PASO" badge with pulsing coral dot
        val pulseTransition = rememberInfiniteTransition(label = "badge_pulse")
        val pulseAlpha by pulseTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_alpha",
        )

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SecondaryFixed)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer.copy(alpha = pulseAlpha)),
            )
            Text(
                text = "ÚLTIMO PASO",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = Color(0xFF410000), // on-secondary-fixed
            )
        }

        Text(
            text = "Protección activa en el colegio",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.4).sp,
            color = Primary,
        )

        Text(
            text = "Estos permisos hacen que AulaSegura funcione correctamente. Tú decides cuáles activar.",
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = OnSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Real permission state
// ---------------------------------------------------------------------------

private fun permState(granted: Boolean): PermState =
    if (granted) PermState.GRANTED else PermState.PENDING

private fun locationGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

private fun notificationsGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

/**
 * The mockup had a third "Cámara" card; the app now really asks for CAMERA, so
 * this note only clarifies that the permission is used for QR scanning and for
 * the photos the user chooses to take.
 */
@Composable
private fun PermissionNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.8f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "La cámara solo se usa para escanear códigos QR y para las fotos que " +
                "decidas tomar; si la bloqueas, la app te lleva a Ajustes.",
            fontFamily = DmSans,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = OnSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Permission Card
// ---------------------------------------------------------------------------

@Composable
private fun PermissionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String,
    state: PermState,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            PermState.GRANTED -> TertiaryFixedDim.copy(alpha = 0.5f)
            PermState.DENIED -> Color(0xFFBA1A1A).copy(alpha = 0.4f)
            PermState.PENDING -> OutlineVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "border_color",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x081B1D3A))
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon container 48dp
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }

        // Text block
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
                color = Primary,
            )
            Text(
                text = description,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = OnSurfaceVariant,
                maxLines = 2,
            )
        }

        // Trailing: badge or button
        when (state) {
            PermState.GRANTED -> GrantedBadge()
            PermState.DENIED -> DeniedBadge(onClick = onOpenSettings)
            PermState.PENDING -> PermitButton(onClick = onGrant)
        }
    }
}

@Composable
private fun GrantedBadge() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(TertiaryFixed.copy(alpha = 0.4f))
            .border(1.dp, TertiaryFixedDim.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = TertiaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Activado",
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = (-0.1).sp,
            color = TertiaryContainer,
        )
    }
}

@Composable
private fun DeniedBadge(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFFFFDAD6).copy(alpha = 0.6f))
            .border(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Abrir ajustes",
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFFBA1A1A),
        )
    }
}

@Composable
private fun PermitButton(onClick: () -> Unit) {
    Text(
        text = "Permitir",
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = PrimaryContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(PrimaryFixed)
            .border(1.dp, OutlineVariant, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ---------------------------------------------------------------------------
// Trust assurance box
// ---------------------------------------------------------------------------

@Composable
private fun TrustBox() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.8f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp),
        )
        val text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Primary)) {
                append("Protocolo de Privacidad Escolar: ")
            }
            append("Los datos se resguardan bajo la Ley de Protección Estudiantil y se cifran de extremo a extremo.")
        }
        Text(
            text = text,
            fontFamily = DmSans,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = OnSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Fixed bottom bar
// ---------------------------------------------------------------------------

@Composable
private fun PermBottomBar(
    onContinue: () -> Unit,
    onLater: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(BrandCream.copy(alpha = 0f), BrandCream),
                    startY = 0f,
                    endY = 60f,
                ),
            )
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Coral "Continuar" button
        PillButton(
            text = "Continuar",
            onClick = onContinue,
            containerColor = SecondaryContainer,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
        )

        // "Configurar más tarde" ghost link
        Text(
            text = "Configurar más tarde",
            fontFamily = DmSans,
            fontSize = 12.sp,
            color = OnSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onLater)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )

        // Lock reassurance line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Nunca compartimos tu ubicación fuera de la escuela.",
                fontFamily = DmSans,
                fontSize = 11.sp,
                letterSpacing = (-0.1).sp,
                color = OnSurfaceVariant,
            )
        }

        // Android home bar indicator
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(width = 128.dp, height = 4.dp)
                .clip(CircleShape)
                .background(OutlineVariant),
        )
    }
}

// ---------------------------------------------------------------------------
// Decorative background watermark: large shield outline
// ---------------------------------------------------------------------------

@Composable
private fun PermShieldWatermark() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .size(width = 280.dp, height = 320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = 112.dp),
        ) {
            val w = size.width
            val h = size.height
            // Shield path matching the SVG in the Stitch HTML
            val path = Path().apply {
                // "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" scaled to canvas
                val cx = w / 2f
                val top = h * 0.02f
                val mid = h * 0.42f
                val bot = h * 0.98f
                moveTo(cx, bot)              // bottom center
                cubicTo(cx, bot, w * 0.9f, h * 0.75f, w * 0.9f, mid)
                lineTo(w * 0.9f, h * 0.17f)
                lineTo(cx, top)
                lineTo(w * 0.1f, h * 0.17f)
                lineTo(w * 0.1f, mid)
                cubicTo(w * 0.1f, h * 0.75f, cx, bot, cx, bot)
                close()
            }
            drawPath(
                path,
                color = PrimaryContainer.copy(alpha = 0.06f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Checkmark inside shield
            val checkPath = Path().apply {
                moveTo(w * 0.3f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.63f)
                lineTo(w * 0.70f, h * 0.42f)
            }
            drawPath(
                checkPath,
                color = PrimaryContainer.copy(alpha = 0.06f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

