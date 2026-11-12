package com.upisoundbox.core.design.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.design.theme.Merriweather
import com.upisoundbox.core.design.theme.SoundboxTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AmountDisplaySize {
    Large,
    Hero,
    Counter
}

typealias AmountSize = AmountDisplaySize

fun formatRupeeAmount(amountPaise: Long): String {
    val rupees = amountPaise / 100.0
    val symbols = DecimalFormatSymbols(Locale("en", "IN"))
    val pattern = if (amountPaise % 100 == 0L) "##,##,##,##0" else "##,##,##,##0.00"
    val formatter = DecimalFormat(pattern, symbols)
    return "₹" + formatter.format(rupees)
}

@Composable
fun AmountDisplay(
    amountPaise: Long,
    modifier: Modifier = Modifier,
    size: AmountDisplaySize = AmountDisplaySize.Large
) {
    val colors = SoundboxTheme.colors
    val formatted = formatRupeeAmount(amountPaise)

    val textStyle = when (size) {
        AmountDisplaySize.Hero -> TextStyle(
            fontFamily = Merriweather,
            fontWeight = FontWeight.Bold,
            fontSize = 52.sp,
            lineHeight = 60.sp,
            fontFeatureSettings = "lnum, tnum"
        )
        AmountDisplaySize.Large -> TextStyle(
            fontFamily = Merriweather,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            fontFeatureSettings = "lnum, tnum"
        )
        AmountDisplaySize.Counter -> TextStyle(
            fontFamily = Merriweather,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontFeatureSettings = "lnum, tnum"
        )
    }

    Text(
        text = formatted,
        style = textStyle,
        color = colors.textPrimary,
        modifier = modifier
    )
}
