package com.example.aulasegura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.AulaTextStyles
import com.example.aulasegura.ui.theme.InnerShape
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * Form field used across the access screens.
 *
 * Visual contract taken from the Stitch mockups:
 *  - 16 dp radius, 1 dp border, 20 dp leading icon at 14 dp inset
 *  - focused state turns the container white and the border indigo
 *  - optional fixed prefix (phone country code) separated by a hairline
 */
@Composable
fun AulaField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelTrailing: (@Composable () -> Unit)? = null,
    height: Dp = 52.dp,
    containerColor: Color = SurfaceContainerLowest,
    focusedContainerColor: Color = SurfaceContainerLowest,
    borderColor: Color = OutlineVariant.copy(alpha = 0.4f),
    shape: Shape = InnerShape,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    prefixText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = OnSurface,
                )
                labelTrailing?.invoke()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    color = if (focused) focusedContainerColor else containerColor,
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    color = if (focused) PrimaryContainer else borderColor,
                    shape = shape,
                ),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(20.dp),
                )
                if (prefixText != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = prefixText,
                        style = AulaTextStyles.dataMonoMd,
                        color = OnSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(OutlineVariant.copy(alpha = 0.5f)),
                    )
                }
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (prefixText != null) 80.dp else 44.dp,
                        end = if (trailing != null) 44.dp else 16.dp,
                    ),
                textStyle = textStyle.copy(color = OnSurface),
                singleLine = true,
                cursorBrush = SolidColor(PrimaryContainer),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle,
                                color = Outline.copy(alpha = 0.7f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            if (trailing != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    trailing()
                }
            }
        }
    }
}
