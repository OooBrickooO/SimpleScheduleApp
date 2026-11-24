package com.example.simpleschedule.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.LocalAccentColor

@Composable
fun SettingValueItem(
    title: String,
    value: String,
    showBottomBorder: Boolean = true,
    textColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)
    } else {
        Modifier.fillMaxWidth().padding(16.dp)
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
        } else if (onClick != null) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
        }
    }
    if (showBottomBorder) HorizontalDivider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun SettingCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showBottomBorder: Boolean = true,
    textColor: Color,
    borderColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        val accent = LocalAccentColor.current ?: textColor
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = textColor.copy(alpha = 0.4f),
                checkmarkColor = if (isDark) BgDark else BgLight
            )
        )
    }
    if (showBottomBorder) HorizontalDivider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun SettingItemWithSubtext(
    title: String,
    subtext: String,
    showBottomBorder: Boolean = true,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtext, fontSize = 10.sp, color = textColor.copy(alpha = 0.5f), lineHeight = 14.sp)
        }
    }
    if (showBottomBorder) HorizontalDivider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun SettingCheckboxItemWithSubtext(
    title: String,
    subtext: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showBottomBorder: Boolean = true,
    textColor: Color,
    borderColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtext, fontSize = 10.sp, color = textColor.copy(alpha = 0.5f), lineHeight = 14.sp)
        }
        val accent = LocalAccentColor.current ?: textColor
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = textColor.copy(alpha = 0.4f),
                checkmarkColor = if (isDark) BgDark else BgLight
            )
        )
    }
    if (showBottomBorder) HorizontalDivider(color = borderColor, thickness = 0.5.dp)
}
