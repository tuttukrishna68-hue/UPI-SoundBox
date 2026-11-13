package com.upisoundbox.core.design.util

import androidx.annotation.DrawableRes
import com.upisoundbox.core.design.R

object AppLogoProvider {
    @DrawableRes
    fun getLogoForPackage(pkg: String): Int {
        val lower = pkg.trim().lowercase()
        return when {
            lower.contains("nbu.paisa") ||
                    lower.contains("google pay") ||
                    lower.contains("googlepay") ||
                    lower.contains("gpay") -> R.drawable.ic_brand_gpay
            lower.contains("phonepe") -> R.drawable.ic_brand_phonepe
            lower.contains("paytm") || lower.contains("net.one97") -> R.drawable.ic_brand_paytm
            lower == "in.org.npci.upiapp" || lower.contains("bhim") -> R.drawable.ic_brand_bhim
            lower.contains("amazon") -> R.drawable.ic_brand_amazonpay
            lower.contains("dreamplug") || lower.contains("cred") -> R.drawable.ic_brand_cred
            lower.contains("naviapp") || lower.contains("navi") -> R.drawable.ic_brand_navi
            lower.contains("super.payments") || lower.contains("supermoney") -> R.drawable.ic_brand_supermoney
            lower.contains("whatsapp") -> R.drawable.ic_brand_whatsapp
            lower.contains("mobikwik") -> R.drawable.ic_brand_mobikwik
            lower.contains("popclub") || lower.contains("pop") -> R.drawable.ic_brand_pop
            lower.contains("shareapp") || lower.contains("slice") -> R.drawable.ic_brand_slice
            lower.contains("fampay") || lower.contains("famapp") -> R.drawable.ic_brand_famapp
            lower.contains("nextbillion.groww") || lower.contains("groww") -> R.drawable.ic_brand_groww
            lower.contains("myairtelapp") || lower.contains("airtel") -> R.drawable.ic_brand_airtel
            else -> R.drawable.ic_app_logo
        }
    }
}
