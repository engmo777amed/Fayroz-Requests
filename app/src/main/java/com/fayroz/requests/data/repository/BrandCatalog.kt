package com.fayroz.requests.data.repository

object BrandCatalog {
    val byCategory: Map<String, List<String>> = mapOf(
        "سباكة - تغذية" to listOf("الشريف", "أكوافلو", "BR", "Bänninger"),
        "سباكة - صرف" to listOf("الشريف", "أكوافلو", "ERA", "Hepworth"),
        "أدوات صحية" to listOf("Ideal Standard", "Lecico", "Roca", "Duravit"),
        "كهرباء - تأسيس" to listOf("السويدي", "كابلات مصر", "El Nasr", "Elsewedy Cables"),
        "كهرباء - لوحات وحماية" to listOf("Schneider", "ABB", "Legrand", "Siemens"),
        "كهرباء - مفاتيح وبرايز" to listOf("Schneider", "Legrand", "BTicino", "Gewiss"),
        "إضاءة" to listOf("Philips", "Osram", "V-TAC", "Opple"),
        "تيار خفيف وسمارت" to listOf("Hikvision", "Dahua", "TP-Link", "Sonoff"),
        "تكييف وتهوية" to listOf("Carrier", "Sharp", "LG", "Samsung"),
        "سيراميك وبورسلين" to listOf("Cleopatra", "Royal", "Gloria", "Al Jawhara"),
        "رخام وجرانيت وحجر" to listOf("Marmarica", "Marmonil", "Hashma", "Imported"),
        "جبس بورد وأسقف" to listOf("Knauf", "Gyproc", "Siniat", "USG"),
        "دهانات" to listOf("Jotun", "GLC", "Sipes", "Pachin"),
        "نجارة وأبواب" to listOf("Good Wood", "Kronospan", "Egger", "Art Wood"),
        "ألوميتال وUPVC" to listOf("Jumbo", "PS", "Rehau", "Kommerling"),
        "زجاج ومرايات" to listOf("Saint-Gobain", "Guardian", "Sphinx", "Imported"),
        "مطابخ" to listOf("Hettich", "Blum", "Hafele", "Grass"),
        "دواليب ودريسينج" to listOf("Hettich", "Blum", "Hafele", "Grass"),
        "شتر" to listOf("Somfy", "Nice", "Dooya", "Cherubini"),
        "حديد خفيف وإكسسوارات" to listOf("Local", "Turkish", "Chinese", "Italian"),
        "واجهات تشطيب" to listOf("Alubond", "Alucobond", "Trespa", "Fundermax"),
        "أعمال خارجية" to listOf("Local", "Imported", "Premium", "Economy"),
        "مثبتات وإكسسوارات" to listOf("Fischer", "Hilti", "Rawlplug", "Wurth"),
        "مواد لاصقة وكيماويات" to listOf("Sika", "Bostik", "Pattex", "Mapei"),
        "مستهلكات موقع" to listOf("Bosch", "Makita", "Stanley", "Total"),
        "أخرى" to listOf("Local", "Imported", "OEM", "Generic"),
    )
}
