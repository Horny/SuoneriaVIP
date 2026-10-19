package com.example.suoneriavip

import android.content.Context
import android.content.SharedPreferences

class VipContactsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vip_prefs", Context.MODE_PRIVATE)

    fun getVipNumbers(): Set<String> {
        return prefs.getStringSet("vip_numbers", emptySet()) ?: emptySet()
    }

    fun addVipNumber(number: String) {
        val current = getVipNumbers().toMutableSet()
        val cleanNumber = sanitizeNumber(number)
        current.add(cleanNumber)
        prefs.edit().putStringSet("vip_numbers", current).apply()
    }

    fun removeVipNumber(number: String) {
        val current = getVipNumbers().toMutableSet()
        val cleanNumber = sanitizeNumber(number)
        current.remove(cleanNumber)
        prefs.edit().putStringSet("vip_numbers", current).apply()
    }

    fun isVip(incomingNumber: String?): Boolean {
        if (incomingNumber.isNullOrEmpty()) return false
        val cleanIncoming = sanitizeNumber(incomingNumber)
        return getVipNumbers().any { vip -> cleanIncoming.contains(vip) || vip.contains(cleanIncoming) }
    }

    private fun sanitizeNumber(num: String): String {
        return num.replace(Regex("[^0-9+]"), "")
    }
}
