package ir.techima.kardan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun KCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = K.c
    val shape = RoundedCornerShape(20.dp)
    val m = modifier.fillMaxWidth()
        .background(c.card, shape)
        .border(BorderStroke(1.dp, c.line), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(padding)
    Column(m, verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = K.c.primary, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).background(tint.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = K.c
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = c.text) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = c.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        trailing()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium, color = K.c.text)
}

@Composable
fun KButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val c = K.c
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary, disabledContainerColor = c.cardHigh, disabledContentColor = c.muted),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, danger: Boolean = false) {
    val c = K.c
    val col = if (danger) c.danger else c.text
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = 46.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (danger) c.danger else c.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = col),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text)
    }
}

@Composable
fun KField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, keyboard: KeyboardType = KeyboardType.Text, minLines: Int = 1,
    visual: VisualTransformation = VisualTransformation.None, trailing: (@Composable () -> Unit)? = null, placeholder: String? = null
) {
    val c = K.c
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), label = { Text(label) },
        singleLine = singleLine, minLines = minLines, visualTransformation = visual, trailingIcon = trailing,
        placeholder = if (placeholder != null) { { Text(placeholder, color = c.muted) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary, unfocusedBorderColor = c.line, focusedLabelColor = c.primary,
            unfocusedLabelColor = c.muted, cursorColor = c.primary, focusedTextColor = c.text, unfocusedTextColor = c.text
        )
    )
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = K.c
    val shape = RoundedCornerShape(50)
    Text(
        text,
        modifier = Modifier.background(if (selected) c.primary else c.cardHigh, shape)
            .border(1.dp, if (selected) c.primary else c.line, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) c.onPrimary else c.text, style = MaterialTheme.typography.labelLarge
    )
}

@Composable
fun Tag(text: String, color: Color = K.c.accent) {
    Text(
        text, modifier = Modifier.background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        color = color, style = MaterialTheme.typography.labelMedium
    )
}

enum class OpStatus(val label: String) {
    DONE("انجام شد"), NEEDS_CONFIRM("نیازمند تأیید شما"), NEEDS_PERMISSION("نیازمند مجوز"),
    NEEDS_USER_ACTION("نیازمند اقدام شما در تنظیمات"), IN_PROGRESS("در حال انجام"), FAILED("ناموفق"),
    UNSUPPORTED("پشتیبانی نمی‌شود"), UNKNOWN("نتیجه نامشخص است")
}

@Composable
fun StatusTag(s: OpStatus) {
    val c = K.c
    val col = when (s) {
        OpStatus.DONE -> c.ok
        OpStatus.NEEDS_CONFIRM, OpStatus.NEEDS_PERMISSION, OpStatus.NEEDS_USER_ACTION -> c.warn
        OpStatus.IN_PROGRESS -> c.accent
        OpStatus.FAILED -> c.danger
        else -> c.muted
    }
    Tag(s.label, col)
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    val c = K.c
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBadge(icon, size = 64.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun Spacer8() = Spacer(Modifier.height(8.dp))
