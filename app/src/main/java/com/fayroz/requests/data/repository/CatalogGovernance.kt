package com.fayroz.requests.data.repository

import com.fayroz.requests.data.model.ItemEntity

/**
 * قواعد حوكمة دليل الأصناف:
 * - Master category ثابت لكل صنف متكرر بين أكثر من قسم.
 * - Master item للأسماء القديمة التي تم استبدالها بصنف أدق.
 * - ترتيب شراء عملي داخل كل كاتجوري بدل الترتيب الأبجدي أو أول رقم في الاسم.
 *
 * لا تحتوي هذه القواعد على أسعار، ولا تحذف تاريخ المستخدم.
 */
object CatalogGovernance {

    data class ItemOverride(
        val category: String? = null,
        val unit: String? = null,
        val marketName: String? = null,
        val specification: String? = null,
    )

    val hiddenCategoryTargets: Map<String, String> = mapOf(
        "مواد بناء" to "مواد بناء ومباني",
        "مباني" to "مواد بناء ومباني",
        "محارة" to "محارة وبياض",
        "عزل" to "عزل مائي وحراري",
        "إنشائي" to "خرسانة وحديد تسليح",
    )

    /** الكاتجوري القديمة المختلطة تحتاج قرار حسب اسم الصنف. */
    fun targetForLegacyCategory(oldCategory: String, itemName: String): String? = when (oldCategory) {
        "مباني ومحارة" -> when {
            itemName.contains("محارة", true) ||
                itemName.contains("بياض", true) ||
                itemName.contains("بؤج", true) ||
                itemName.contains("طرطشة", true) ||
                itemName.contains("معجون", true) -> "محارة وبياض"
            else -> "مواد بناء ومباني"
        }
        else -> hiddenCategoryTargets[oldCategory]
    }

    /**
     * aliases قديمة ندمج تاريخها في Master item واحد.
     * المفتاح = الاسم الفني القديم، القيمة = الاسم الفني الـMaster.
     */
    val mergeAliases: Map<String, String> = buildMap {
        listOf("1.5","2.5","4","6","10","16","25","35","50","70","95","120","150").forEach { s ->
            put("سلك نحاس $s مم²", "H07V-R 450/750V 1×$s مم²")
        }
        listOf("1.5","2.5","4","6","10","16","25","35","50","70","95","120","150").forEach { s ->
            put("سلك أرضي $s مم²", "PE H07V-K 450/750V 1×$s مم² أخضر/أصفر")
        }

        put("Stud 50 مم", "CW Stud 50 مم")
        put("Stud 70 مم", "CW Stud 75 مم")
        put("Stud 100 مم", "CW Stud 100 مم")
        put("Track 50 مم", "UW Track 50 مم")
        put("Track 70 مم", "UW Track 75 مم")
        put("Track 100 مم", "UW Track 100 مم")

        put("صوف صخري 50 مم", "صوف صخري سمك 50 مم")
        put("صوف صخري 100 مم", "صوف صخري سمك 100 مم")

        put("لاصق سيراميك عادي", "لاصق سيراميك C1")
        put("لاصق سيراميك مرن", "لاصق سيراميك C2")
        put("لاصق بورسلين", "لاصق بورسلين مرن C2TE")
        put("جراوت فواصل أبيض", "جراوت أسمنتي فواصل")
        put("جراوت فواصل ملون", "جراوت أسمنتي فواصل")
    }

    /**
     * أصناف قديمة عامة لا يمكن تحويلها لصنف أدق بدون افتراض مواصفة.
     * نخفيها من الاختيار الجديد، وتظل موجودة لأي كشف/سعر تاريخي مرتبط بها.
     */
    val archiveNames: Set<String> = setOf(
        "لوح أسمنتي Cement Board",
        "مسّلوب صرف UPVC 32 مم",
        "مسّلوب صرف UPVC 40 مم",
        "مسّلوب صرف UPVC 50 مم",
        "مسّلوب صرف UPVC 75 مم",
        "مسّلوب صرف UPVC 110 مم",
        "مسّلوب صرف UPVC 160 مم",
    )

