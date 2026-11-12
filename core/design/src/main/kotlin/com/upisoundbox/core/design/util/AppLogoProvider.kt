package com.upisoundbox.core.design.util

import androidx.annotation.DrawableRes
import com.upisoundbox.core.design.R

object AppLogoProvider {
    @DrawableRes
    fun getLogoForPackage(pkg: String): Int {
        val lower = pkg.lowercase()
        return when {
            lower.contains("nbu.paisa") -> R.drawable.ic_brand_gpay
            lower.contains("phonepe") -> R.drawable.ic_brand_phonepe
            lower.contains("paytm") -> R.drawable.ic_brand_paytm
            lower.contains("npci.upiapp") || lower.contains("bhim") -> R.drawable.ic_brand_bhim
            lower.contains("amazon") -> R.drawable.ic_brand_amazonpay
            lower.contains("dreamplug") || lower.contains("cred") -> R.drawable.ic_brand_cred
            else -> R.drawable.ic_brand_bhim
        }
    }
}

