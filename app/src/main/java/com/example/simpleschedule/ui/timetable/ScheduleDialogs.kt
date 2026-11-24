package com.example.simpleschedule.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DeleteScheduleDialog(
    isDark: Boolean,
    scheduleName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("删除课表", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                Spacer(modifier = Modifier.height(16.dp))
                Text("确定要删除「$scheduleName」吗？此操作无法恢复。", fontSize = 14.sp, color = textColor.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateScheduleDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("新建课表", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("例如：大二上学期", color = textColor.copy(alpha = 0.6f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        cursorColor = textColor
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