    /** تصحيح الـMaster category والوحدة للأصناف التي كانت متداخلة أو لها Unit خاطئة. */
    val exactOverrides: Map<String, ItemOverride> = buildMap {
        // مثبتات/لاصق
        put("غراء خشب", ItemOverride(category = "مواد لاصقة وكيماويات", unit = "عبوة"))
        listOf("25","40","60").forEach { s ->
            put("مسمار خشب $s مم", ItemOverride(category = "مثبتات وإكسسوارات", unit = "علبة"))
        }

        // ري: كل الشبكة تحت كاتجوري واحدة.
        listOf("ماسورة ري 20 مم","ماسورة ري 25 مم","ماسورة ري 32 مم").forEach {
            put(it, ItemOverride(category = "شبكات مياه وري", unit = "م"))
        }
        put("رشاش Pop-up", ItemOverride(category = "شبكات مياه وري", unit = "عدد"))
        put("نقاطة ري", ItemOverride(category = "شبكات مياه وري", unit = "عدد"))
        put("محبس ري", ItemOverride(category = "شبكات مياه وري", unit = "عدد"))
        put("Timer ري", ItemOverride(category = "شبكات مياه وري", unit = "عدد"))
        put("نقاطة 4 لتر/ساعة", ItemOverride(category = "شبكات مياه وري", unit = "عدد"))
        put("خرطوم تنقيط 16 مم", ItemOverride(category = "شبكات مياه وري", unit = "م"))

        // كهرباء
        // المقاطع الصغيرة تُشترى غالبًا كلفة، والمقاطع الكبيرة بالمتر/الطبل.
        listOf("1.5","2","2.5","3","4","6","10","16","25","35","50","70","95","120","150","185","240","300","400","500","630").forEach { s ->
            put(
                "H07V-R 450/750V 1×$s مم²",
                ItemOverride(category = "كهرباء - تأسيس", unit = if ((s.toDoubleOrNull() ?: 0.0) <= 10.0) "لفة" else "م"),
            )
        }
        listOf("1.5","2","2.5","3","4","6","10","16","25","35","50","70","95","120","150","185","240","300").forEach { s ->
            put(
                "H07V-K 450/750V 1×$s مم²",
                ItemOverride(category = "كهرباء - تأسيس", unit = if ((s.toDoubleOrNull() ?: 0.0) <= 10.0) "لفة" else "م"),
            )
            put(
                "PE H07V-K 450/750V 1×$s مم² أخضر/أصفر",
                ItemOverride(category = "كهرباء - تأسيس", unit = if ((s.toDoubleOrNull() ?: 0.0) <= 10.0) "لفة" else "م"),
            )
        }

        put("تيب كهرباء", ItemOverride(category = "كهرباء - تأسيس", unit = "لفة", marketName = "شريط عزل كهرباء"))
        put("Junction Box", ItemOverride(category = "كهرباء - تأسيس", unit = "عدد", marketName = "علبة بواط"))
        listOf("10","20","30","40").forEach { s ->
            put("Cable Tie $s سم", ItemOverride(category = "كهرباء - تأسيس", unit = "علبة", marketName = "أفيز كابلات $s سم"))
        }

        // نجارة وألوميتال
        put("Laminate HPL", ItemOverride(category = "نجارة وأبواب", unit = "م²"))
        put("قشرة طبيعية", ItemOverride(category = "نجارة وأبواب", unit = "م²"))
        put("سلك ناموس فيبر", ItemOverride(category = "ألوميتال وUPVC", unit = "م²"))
        put("سلك ناموس ألومنيوم", ItemOverride(category = "ألوميتال وUPVC", unit = "م²"))
        put("سيليكون ألوميتال", ItemOverride(category = "مواد لاصقة وكيماويات", unit = "عبوة"))
        put("سيليكون زجاج", ItemOverride(category = "مواد لاصقة وكيماويات", unit = "عبوة"))

        // شتر
        put("دليل شتر جانبي", ItemOverride(category = "شتر", unit = "م"))
        put("ريشة شتر", ItemOverride(category = "شتر", unit = "م"))
        put("علبة شتر", ItemOverride(category = "شتر", unit = "عدد"))

        // واجهات
        put("سيليكون واجهات", ItemOverride(category = "مواد لاصقة وكيماويات", unit = "عبوة"))
        put("إكسسوارات تثبيت واجهات", ItemOverride(category = "واجهات تشطيب", unit = "عدد"))
        put("Anchor واجهات", ItemOverride(category = "مثبتات وإكسسوارات", unit = "عدد"))
        put("براغي ستانلس واجهات", ItemOverride(category = "مثبتات وإكسسوارات", unit = "علبة"))
        put("بروفايل واجهات", ItemOverride(category = "واجهات تشطيب", unit = "م"))

        // أعمال خارجية
        put("زلط ديكوري أبيض", ItemOverride(category = "أعمال خارجية", unit = "م³"))
        put("زلط ديكوري رمادي", ItemOverride(category = "أعمال خارجية", unit = "م³"))
        put("تربة زراعية", ItemOverride(category = "أعمال خارجية", unit = "م³"))

        // تكييف
        put("Duct صاج مجلفن", ItemOverride(category = "تكييف وتهوية", unit = "م²", marketName = "دكت صاج مجلفن"))
        put("فريون R410A", ItemOverride(category = "تكييف وتهوية", unit = "اسطوانة"))
        put("فريون R32", ItemOverride(category = "تكييف وتهوية", unit = "اسطوانة"))

        // حديد خفيف
        put("دهان مقاوم صدأ", ItemOverride(category = "دهانات", unit = "جردل"))
        put("إلكترود لحام 2.5 مم", ItemOverride(category = "حديد خفيف وإكسسوارات", unit = "علبة"))
        put("إلكترود لحام 3.2 مم", ItemOverride(category = "حديد خفيف وإكسسوارات", unit = "علبة"))
        put("Chemical Anchor", ItemOverride(category = "مواد لاصقة وكيماويات", unit = "عبوة", marketName = "كيميكال أنكر"))
        put("وزرة مطبخ", ItemOverride(category = "مطابخ", unit = "م"))
        put("تنر", ItemOverride(category = "دهانات", unit = "لتر"))

        // أدوات وليست مستهلكات
        listOf("متر 5 م","متر 8 م","ميزان مياه 60 سم","ميزان مياه 100 سم").forEach {
            put(it, ItemOverride(category = "عدد وأدوات", unit = "عدد"))
        }

        // مواد عزل لها Master واحد
        put("صوف صخري سمك 50 مم", ItemOverride(category = "عزل مائي وحراري", unit = "م²"))
        put("صوف صخري سمك 100 مم", ItemOverride(category = "عزل مائي وحراري", unit = "م²"))

        // Smart sensor مستقل عن حساس الإضاءة.
        put("حساس حركة", ItemOverride(category = "إضاءة", unit = "عدد", marketName = "حساس حركة للإضاءة"))
        put(
            "PIR Motion Sensor Smart",
            ItemOverride(category = "تيار خفيف وسمارت", unit = "عدد", marketName = "حساس حركة سمارت"),
        )
    }

