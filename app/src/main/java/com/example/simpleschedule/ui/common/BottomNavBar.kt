package com.example.simpleschedule.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor

@Composable
fun BottomNavBar(
    isDark: Boolean,
    isFloating: Boolean = false,
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val borderColor = if (isDark) BorderDark else BorderLight
    val accent = LocalAccentColor.current
    val activeColor = accent ?: if (isDark) Color.White else Color.Black
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f)
    val indicatorColor = accent?.copy(alpha = 0.14f)
        ?: if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    @Composable
    fun Item(index: Int, icon: ImageVector, label: String) {
        NavBarItem(
            icon = icon,
            label = label,
            isActive = currentTab == index,
            color = if (currentTab == index) activeColor else inactiveColor,
            onClick = { onTabSelected(index) }
        )
    }

    if (isFloating) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = if (isDark) Color(0xFF18181B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(0.8.dp, borderColor.copy(alpha = 0.7f)),
                shadowElevation = 12.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp)
            ) {
                Box(modifier = Modifier.padding(6.dp)) {
                    val itemWidth = 108.dp
                    val indicatorOffset by animateDpAsState(
                        targetValue = itemWidth * currentTab.coerceIn(0, 1),
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "nav_indicator"
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .size(itemWidth, 48.dp)
                            .background(indicatorColor, RoundedCornerShape(24.dp))
                    )
                    Row(
                        modifier = Modifier.width(itemWidth * 2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(itemWidth).height(48.dp), contentAlignment = Alignment.Center) {
                            Item(0, Icons.Rounded.DateRange, "课表")
                        }
                        Box(modifier = Modifier.width(itemWidth).height(48.dp), contentAlignment = Alignment.Center) {
                            Item(1, Icons.Rounded.Person, "我的")
                        }
                    }
                }
            }
        }
    } else {
        Surface(
            color = if (isDark) BgDark.copy(alpha = 0.92f) else BgLight.copy(alpha = 0.92f),
            border = BorderStroke(0.5.dp, borderColor),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Item(0, Icons.Rounded.DateRange, "课表")
                Item(1, Icons.Rounded.Person, "我的")
            }
        }
    }
}

@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val animatedColor by animateColorAsState(targetValue = color, label = "nav_item_color")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Icon(icon, contentDescription = label, tint = animatedColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = animatedColor,
            letterSpacing = 0.5.sp
        )
    }
}
