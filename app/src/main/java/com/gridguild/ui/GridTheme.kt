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

internal val Blue=Color(0xFF315F79)
internal val Ink=Color(0xFF19384B)
internal val Subtle=Color(0xFF586F7C)
internal val Pale=Color(0xFFE6EEF2)
internal val Bg=Color(0xFFF3F6F7)
internal val Green=Color(0xFF247B70)
internal val Gold=Color(0xFFAD8A56)
internal val Line=Color(0xFFDDE6EA)
internal val SoftGreen=Color(0xFFE5F1EB)

@Composable internal fun GridTheme(content: @Composable ()->Unit) {
    MaterialTheme(
        colorScheme=lightColorScheme(
            primary=Blue,onPrimary=Color.White,primaryContainer=Pale,onPrimaryContainer=Ink,
            secondary=Green,onSecondary=Color.White,secondaryContainer=SoftGreen,onSecondaryContainer=Color(0xFF18564E),
            tertiary=Gold,onTertiary=Color.White,tertiaryContainer=Color(0xFFF4EEDF),onTertiaryContainer=Color(0xFF624921),
            background=Bg,onBackground=Ink,surface=Color.White,onSurface=Ink,
            surfaceTint=Blue,surfaceDim=Color(0xFFE3E9EC),surfaceBright=Color.White,
            surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF6F8F9),
            surfaceContainer=Bg,surfaceContainerHigh=Color(0xFFEDF2F4),surfaceContainerHighest=Color(0xFFE7EDF0),
            surfaceVariant=Color(0xFFEBF0F2),onSurfaceVariant=Subtle,outline=Color(0xFF93A8B4),outlineVariant=Line,
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