    /** أسماء ليست شركات/ماركات ولا يجب أن تظهر في مكتبة الشركات. */
    val nonBrandNames: Set<String> = setOf("Local", "Imported", "OEM", "Generic")

    fun familyRank(category: String, item: ItemEntity): Int =
        familyRank(category, item.name, item.marketName)

    fun familyRank(category: String, name: String, marketName: String = ""): Int {
        val text = "$marketName $name"
        return when (category) {
            "مواد بناء ومباني" -> when {
                text.contains("أسمنت") -> 10
                text.contains("رمل") -> 20
                text.contains("سن ") || text.contains("زلط") -> 30
                text.contains("طوب") -> 40
                text.contains("بلوك") -> 50
                text.contains("جبس") || text.contains("جير") || text.contains("مونة") -> 60
                else -> 90
            }
            "خرسانة وحديد تسليح" -> when {
                text.contains("حديد تسليح") -> 10
                text.contains("سلك رباط") -> 15
                text.contains("خرسانة جاهزة") -> 20
                text.contains("شبك") -> 30
                text.contains("بسكوت") -> 40
                text.contains("شدات") || text.contains("كونتر") || text.contains("بلايوود") -> 50
                else -> 90
            }
            "محارة وبياض" -> when {
                text.contains("شبك") -> 10
                text.contains("زاوية") || text.contains("بروفايل") -> 20
                text.contains("طرطشة") || text.contains("محارة") || text.contains("رابطة") -> 30
                text.contains("معجون") -> 40
                text.contains("مونة") -> 50
                else -> 90
            }
            "عزل مائي وحراري" -> when {
                text.contains("لفائف") || text.contains("ممبرين") -> 10
                text.contains("برايمر") || text.contains("بيتوم") -> 20
                text.contains("أسمنتي") -> 30
                text.contains("Waterstop", true) || text.contains("سيلانت", true) || text.contains("Backer", true) -> 40
                text.contains("XPS", true) || text.contains("EPS", true) -> 50
                text.contains("صوف صخري") -> 60
                else -> 90
            }
            "سباكة - تغذية" -> when {
                text.contains("ماسورة PPR", true) -> 10
                text.contains("كوع PPR", true) -> 20
                text.contains("تي PPR", true) && !text.contains("مسّلوب") -> 30
                text.contains("جلبة PPR", true) -> 40
                text.contains("سن داخلي") || text.contains("سن خارجي") -> 45
                text.contains("مسّلوب") -> 50
                text.contains("اتحاد") || text.contains("فلنشة") -> 55
                text.contains("محبس") -> 60
                text.contains("كلبسة") -> 70
                text.contains("فلتر") || text.contains("منظم") || text.contains("عداد") -> 80
                text.contains("مجمع") -> 82
                text.contains("خزان") || text.contains("موتور") || text.contains("Pressure", true) -> 85
                else -> 90
            }
            "سباكة - صرف" -> when {
                text.contains("ماسورة UPVC", true) -> 10
                text.contains("كوع") && text.contains("45") -> 20
                text.contains("كوع") && text.contains("90") -> 22
                text.contains("تي صرف") -> 30
                text.contains("واي") || text.contains("كروس") -> 35
                text.contains("مسّلوب") -> 40
                text.contains("جلبة") || text.contains("سوكت") -> 45
                text.contains("تسليك") -> 50
                text.contains("كلبسة") -> 60
                text.contains("سيفون") || text.contains("بالوعة") || text.contains("جريل") -> 70
                else -> 90
            }
            "صرف خارجي وشبكات" -> when {
                text.contains("ماسورة") -> 10
                text.contains("كوع") -> 20
                text.contains("تي ") || text.contains("واي") -> 30
                text.contains("وصلة") -> 40
                text.contains("غرفة") || text.contains("غطاء") || text.contains("جريلة") -> 50
                else -> 90
            }
            "شبكات مياه وري" -> when {
                text.contains("ماسورة") || text.contains("خرطوم تنقيط") -> 10
                text.contains("كوع") || text.contains("تي ") || text.contains("وصلة") -> 20
                text.contains("محبس") || text.contains("Valve", true) -> 30
                text.contains("عداد") || text.contains("Air Valve", true) -> 40
                text.contains("رشاش") || text.contains("نقاطة") -> 50
                text.contains("Controller", true) || text.contains("Timer", true) -> 60
                text.contains("فلتر") -> 70
                else -> 90
            }
            "أدوات صحية" -> when {
                text.contains("مرحاض") || text.contains("شاسيه") || text.contains("طرد") -> 10
                text.contains("حوض حمام") || text.contains("وحدة حوض") -> 20
                text.contains("خلاط") -> 30
                text.contains("شاور") || text.contains("دش") || text.contains("شطاف") -> 40
                text.contains("بانيو") || text.contains("كابينة") -> 50
                text.contains("حوض مطبخ") -> 60
                text.contains("سخان") -> 70
                text.contains("مرآ") -> 80
                else -> 90
            }
            "مكافحة حريق" -> when {
                text.contains("ماسورة") -> 10
                text.contains("صمام") || text.contains("Valve", true) -> 20
                text.contains("رشاش") || text.contains("Sprinkler", true) -> 30
                text.contains("Switch", true) -> 40
                text.contains("Hose", true) || text.contains("خرطوم") || text.contains("Landing", true) -> 50
                text.contains("خزانة") || text.contains("طفاية") -> 60
                else -> 90
            }
            "إنذار حريق" -> when {
                text.contains("لوحة") || text.contains("Panel", true) -> 10
                text.contains("كاشف") || text.contains("Detector", true) -> 20
                text.contains("Call Point", true) -> 30
                text.contains("Sounder", true) || text.contains("Bell", true) -> 40
                text.contains("Module", true) || text.contains("Isolator", true) -> 50
                text.contains("كابل") -> 60
                else -> 90
            }
            "كهرباء - تأسيس" -> when {
                text.contains("H05V-R", true) -> 10
                text.contains("H05V-K", true) -> 11
                text.contains("H07V-U", true) -> 12
                text.contains("H07V-R", true) && !text.contains("PE ", true) -> 13
                text.contains("H07V-K", true) && !text.contains("PE ", true) -> 14
                text.contains("PE H07V-K", true) || text.contains("سلك أرضي") -> 15
                text.contains("سلك نحاس") -> 16
                text.contains("كابل نحاس") -> 20
                text.contains("ماسورة PVC", true) || text.contains("خرطوم كهرب") -> 30
                text.contains("كوع ماسورة") || text.contains("جلبة ماسورة") || text.contains("كلبسة ماسورة") -> 40
                text.contains("علبة") || text.contains("Junction", true) -> 50
                text.contains("Tray", true) || text.contains("Ladder", true) || text.contains("Trunking", true) -> 60
                text.contains("Gland", true) || text.contains("ترامل") -> 70
                text.contains("تأريض") || text.contains("أرضي") || text.contains("قضيب") || text.contains("شريط نحاس") -> 80
                else -> 90
            }
            "كهرباء - لوحات وحماية" -> when {
                text.contains("MCB", true) -> 10
                text.contains("RCBO", true) -> 20
                text.contains("RCCB", true) -> 30
                text.contains("MCCB", true) -> 40
                text.contains("Isolator", true) || text.contains("قاطع رئيسي") || text.contains("Main Breaker", true) -> 50
                text.contains("SPD", true) -> 60
                text.contains("كونتاكتور") || text.contains("ريلاي") || text.contains("تايمر") -> 70
                text.contains("لوحة توزيع") -> 80
                else -> 90
            }
            "كهرباء - مفاتيح وبرايز" -> when {
                text.startsWith("مفتاح") || text.contains("ديفياتير") || text.contains("Dimmer", true) -> 10
                text.contains("بريزة") -> 20
                text.contains("Faceplate", true) || text.contains("فريم") || text.contains("ميكانيزم") -> 30
                else -> 90
            }
            "إضاءة" -> when {
                text.contains("سبوت") || text.contains("Downlight", true) -> 10
                text.contains("Track", true) -> 20
                text.contains("شريط LED", true) -> 30
                text.contains("بروفايل LED", true) -> 40
                text.contains("Driver", true) -> 50
                text.contains("نجفة") || text.contains("أبليك") || text.contains("إضاءة") -> 60
                text.contains("حساس") -> 70
                text.contains("Emergency", true) -> 80
                else -> 90
            }
            "تيار خفيف وسمارت" -> when {
                text.contains("كابل شبكة") -> 10
                text.contains("Faceplate", true) || text.contains("Jack", true) -> 20
                text.contains("Patch", true) -> 30
                text.contains("Switch", true) || text.contains("Access Point", true) || text.contains("راوتر") -> 40
                text.contains("Rack", true) -> 50
                text.contains("كاميرا") || text.contains("NVR", true) || text.contains("HDD", true) -> 60
                text.contains("انتركم") || text.contains("قفل") || text.contains("جرس") -> 70
                text.contains("Smart", true) || text.contains("Zigbee", true) || text.contains("Matter", true) -> 80
                text.contains("حساس") || text.contains("Sensor", true) -> 90
                else -> 95
            }
            "تكييف وتهوية" -> when {
                text.contains("ماسورة نحاس") -> 10
                text.contains("عزل ماسورة") -> 20
                text.contains("صرف تكييف") || text.contains("كابل تكييف") -> 30
                text.contains("فريون") -> 40
                text.contains("Duct", true) -> 50
                text.contains("جريل") || text.contains("Diffuser", true) -> 60
                text.contains("مروحة") || text.contains("شفاط") -> 70
                else -> 90
            }
            "سيراميك وبورسلين" -> when {
                text.contains("سيراميك أرضيات") -> 10
                text.contains("سيراميك حوائط") -> 20
                text.contains("بورسلين") -> 30
                text.contains("وزرة") -> 40
                text.contains("Spacer", true) || text.contains("Leveling", true) -> 50
                text.contains("بروفايل") -> 60
                else -> 90
            }
            "أرضيات خشبية وبدائل" -> when {
                text.contains("HDF", true) -> 10
                text.contains("SPC", true) -> 20
                text.contains("LVT", true) || text.contains("Vinyl", true) -> 30
                text.contains("وزرة") -> 40
                text.contains("Underlay", true) || text.contains("Barrier", true) -> 50
                text.contains("Profile", true) -> 60
                else -> 90
            }
            "رخام وجرانيت وحجر" -> when {
                text.contains("رخام") -> 10
                text.contains("جرانيت") -> 20
                text.contains("كوارتز") -> 30
                text.contains("حجر") -> 40
                else -> 90
            }
            "جبس بورد وأسقف" -> when {
                text.contains("لوح جبس") || text.contains("Cement Board", true) -> 10
                text.contains("Stud", true) || text.contains("Track", true) -> 20
                text.contains("Channel", true) || text.contains("Angle", true) -> 30
                text.contains("مسمار") || text.contains("شريط") || text.contains("معجون") -> 40
                text.contains("Access Panel", true) -> 50
                text.contains("Acoustic", true) || text.contains("T-Grid", true) -> 60
                else -> 90
            }
            "دهانات" -> when {
                text.contains("سيلر") || text.contains("برايمر") -> 10
                text.contains("معجون") -> 20
                text.contains("دهان") || text.contains("لاكيه") || text.contains("ورنيش") -> 30
                text.contains("تنر") -> 40
                text.contains("صنفرة") -> 50
                text.contains("شريط ماسكنج") -> 60
                text.contains("رول") || text.contains("فرشة") -> 70
                else -> 90
            }
            "نجارة وأبواب" -> when {
                text.contains("باب") || text.contains("حلق") || text.contains("برواز") -> 10
                text.contains("MDF", true) || text.contains("HDF", true) || text.contains("Plywood", true) ||
                    text.contains("كونتر") || text.contains("ميلامين") || text.contains("HPL", true) || text.contains("قشرة") -> 20
                text.contains("مفصلة") || text.contains("كالون") || text.contains("سلندر") || text.contains("يد باب") -> 30
                else -> 90
            }
            "ألوميتال وUPVC" -> when {
                text.contains("قطاع") -> 10
                text.contains("ناموس") -> 20
                text.contains("جوان") || text.contains("كاوتش") -> 30
                text.contains("مقبض") || text.contains("مفصلة") || text.contains("Roller", true) || text.contains("كالون") -> 40
                else -> 90
            }
            "زجاج ومرايات" -> when {
                text.startsWith("زجاج") -> 10
                text.startsWith("مرا") -> 20
                else -> 30
            }
            "مطابخ" -> when {
                text.contains("وحدة") || text.contains("ضلفة") -> 10
                text.contains("مفصلة") || text.contains("سحابة") -> 20
                text.contains("سلة") || text.contains("Corner", true) -> 30
                else -> 90
            }
            "دواليب ودريسينج" -> when {
                text.contains("وحدة") || text.contains("ضلفة") -> 10
                text.contains("مفصلة") || text.contains("سحابة") -> 20
                text.contains("ماسورة") || text.contains("Rack", true) || text.contains("Hanger", true) -> 30
                else -> 90
            }
            "شتر" -> when {
                text.startsWith("شتر") -> 10
                text.contains("موتور") -> 20
                text.contains("ريموت") || text.contains("مفتاح") || text.contains("ريسيفر") -> 30
                text.contains("دليل") || text.contains("ريشة") || text.contains("علبة") -> 40
                else -> 90
            }
            "حديد خفيف وإكسسوارات" -> when {
                text.contains("قطاع Box", true) || text.contains("زاوية") || text.contains("ماسورة حديد") -> 10
                text.contains("صاج") || text.contains("ستانلس") -> 20
                text.contains("إلكترود") -> 30
                text.contains("ديسك") -> 40
                else -> 90
            }
            "واجهات تشطيب" -> when {
                text.contains("HPL", true) || text.contains("كلادينج") || text.contains("GRC", true) || text.contains("GFRC", true) -> 10
                text.contains("لوفر") || text.contains("بروفايل") -> 20
                else -> 90
            }
            "أعمال خارجية" -> when {
                text.contains("إنترلوك") || text.contains("بلاط") -> 10
                text.contains("بردورة") -> 20
                text.contains("زلط") || text.contains("تربة") || text.contains("نجيلة") -> 30
                text.contains("إضاءة") || text.contains("Bollard", true) -> 40
                else -> 90
            }
            "مثبتات وإكسسوارات" -> when {
                text.contains("فيشر") -> 10
                text.contains("Anchor", true) -> 20
                text.contains("مسمار") || text.contains("براغي") -> 30
                text.contains("صامولة") || text.contains("وردة") -> 40
                text.contains("Rivet", true) -> 50
                text.contains("U-Bolt", true) || text.contains("Clamp", true) || text.contains("Bracket", true) -> 60
                text.contains("Rod", true) -> 70
                else -> 90
            }
            "مواد لاصقة وكيماويات" -> when {
                text.contains("لاصق") || text.contains("Adhesive", true) || text.contains("غراء") -> 10
                text.contains("جراوت") -> 20
                text.contains("Sealant", true) || text.contains("سيليكون") -> 30
                text.contains("Epoxy", true) || text.contains("إيبوكسي") -> 40
                text.contains("مادة") || text.contains("مونة") -> 50
                text.contains("Cleaner", true) || text.contains("منظف") || text.contains("Remover", true) -> 60
                else -> 90
            }
            "عدد وأدوات" -> when {
                text.contains("متر ") -> 10
                text.contains("ميزان") -> 20
                else -> 90
            }
            "مستهلكات موقع" -> when {
                text.contains("حماية") || text.contains("مشمع") || text.contains("نايلون") || text.contains("كرتون") -> 10
                text.contains("جوانتي") || text.contains("ماسك") || text.contains("نظارة") || text.contains("سدادة") -> 20
                text.contains("ديسك") || text.contains("ريشة") || text.contains("شفرة") -> 30
                text.contains("قلم") || text.contains("ماركر") -> 40
                else -> 90
            }
            else -> 90
        }
    }

