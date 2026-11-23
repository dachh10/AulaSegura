package com.example.aulasegura.ui.screens.parent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.profile.rememberCameraPermissionGate
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.ErrorContainer
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryContainer
import com.example.aulasegura.ui.theme.TertiaryFixed
import java.io.ByteArrayOutputStream

/**
 * 3.4 — Justificar falta. Ported from Stitch screen `9a38ede8a0894eb9886bd7197031d841`.
 *
 * The whole screen is a projection of [ParentJustifyUiState]: the date, the child
 * and the group come from the sessions the school registered, and the send button
 * is disabled when there is no absence to justify. The only local state left is
 * the photo preview, which is a decoded byte array and therefore not something a
 * ViewModel should hold.
 */
@Composable
fun ParentJustifyScreen(
    ui: ParentJustifyUiState,
    onBack: () -> Unit,
    onSent: () -> Unit,
    onRetry: () -> Unit,
    onSelectReason: (JustificationReason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onAttach: (fileName: String, mimeType: String, bytes: ByteArray) -> Unit,
    onRemoveReceipt: () -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current

    // The camera returns a small preview bitmap, which is already the right size
    // for a receipt; the gallery hands over the original, and the store compresses
    // it before the upload.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, CAMERA_QUALITY, out)
                out.toByteArray()
            }
            bitmap.recycle()
            onAttach("comprobante_foto.jpg", "image/jpeg", bytes)
        } else {
            Toast.makeText(
                context,
                "La cámara no devolvió la foto. Prueba con la galería.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) {
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                onAttach(displayName(context, uri), mimeType, bytes)
            }
        }
    }

    val loaded = ui as? ParentJustifyUiState.Loaded
    LaunchedEffect(loaded?.sent) {
        if (loaded?.sent == true) onSent()
    }

    val requireCameraPermission = rememberCameraPermissionGate(
        onDenied = {
            Toast.makeText(
                context,
                "Permite el acceso a la cámara para tomar el comprobante.",
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        JustifyTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (ui) {
                ParentJustifyUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = SecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }

                ParentJustifyUiState.NoChildren -> JustifyMessage(
                    title = "Sin alumno vinculado",
                    message = "Tu cuenta todavía no tiene un alumno registrado.",
                )

                is ParentJustifyUiState.Failed -> JustifyMessage(
                    title = "No se pudo abrir el justificante",
                    message = ui.message,
                    actionLabel = "Reintentar",
                    onAction = onRetry,
                )

                is ParentJustifyUiState.Loaded -> LoadedContent(
                    ui = ui,
                    onSelectReason = onSelectReason,
                    onDetailsChange = onDetailsChange,
                    onTakePhoto = { requireCameraPermission { cameraLauncher.launch(null) } },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onRemoveReceipt = onRemoveReceipt,
                )
            }
        }

        JustifyFooter(
            enabled = loaded?.canSend == true,
            sending = loaded?.sending == true,
            onSend = onSend,
        )
    }
}

