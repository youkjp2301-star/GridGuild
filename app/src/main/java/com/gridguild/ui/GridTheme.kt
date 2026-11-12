package com.gridguild.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val GreenDark=Color(0xFF187344)
internal val Blue=GreenDark
internal val Ink=Color(0xFF171A18)
internal val Subtle=Color(0xFF66716B)
internal val Pale=Color(0xFFECEFEC)
internal val Bg=Color(0xFFF6F7F5)
internal val Paper=Color.White
internal val Green=Color(0xFF37C67A)
internal val Gold=Color(0xFFB07829)
internal val Line=Color(0xFFE6EAE6)
internal val SoftGreen=Color(0xFFE1F5E9)
internal val Charcoal=Color(0xFF202522)
internal val OnDarkMuted=Color(0xFFB8C6BD)

@Composable internal fun GridTheme(content: @Composable ()->Unit) {
    MaterialTheme(
        colorScheme=lightColorScheme(
            primary=GreenDark,onPrimary=Color.White,primaryContainer=SoftGreen,onPrimaryContainer=Ink,
            secondary=Green,onSecondary=Color.White,secondaryContainer=SoftGreen,onSecondaryContainer=Color(0xFF18564E),
            tertiary=Gold,onTertiary=Color.White,tertiaryContainer=Color(0xFFF4EEDF),onTertiaryContainer=Color(0xFF624921),
            background=Bg,onBackground=Ink,surface=Paper,onSurface=Ink,
            surfaceTint=Blue,surfaceDim=Pale,surfaceBright=Paper,
            surfaceContainerLowest=Paper,surfaceContainerLow=Bg,
            surfaceContainer=Bg,surfaceContainerHigh=Pale,surfaceContainerHighest=Pale,
            surfaceVariant=Pale,onSurfaceVariant=Subtle,outline=Color(0xFF99A18F),outlineVariant=Line,
            error=Color(0xFFAA4C48),onError=Color.White),
        typography=Typography(
            headlineLarge=TextStyle(fontSize=34.sp,lineHeight=41.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp),
            headlineMedium=TextStyle(fontSize=26.sp,lineHeight=33.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.7).sp),
            titleLarge=TextStyle(fontSize=21.sp,lineHeight=29.sp,fontWeight=FontWeight.SemiBold),
            titleMedium=TextStyle(fontSize=18.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold),
            bodyLarge=TextStyle(fontSize=16.sp,lineHeight=25.sp),
            bodyMedium=TextStyle(fontSize=14.sp,lineHeight=22.sp),
            bodySmall=TextStyle(fontSize=13.sp,lineHeight=20.sp),
            labelLarge=TextStyle(fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold),
            labelMedium=TextStyle(fontSize=12.sp,lineHeight=18.sp,fontWeight=FontWeight.Medium)),
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),
            medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(30.dp)),
        content={CompositionLocalProvider(LocalContentColor provides Ink,content=content)})
}