    /** رقم عملي للترتيب داخل نفس العائلة: مقطع/أمبير/قطر ثم الأبعاد. */
    fun sizeRank(name: String): Double {
        val pair = Regex("""(\d+(?:\.\d+)?)\s*[×xX]\s*(\d+(?:\.\d+)?)""")
            .find(name)
        if (pair != null) {
            val a = pair.groupValues[1].toDoubleOrNull() ?: 0.0
            val b = pair.groupValues[2].toDoubleOrNull() ?: 0.0
            return a * 10000.0 + b
        }

        val amp = Regex("""(\d+(?:\.\d+)?)\s*A(?:\s|$)""", RegexOption.IGNORE_CASE)
            .find(name)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        if (amp != null) return amp

        val dn = Regex("""DN\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(name)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        if (dn != null) return dn

        val section = Regex("""(\d+(?:\.\d+)?)\s*مم²""")
            .findAll(name).lastOrNull()?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        if (section != null) return section

        val measurements = Regex("""\d+(?:\.\d+)?""").findAll(name)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()
        return measurements.firstOrNull() ?: Double.MAX_VALUE
    }

    fun comparator(category: String): Comparator<ItemEntity> =
        compareBy<ItemEntity> { familyRank(category, it) }
            .thenBy { sizeRank(it.name) }
            .thenBy { it.marketName.ifBlank { StarterCatalog.marketName(it.name) } }
            .thenBy { it.name }
}