@Composable
private fun LoadedContent(
    ui: ParentJustifyUiState.Loaded,
    onSelectReason: (JustificationReason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRemoveReceipt: () -> Unit,
) {
    val photo = remember(ui.receipt) { ui.receipt?.decodePreview() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Reveal(0) {
            AbsenceCard(
                absence = ui.absence,
                childName = ui.childName,
                groupLabel = ui.groupLabel,
                emptyMessage = ui.emptyMessage,
            )
        }

        Reveal(60) {
            ReasonSection(
                selected = ui.reason,
                onSelect = onSelectReason,
                details = ui.details,
                onDetailsChange = onDetailsChange,
            )
        }

        Reveal(120) {
            ReceiptSection(
                receipt = ui.receipt,
                photo = photo,
                onTakePhoto = onTakePhoto,
                onPickGallery = onPickGallery,
                onRemove = onRemoveReceipt,
            )
        }

        if (ui.error != null) {
            Reveal(150) { ErrorNote(message = ui.error) }
        }

        Reveal(180) { OfflineNote() }
    }
}

@Composable
private fun JustifyTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (interaction, pressModifier) = rememberPressScale(0.9f)
            Box(
                modifier = pressModifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = interaction, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Justificar falta",
                modifier = Modifier.weight(1f),
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                letterSpacing = (-0.18).sp,
            )
            Spacer(Modifier.width(24.dp))
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = OutlineVariant.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun AbsenceCard(
    absence: JustifyAbsenceUi?,
    childName: String,
    groupLabel: String,
    emptyMessage: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PrimaryFixed.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = OutlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
            ),
    ) {
        Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = PrimaryContainer.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 16.dp)
                .size(112.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (absence != null) {
                AbsencePill()
                Text(
                    text = absence.dateLabel,
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.2).sp,
                )
            } else {
                Text(
                    text = "Sin falta por justificar",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.2).sp,
                )
            }

            Text(
                text = "$childName · $groupLabel",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            if (absence == null && emptyMessage != null) {
                Text(
                    text = emptyMessage,
                    color = Outline,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun AbsencePill() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(ErrorContainer)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Falta",
            color = Error,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun ReasonSection(
    selected: JustificationReason,
    onSelect: (JustificationReason) -> Unit,
    details: String,
    onDetailsChange: (String) -> Unit,
) {
    Column {
        SectionTitle("Motivo")
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JustificationReason.entries.forEach { reason ->
                ReasonChip(
                    label = reason.label,
                    selected = reason == selected,
                    onClick = { onSelect(reason) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        DetailsField(
            value = details,
            onValueChange = onDetailsChange,
        )
    }
}

@Composable
private fun ReasonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Row(
        modifier = pressModifier
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .shadow(2.dp, CircleShape)
                        .background(PrimaryContainer)
                } else {
                    Modifier
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.6f), CircleShape)
                },
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = OnPrimary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (selected) OnPrimary else PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.24.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailsField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (focused) PrimaryContainer else OutlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= DETAILS_LIMIT) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp),
            textStyle = TextStyle(
                color = OnSurface,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(PrimaryContainer),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Detalles (opcional)",
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${value.length}/$DETAILS_LIMIT",
            modifier = Modifier.fillMaxWidth(),
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ReceiptSection(
    receipt: JustifyReceiptUi?,
    photo: Bitmap?,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRemove: () -> Unit,
) {
    Column {
        SectionTitle("Comprobante")
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryFixed.copy(alpha = 0.2f))
                .dashedBorder(
                    color = PrimaryContainer.copy(alpha = 0.3f),
                    cornerRadius = 8.dp,
                    strokeWidth = 2.dp,
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = SecondaryContainer,
                    modifier = Modifier.size(26.dp),
                )
            }

            Text(
                text = "Toma una foto del comprobante",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val (cameraInteraction, cameraPress) = rememberPressScale(0.95f)
                Row(
                    modifier = cameraPress
                        .weight(1f)
                        .height(40.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(SecondaryContainer)
                        .clickable(
                            interactionSource = cameraInteraction,
                            indication = null,
                            onClick = onTakePhoto,
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tomar foto",
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }

                val (galleryInteraction, galleryPress) = rememberPressScale(0.95f)
                Row(
                    modifier = galleryPress
                        .weight(1f)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.7f), CircleShape)
                        .clickable(
                            interactionSource = galleryInteraction,
                            indication = null,
                            onClick = onPickGallery,
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Elegir de galería",
                        color = PrimaryContainer,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
        }

        if (receipt != null) {
            Spacer(Modifier.height(12.dp))
            ReceiptPreview(
                fileName = receipt.fileName,
                photo = photo,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun ReceiptPreview(
    fileName: String,
    photo: Bitmap?,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryFixed.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Vista previa del comprobante",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TertiaryFixed)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = TertiaryContainer,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Listo para enviar",
                    color = TertiaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.33.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        val (interaction, pressModifier) = rememberPressScale(0.9f)
        Box(
            modifier = pressModifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Eliminar comprobante",
                tint = Outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ErrorNote(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ErrorContainer.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = Error,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            color = Error,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun OfflineNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Si no tienes internet, se guardará en tu celular y se enviará después.",
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun JustifyFooter(
    enabled: Boolean,
    sending: Boolean,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = OutlineVariant.copy(alpha = 0.3f),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            val (interaction, pressModifier) = rememberPressScale(0.98f)
            Row(
                modifier = pressModifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SecondaryContainer.copy(alpha = if (enabled) 1f else 0.4f))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onSend,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (sending) "Enviando…" else "Enviar justificante",
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun JustifyMessage(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.EventBusy,
            contentDescription = null,
            tint = PrimaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = Outline,
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Box(
                modifier = pressModifier
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    text = actionLabel,
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = PrimaryContainer,
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    )
}

/** Decodes only what the preview shows; a PDF keeps the placeholder icon. */
private fun JustifyReceiptUi.decodePreview(): Bitmap? {
    if (!isImage) return null
    return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
}

/** The picker's own name when it has one, instead of a random document id. */
private fun displayName(context: Context, uri: Uri): String = uri.lastPathSegment
    ?.substringAfterLast('/')
    ?.substringAfterLast(':')
    ?.takeIf { it.isNotBlank() }
    ?: "comprobante.jpg"

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

private const val CAMERA_QUALITY = 80
private const val DETAILS_LIMIT = 200
