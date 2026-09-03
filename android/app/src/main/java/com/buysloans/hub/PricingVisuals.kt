package com.buysloans.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class PricingVisual { LAPTOP, DESKTOP, PHONE, CONSOLE }

@Composable
internal fun PricingCategoryVisual(type: PricingVisual, modifier: Modifier = Modifier) {
    val background = when (type) {
        PricingVisual.LAPTOP -> Color(0xFFE7EEF8)
        PricingVisual.DESKTOP -> Color(0xFFE9ECEC)
        PricingVisual.PHONE -> Color(0xFFE4F2EE)
        PricingVisual.CONSOLE -> Color(0xFFF0ECE8)
    }
    Box(modifier.background(background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        when (type) {
            PricingVisual.LAPTOP -> MorleyIcon(MorleyIcons.Laptop, "Laptop", Color(0xFF355B8C), Modifier.size(31.dp))
            PricingVisual.DESKTOP -> MorleyIcon(MorleyIcons.Computer, "Desktop", Color(0xFF37403E), Modifier.size(31.dp))
            PricingVisual.PHONE -> MorleyIcon(MorleyIcons.Phone, "Mobile phone", Color(0xFF287E68), Modifier.size(31.dp))
            PricingVisual.CONSOLE -> MorleyIcon(MorleyIcons.Console, "Gaming console", Color(0xFF34383A), Modifier.size(31.dp))
        }
    }
}

@Composable
internal fun PhoneBrandVisual(brand: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when (brand.lowercase()) {
            "apple" -> AppleMark()
            "samsung" -> SamsungMark()
            "google" -> GoogleMark()
            "oneplus" -> OnePlusMark()
            "xiaomi" -> XiaomiMark()
            else -> MoreBrandsMark()
        }
    }
}

@Composable
private fun AppleMark() {
    Canvas(Modifier.size(38.dp)) {
        val green = Color(0xFF198A68)
        drawCircle(green, radius = size.minDimension * .21f, center = Offset(size.width * .43f, size.height * .56f))
        drawCircle(green, radius = size.minDimension * .21f, center = Offset(size.width * .61f, size.height * .56f))
        drawOval(green, topLeft = Offset(size.width * .35f, size.height * .46f), size = Size(size.width * .34f, size.height * .36f))
        rotate(-35f, pivot = Offset(size.width * .62f, size.height * .24f)) {
            drawOval(green, topLeft = Offset(size.width * .55f, size.height * .12f), size = Size(size.width * .18f, size.height * .11f))
        }
        drawCircle(MorleySurface, radius = size.minDimension * .075f, center = Offset(size.width * .77f, size.height * .48f))
    }
}

@Composable
private fun SamsungMark() {
    Box(
        Modifier.size(width = 46.dp, height = 27.dp).background(Color(0xFF1428A0), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("SAMSUNG", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun GoogleMark() {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Text("G", color = Color(0xFF4285F4), fontSize = 30.sp, fontWeight = FontWeight.Black)
        Canvas(Modifier.size(40.dp)) {
            drawCircle(Color(0xFFEA4335), radius = 2.6.dp.toPx(), center = Offset(size.width * .66f, size.height * .25f))
            drawCircle(Color(0xFFFBBC05), radius = 2.6.dp.toPx(), center = Offset(size.width * .72f, size.height * .55f))
            drawCircle(Color(0xFF34A853), radius = 2.6.dp.toPx(), center = Offset(size.width * .55f, size.height * .76f))
        }
    }
}

@Composable
private fun OnePlusMark() {
    Box(
        Modifier.size(38.dp).background(Color(0xFFF5010C), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) { Text("1+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun XiaomiMark() {
    Box(
        Modifier.size(38.dp).background(Color(0xFFFF6900), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) { Text("mi", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun MoreBrandsMark() {
    Canvas(Modifier.size(38.dp)) {
        val color = Color(0xFF37403E)
        val y = size.height / 2f
        drawCircle(color, 2.8.dp.toPx(), Offset(size.width * .28f, y))
        drawCircle(color, 2.8.dp.toPx(), Offset(size.width * .50f, y))
        drawCircle(color, 2.8.dp.toPx(), Offset(size.width * .72f, y))
    }
}
