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

internal val Blue=Color(0xFF41604B)
internal val Ink=Color(0xFF252B26)
internal val Subtle=Color(0xFF647064)
internal val Pale=Color(0xFFEAE7DE)
internal val Bg=Color(0xFFF7F4ED)
internal val Paper=Color(0xFFFFFDF8)
internal val Green=Color(0xFF3F775C)
internal val Gold=Color(0xFFB88A43)
internal val Line=Color(0xFFDEDCD3)
internal val SoftGreen=Color(0xFFE4ECE2)

@Composable internal fun GridTheme(content: @Composable ()->Unit) {
    MaterialTheme(
        colorScheme=lightColorScheme(
            primary=Blue,onPrimary=Color.White,primaryContainer=Pale,onPrimaryContainer=Ink,
            secondary=Green,onSecondary=Color.White,secondaryContainer=SoftGreen,onSecondaryContainer=Color(0xFF18564E),
            tertiary=Gold,onTertiary=Color.White,tertiaryContainer=Color(0xFFF4EEDF),onTertiaryContainer=Color(0xFF624921),
            background=Bg,onBackground=Ink,surface=Paper,onSurface=Ink,
            surfaceTint=Blue,surfaceDim=Pale,surfaceBright=Paper,
            surfaceContainerLowest=Paper,surfaceContainerLow=Bg,
            surfaceContainer=Bg,surfaceContainerHigh=Color(0xFFF0EEE5),surfaceContainerHighest=Pale,
            surfaceVariant=Pale,onSurfaceVariant=Subtle,outline=Color(0xFF99A18F),outlineVariant=Line,
            error=Color(0xFFAA4C48),onError=Color.White),
        typography=Typography(
            headlineLarge=TextStyle(fontSize=32.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.6).sp),
            headlineMedium=TextStyle(fontSize=27.sp,lineHeight=36.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.4).sp),
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
