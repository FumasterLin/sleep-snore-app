package com.mytools.sleepsnore.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val surfaceVariant: Color,
    val containerDark: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val snoreQuiet: Color,
    val snoreLight: Color,
    val snoreLoud: Color,
    val snoreEpic: Color
)

// SnoreLab 官方经典深色底色体系 (暗色模式)
val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF181B21),      // 官方主背景色
    surface = Color(0xFF1D2026),         // 官方面板底色
    card = Color(0xFF1D2026),            // 官方卡片底色
    surfaceVariant = Color(0xFF131419),  // 官方更深容器底色
    containerDark = Color(0xFF131419),   // 播放器底色
    border = Color(0xFF30353F),          // 边框灰
    textPrimary = Color(0xD8FFFFFF),     // 85% 白
    textSecondary = Color(0xB2FFFFFF),   // 70% 白
    textMuted = Color(0x66FFFFFF),       // 40% 白
    primaryAccent = Color(0xFF29ABE3),   // 官方高亮蓝
    secondaryAccent = Color(0xFF2599CC),
    snoreQuiet = Color(0xFF344E59),
    snoreLight = Color(0xFF488E83),
    snoreLoud = Color(0xFFC9A36E),
    snoreEpic = Color(0xFFD1693C)
)

// 清新优雅现代明亮体系 (亮色模式)
val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF6F8FA),      // 清爽微灰白底色
    surface = Color(0xFFFFFFFF),         // 纯白卡片与面板
    card = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF2F6),  // 柔和底色容器
    containerDark = Color(0xFFE2E8F0),   // 浅灰凹槽底色
    border = Color(0xFFE2E8F0),          // 细致浅灰边框
    textPrimary = Color(0xFF0F172A),     // 高对比度深蓝黑
    textSecondary = Color(0xFF475569),   // 典雅中灰
    textMuted = Color(0xFF94A3B8),       // 辅助灰
    primaryAccent = Color(0xFF0288D1),   // 亮色晴空蓝
    secondaryAccent = Color(0xFF0277BD),
    snoreQuiet = Color(0xFF546E7A),
    snoreLight = Color(0xFF00897B),
    snoreLoud = Color(0xFFF57C00),
    snoreEpic = Color(0xFFE53935)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

// 动态响应当前主题的 Composable 颜色属性
val StandardBackground: Color @Composable get() = AppTheme.colors.background
val DarkBackground: Color @Composable get() = AppTheme.colors.background
val LighterBackground: Color @Composable get() = AppTheme.colors.surface
val DarkerBackground: Color @Composable get() = AppTheme.colors.containerDark
val DialOuterCircle: Color @Composable get() = AppTheme.colors.border

val DarkSurface: Color @Composable get() = AppTheme.colors.surface
val DarkSurfaceVariant: Color @Composable get() = AppTheme.colors.surfaceVariant
val DarkCard: Color @Composable get() = AppTheme.colors.card
val DarkBorder: Color @Composable get() = AppTheme.colors.border

val BlueHighlight: Color @Composable get() = AppTheme.colors.primaryAccent
val BlueButton: Color @Composable get() = AppTheme.colors.secondaryAccent
val AccentPrimary: Color @Composable get() = AppTheme.colors.primaryAccent
val PrimaryAccent: Color @Composable get() = AppTheme.colors.primaryAccent
val AccentSecondary: Color @Composable get() = AppTheme.colors.secondaryAccent

val SnoreQuiet: Color @Composable get() = AppTheme.colors.snoreQuiet
val SnoreLight: Color @Composable get() = AppTheme.colors.snoreLight
val SnoreMild: Color @Composable get() = AppTheme.colors.snoreLight
val SnoreLoud: Color @Composable get() = AppTheme.colors.snoreLoud
val SnoreEpic: Color @Composable get() = AppTheme.colors.snoreEpic

// 睡眠夜视界面专属固定深空底色 (不受白天日间亮色主题影响，确保夜间全暗环境防刺眼)
val NightCenter = Color(0xFF153066)
val NightCorner = Color(0xFF091936)

val TextBright: Color @Composable get() = AppTheme.colors.textPrimary
val TextLessBright: Color @Composable get() = AppTheme.colors.textSecondary
val TextPrimary: Color @Composable get() = AppTheme.colors.textPrimary
val TextSecondary: Color @Composable get() = AppTheme.colors.textSecondary
val TextMuted: Color @Composable get() = AppTheme.colors.textMuted
