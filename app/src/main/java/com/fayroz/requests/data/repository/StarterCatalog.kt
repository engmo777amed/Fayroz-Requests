package com.fayroz.requests.data.repository

data class StarterItem(
    val category: String,
    val name: String,
    val unit: String,
    val specification: String = "",
)

object StarterCatalog {
    val hiddenCategories = setOf(
        "مواد بناء",
        "مباني ومحارة",
        "عزل",
        "إنشائي",
        "مباني",
        "محارة",
    )

    val categories = listOf(
        "مواد بناء ومباني",
        "خرسانة وحديد تسليح",
        "محارة وبياض",
        "عزل مائي وحراري",
        "سباكة - تغذية",
        "سباكة - صرف",
        "صرف خارجي وشبكات",
        "شبكات مياه وري",
        "أدوات صحية",
        "مكافحة حريق",
        "إنذار حريق",
        "كهرباء - تأسيس",
        "كهرباء - لوحات وحماية",
        "كهرباء - مفاتيح وبرايز",
        "إضاءة",
        "تيار خفيف وسمارت",
        "مولدات وUPS",
        "تكييف وتهوية",
        "سيراميك وبورسلين",
        "أرضيات خشبية وبدائل",
        "رخام وجرانيت وحجر",
        "جبس بورد وأسقف",
        "دهانات",
        "نجارة وأبواب",
        "ألوميتال وUPVC",
        "زجاج ومرايات",
        "مطابخ",
        "دواليب ودريسينج",
        "شتر",
        "حديد خفيف وإكسسوارات",
        "واجهات تشطيب",
        "أعمال خارجية",
        "مثبتات وإكسسوارات",
        "مواد لاصقة وكيماويات",
        "مستهلكات موقع",
        "أخرى",
    )

    fun categoryRank(name: String): Int {
        val rank = categories.indexOf(name)
        return if (rank >= 0) rank else categories.size + 100
    }

    /**
     * Keeps items in the same practical order used when buying from the market:
     * main product first, then fittings/accessories, then by size/rating.
     */
    fun itemFamilyRank(name: String): Int = when {
        name.startsWith("أسمنت") -> 10
        name.startsWith("حديد تسليح") -> 10
        name.startsWith("خرسانة جاهزة") -> 20
        name.startsWith("طوب") || name.startsWith("بلوك") -> 30
        name.startsWith("ماسورة") -> 10
        name.startsWith("سلك ") -> 10
        name.startsWith("كابل ") -> 12
        name.startsWith("MCB") -> 10
        name.startsWith("MCCB") -> 12
        name.startsWith("RCCB") -> 14
        name.startsWith("RCBO") -> 16
        name.startsWith("لوحة توزيع") -> 20
        name.startsWith("كوع") -> 20
        name.startsWith("تي ") || name.startsWith("تي PPR") -> 30
        name.startsWith("واي") -> 35
        name.startsWith("جلبة") || name.startsWith("سوكت") -> 40
        name.startsWith("مسّلوب") -> 45
        name.startsWith("محبس") -> 50
        name.startsWith("كلبسة") -> 60
        name.startsWith("علبة") -> 20
        name.startsWith("مفتاح") -> 20
        name.startsWith("بريزة") -> 30
        name.startsWith("فريم") -> 40
        name.startsWith("ميكانيزم") -> 45
        name.startsWith("سبوت") || name.startsWith("Downlight") -> 10
        name.startsWith("Track Light") || name.startsWith("مسار Track") -> 20
        name.startsWith("شريط LED") -> 30
        name.startsWith("بروفايل LED") -> 40
        name.startsWith("Driver") -> 50
        else -> 100
    }

    fun firstMarketNumber(name: String): Double =
        Regex("""\d+(?:\.\d+)?""").find(name)?.value?.toDoubleOrNull() ?: Double.MAX_VALUE

    private fun marketAlias(name: String): String = when {
        name.startsWith("ماسورة PPR") -> "ماسورة حراري"
        name.startsWith("كوع PPR") -> "كوع حراري"
        name.startsWith("تي PPR") -> "تي حراري"
        name.startsWith("جلبة PPR") -> "جلبة حراري"
        name.startsWith("جلبة سن داخلي PPR") -> "جلبة نحاس داخلي"
        name.startsWith("جلبة سن خارجي PPR") -> "جلبة نحاس خارجي"
        name.startsWith("محبس PPR") -> "محبس حراري"
        name.startsWith("كلبسة تثبيت PPR") -> "كليبسة حراري"
        name.startsWith("مسّلوب PPR") -> "مسّلوب حراري"
        name.startsWith("تي مسّلوب PPR") -> "تي مسّلوب حراري"
        name.startsWith("ماسورة UPVC") -> "ماسورة صرف"
        name.startsWith("كوع صرف 45") -> "كوع 45 صرف"
        name.startsWith("كوع صرف 90") -> "كوع 90 صرف"
        name.startsWith("تي صرف") -> "تي صرف"
        name.startsWith("واي صرف") -> "واي صرف"
        name.startsWith("جلبة صرف") -> "سوكت صرف"
        name.startsWith("طبة تسليك") -> "طبة تسليك"
        name.startsWith("سيفون أرضية") -> "بيبة"
        name == "جالي تراب" -> "جالي تراب / جالي"
        name.startsWith("شاسيه مرحاض دفن") -> "شاسيه دفن / صندوق طرد مدفون"
        name.startsWith("مرحاض معلق") -> "قاعدة معلقة / Wall Hung WC"
        name.startsWith("مرحاض أرضي") -> "قاعدة حمام"
        name.startsWith("خلاط شاور دفن") -> "خلاط دفن"
        name.startsWith("هاند شاور") -> "سماعة"
        name.startsWith("مسطرة شاور") -> "مسطرة دش"
        name.startsWith("سلك نحاس") -> "سلك نحاس مفرد / Building Wire 450/750V"
        name.startsWith("سلك أرضي") -> "سلك أرضي / إيرث"
        name.startsWith("ماسورة PVC كهرباء") -> "ماسورة كهربا"
        name.startsWith("خرطوم كهرباء") -> "خرطوم كهربا"
        name.startsWith("علبة ماجيك") -> "علبة ماجيك"
        name.startsWith("Junction Box") -> "علبة بواط"
        name.startsWith("MCB") -> "قاطع أوتوماتيك MCB / مفتاح أوتوماتيك"
        name.startsWith("MCCB") -> "قاطع كومباكت MCCB / مفتاح كومباكت"
        name.startsWith("RCCB") -> "قاطع تسريب أرضي RCCB / مفتاح تسريب"
        name.startsWith("RCBO") -> "قاطع RCBO تسريب + زيادة تيار / مفتاح أوتوماتيك تسريب"
        name.startsWith("SPD") -> "مانع صواعق"
        name.startsWith("كونتاكتور") -> "كونتاكتور"
        name.startsWith("بار نحاس") -> "بار نحاس"
        name.startsWith("بار أرضي") -> "بار أرضي"
        name.startsWith("بار نيوترال") -> "بار نيوترال"
        name.startsWith("بريزة Schuko") -> "بريزة شوكو 16A / Schuko"
        name.startsWith("Faceplate") -> "وش داتا"
        name.startsWith("Downlight") -> "سبوت داون لايت"
        name.startsWith("Track Light") -> "سبوت تراك"
        name.startsWith("Driver") -> "درايفر"
        name.startsWith("Access Point") -> "أكسس بوينت"
        name.startsWith("Network Switch") -> "سويتش نتورك"
        name.startsWith("PoE Switch") -> "سويتش POE"
        name.startsWith("Patch Panel") -> "باتش بانل"
        name.startsWith("Rack") -> "راك"
        name.startsWith("NVR") -> "جهاز تسجيل"
        name.startsWith("ماسورة نحاس تكييف") -> "نحاس تكييف"
        name.startsWith("Flexible Duct") -> "فلكسبل"
        name.startsWith("Linear Slot Diffuser") -> "سلوت"
        name.startsWith("Square Diffuser") -> "ديفيوزر"
        name.startsWith("Tile Spacer") -> "صليبة"
        name.startsWith("Leveling Clip") -> "كليب تسوية"
        name.startsWith("Leveling Wedge") -> "إسفين"
        name.startsWith("Stud") -> "قائم"
        name.startsWith("Track") -> "مجرى"
        name.startsWith("Main Channel") -> "شانيل"
        name.startsWith("Furring Channel") -> "أوميجا"
        name.startsWith("Wall Angle") -> "زاوية"
        name.startsWith("Shadow Gap") -> "شادو جاب"
        name.startsWith("Access Panel") -> "فتحة صيانة"
        name.startsWith("Door Closer") -> "مساعد باب"
        name.startsWith("Door Stop") -> "صدادة"
        name.startsWith("Roller") -> "بكرة"
        name.startsWith("Chemical Anchor") -> "كيميكال"
        name.startsWith("Threaded Rod") -> "قلاووظ"
        name.startsWith("PU Sealant") -> "سيكا فليكس / PU"
        name.startsWith("Acrylic Sealant") -> "أكريليك"
        name.startsWith("Contact Adhesive") -> "كولة"
        name.startsWith("Rivet") -> "برشام"
        name.startsWith("Cable Tie") -> "أفيز"
        name.startsWith("U-Bolt") -> "يو بولت"
        else -> ""
    }

    fun marketLabel(name: String): String = marketAlias(name)

    private fun item(category: String, name: String, unit: String, specification: String = ""): StarterItem {
        val alias = marketAlias(name)
        val finalSpec = listOfNotNull(
            specification.takeIf { it.isNotBlank() },
            alias.takeIf { it.isNotBlank() }?.let { "اسم الصنايعي: $it" },
        ).joinToString(" • ")
        return StarterItem(category, name, unit, finalSpec)
    }

    val items: List<StarterItem> = buildList {
        val pprSizes = listOf(20, 25, 32, 40, 50, 63, 75, 90, 110)
        pprSizes.forEach { s ->
            add(item("سباكة - تغذية", "ماسورة PPR $s مم", "م"))
            add(item("سباكة - تغذية", "كوع PPR $s مم", "عدد"))
            add(item("سباكة - تغذية", "تي PPR $s مم", "عدد"))
            add(item("سباكة - تغذية", "جلبة PPR $s مم", "عدد"))
            add(item("سباكة - تغذية", "جلبة سن داخلي PPR $s مم", "عدد"))
            add(item("سباكة - تغذية", "جلبة سن خارجي PPR $s مم", "عدد"))
            add(item("سباكة - تغذية", "محبس PPR $s مم", "عدد"))
            add(item("سباكة - تغذية", "كلبسة تثبيت PPR $s مم", "عدد"))
        }
        listOf("20×25","20×32","25×32","25×40","32×40","32×50","40×50","50×63","63×75","75×90","90×110").forEach {
            add(item("سباكة - تغذية", "مسّلوب PPR $it مم", "عدد"))
            add(item("سباكة - تغذية", "تي مسّلوب PPR $it مم", "عدد"))
        }
        listOf(
            "محبس زاوية 1/2 بوصة","محبس زاوية 3/4 بوصة","محبس بوابة 1 بوصة","محبس بوابة 1.5 بوصة",
            "محبس عدم رجوع 1 بوصة","محبس عدم رجوع 1.5 بوصة","فلتر مياه رئيسي","عداد مياه","منظم ضغط مياه",
            "مجمع توزيع 2 مخرج","مجمع توزيع 3 مخرج","مجمع توزيع 4 مخرج","مجمع توزيع 5 مخرج","مجمع توزيع 6 مخرج",
            "خرطوم مرن 40 سم","خرطوم مرن 60 سم","خرطوم مرن 80 سم","خزان مياه 500 لتر","خزان مياه 1000 لتر",
            "خزان مياه 2000 لتر","موتور مياه 1 حصان","موتور مياه 1.5 حصان","موتور مياه 2 حصان",
            "أوتوماتيك موتور","Pressure Tank 24 لتر","Pressure Tank 50 لتر","عوامة خزان","فلتر شوائب","فلتر 3 مراحل"
        ).forEach { add(item("سباكة - تغذية", it, "عدد")) }

        val drainSizes = listOf(50, 75, 110, 160, 200)
        drainSizes.forEach { s ->
            add(item("سباكة - صرف", "ماسورة UPVC $s مم", "م"))
            add(item("سباكة - صرف", "كوع صرف 45° $s مم", "عدد"))
            add(item("سباكة - صرف", "كوع صرف 90° $s مم", "عدد"))
            add(item("سباكة - صرف", "تي صرف $s مم", "عدد"))
            add(item("سباكة - صرف", "واي صرف $s مم", "عدد"))
            add(item("سباكة - صرف", "جلبة صرف $s مم", "عدد"))
            add(item("سباكة - صرف", "طبة تسليك $s مم", "عدد"))
            add(item("سباكة - صرف", "كلبسة تثبيت صرف $s مم", "عدد"))
        }
        listOf("50×75","50×110","75×110","110×160","160×200").forEach {
            add(item("سباكة - صرف", "مسّلوب صرف $it مم", "عدد"))
        }
        listOf(
            "سيفون أرضية 10×10","سيفون أرضية 15×15","سيفون أرضية 20×20","جالي تراب","بالوعة مطبخ",
            "بالوعة حمام","سيفون حوض حمام","سيفون حوض مطبخ","وصلة صرف غسالة","وصلة صرف غسالة أطباق",
            "صرف تكييف 20 مم","صرف تكييف 25 مم","جريل صرف ستانلس 10 سم","جريل صرف ستانلس 15 سم",
            "جريل صرف خطي 30 سم","جريل صرف خطي 60 سم","جريل صرف خطي 80 سم","غرفة تفتيش بلاستيك"
        ).forEach { add(item("سباكة - صرف", it, "عدد")) }

        listOf(
            "مرحاض أرضي","مرحاض معلق","شاسيه مرحاض دفن","زر طرد مفرد","زر طرد مزدوج","حوض حمام معلق",
            "حوض حمام فوق الرخامة","حوض حمام تحت الرخامة","وحدة حوض حمام","خلاط حوض","خلاط حوض مرتفع",
            "خلاط بانيو","خلاط شاور ظاهر","خلاط شاور دفن","خلاط ترموستات","دش علوي","هاند شاور",
            "مسطرة شاور","طقم شطاف","بانيو أكريليك","بانيو فري ستاند","Shower Tray","كابينة شاور ثابتة",
            "كابينة شاور منزلقة","حوض مطبخ حلة واحدة","حوض مطبخ حلتين","خلاط مطبخ","خلاط مطبخ Pull-out",
            "موزع صابون مطبخ","سخان كهرباء 30 لتر","سخان كهرباء 50 لتر","سخان كهرباء 80 لتر",
            "سخان غاز 6 لتر","سخان غاز 10 لتر","سخان غاز 12 لتر","مرآة حمام","مرآة حمام بإضاءة",
            "حامل مناديل","حامل فوط","شماعة حمام","رف حمام","فرشة مرحاض","سلة حمام"
        ).forEach { add(item("أدوات صحية", it, "عدد")) }

        val wires = listOf("1.5","2.5","4","6","10","16","25","35","50","70","95","120","150")
        wires.forEach { s ->
            add(item("كهرباء - تأسيس", "سلك نحاس $s مم²", "لفة"))
            add(item("كهرباء - تأسيس", "سلك أرضي $s مم²", "لفة"))
        }
        val cableSections = listOf("1.5","2.5","4","6","10","16","25","35")
        listOf(2,3,4,5).forEach { cores ->
            cableSections.forEach { s ->
                add(item("كهرباء - تأسيس", "كابل نحاس $cores×$s مم²", "م"))
            }
        }
        val conduitSizes = listOf(16,20,25,32,40,50)
        conduitSizes.forEach { s ->
            add(item("كهرباء - تأسيس", "ماسورة PVC كهرباء $s مم", "م"))
            add(item("كهرباء - تأسيس", "خرطوم كهرباء $s مم", "لفة"))
            add(item("كهرباء - تأسيس", "كوع ماسورة كهرباء $s مم", "عدد"))
            add(item("كهرباء - تأسيس", "جلبة ماسورة كهرباء $s مم", "عدد"))
            add(item("كهرباء - تأسيس", "كلبسة ماسورة كهرباء $s مم", "عدد"))
        }
        listOf(
            "علبة ماجيك مفرد","علبة ماجيك مزدوج","علبة ماجيك ثلاثي","علبة سحب 10×10","علبة سحب 15×15",
            "علبة سحب 20×20","علبة سقف","Junction Box","تيب كهرباء","سوستة سحب 20 م","سوستة سحب 30 م",
            "ترامل 1.5 مم²","ترامل 2.5 مم²","ترامل 4 مم²","ترامل 6 مم²","ترامل 10 مم²","شرنك حراري",
            "Cable Tie 10 سم","Cable Tie 20 سم","Cable Tie 30 سم","Cable Tie 40 سم"
        ).forEach { add(item("كهرباء - تأسيس", it, if (it.contains("سوستة")) "عدد" else "علبة")) }

        listOf(6,10,16,20,25,32,40,50,63).forEach { amp ->
            add(item("كهرباء - لوحات وحماية", "MCB 1P $amp A", "عدد"))
            add(item("كهرباء - لوحات وحماية", "MCB 2P $amp A", "عدد"))
        }
        listOf(40,63,80,100,125,160,200,250).forEach { amp ->
            add(item("كهرباء - لوحات وحماية", "MCCB $amp A", "عدد"))
        }
        listOf(25,40,63,80,100).forEach { amp ->
            add(item("كهرباء - لوحات وحماية", "RCCB 2P $amp A 30mA", "عدد"))
            add(item("كهرباء - لوحات وحماية", "RCBO $amp A 30mA", "عدد"))
        }
        listOf(8,12,18,24,36,48,54,72).forEach { modules ->
            add(item("كهرباء - لوحات وحماية", "لوحة توزيع $modules خط", "عدد"))
        }
        listOf(
            "Main Breaker","SPD Type 2","كونتاكتور 25A","كونتاكتور 40A","كونتاكتور 63A","ريلاي تحكم",
            "تايمر رقمي","بار نحاس","بار أرضي","بار نيوترال","عداد فولت","عداد أمبير","لمبة بيان لوحة","مفتاح Selector"
        ).forEach { add(item("كهرباء - لوحات وحماية", it, "عدد")) }

        listOf(
            "مفتاح مفرد","مفتاح مزدوج","مفتاح ثلاثي","ديفياتير","مفتاح وسط سلم","مفتاح جرس",
            "بريزة عادية 16A","بريزة Schuko","بريزة USB","بريزة USB-C","بريزة Data","بريزة TV","بريزة Telephone",
            "بريزة Shaver","Dimmer","ثرموستات","Faceplate مفرد","Faceplate مزدوج","Faceplate ثلاثي",
            "فريم 1 فتحة","فريم 2 فتحة","فريم 3 فتحة","فريم 4 فتحة","فريم 6 فتحة","ميكانيزم مفتاح","ميكانيزم بريزة"
        ).forEach { add(item("كهرباء - مفاتيح وبرايز", it, "عدد")) }

        val lighting = listOf(
            "سبوت LED 5W","سبوت LED 7W","سبوت LED 9W","سبوت LED 12W","Downlight 12W","Downlight 18W","Downlight 24W",
            "Track Light 10W","Track Light 20W","Track Light 30W","مسار Track متر","مسار Track 2 متر",
            "شريط LED 12V أبيض","شريط LED 12V دافئ","شريط LED 24V أبيض","شريط LED 24V دافئ",
            "بروفايل LED دفن","بروفايل LED سطحي","بروفايل LED ركن","Driver 12V 60W","Driver 12V 100W",
            "Driver 24V 100W","Driver 24V 200W","نجفة","أبليك حائط","إضاءة مرآة","إضاءة سلم",
            "إضاءة خارجية حائط","إضاءة حديقة","إضاءة أرضية","حساس حركة","حساس إضاءة","Emergency Light"
        )
        lighting.forEach { add(item("إضاءة", it, if (it.contains("شريط") || it.contains("بروفايل") || it.contains("مسار")) "م" else "عدد")) }

        listOf(
            "كابل شبكة Cat6 UTP","كابل شبكة Cat6 FTP","كابل شبكة Cat6A","Faceplate Data مفرد","Faceplate Data مزدوج",
            "Data Jack Cat6","Patch Cord 1 م","Patch Cord 2 م","Patch Cord 3 م","Patch Panel 24 Port","Patch Panel 48 Port",
            "Network Switch 8 Port","Network Switch 16 Port","Network Switch 24 Port","PoE Switch 8 Port","PoE Switch 16 Port",
            "Access Point","راوتر","Rack 6U","Rack 9U","Rack 12U","Rack 18U","كابل كاميرات RG59","كابل كاميرات Cat6",
            "كاميرا Dome","كاميرا Bullet","كاميرا IP Dome","كاميرا IP Bullet","NVR 4 Channel","NVR 8 Channel","NVR 16 Channel",
            "HDD Surveillance 1TB","HDD Surveillance 2TB","HDD Surveillance 4TB","انتركم صوتي","انتركم مرئي",
            "جرس باب","قفل ذكي","Smart Switch 1 Gang","Smart Switch 2 Gang","Smart Switch 3 Gang","Smart Relay",
            "Smart Dimmer","Smart Curtain Module","Smart Thermostat","Zigbee Hub","Matter Hub","حساس فتح باب","حساس حركة","حساس دخان","حساس تسريب مياه"
        ).forEach { add(item("تيار خفيف وسمارت", it, if (it.startsWith("كابل")) "م" else "عدد")) }

        listOf("1/4","3/8","1/2","5/8","3/4","7/8","1 1/8").forEach { s ->
            add(item("تكييف وتهوية", "ماسورة نحاس تكييف $s بوصة", "م"))
            add(item("تكييف وتهوية", "عزل ماسورة تكييف $s بوصة", "م"))
        }
        listOf(
            "خرطوم صرف تكييف 16 مم","خرطوم صرف تكييف 20 مم","كابل تكييف 3×2.5 مم²","كابل تكييف 3×4 مم²",
            "قاعدة وحدة خارجية صغيرة","قاعدة وحدة خارجية كبيرة","فريون R410A","فريون R32","Duct صاج مجلفن",
            "Flexible Duct 4 بوصة","Flexible Duct 6 بوصة","Flexible Duct 8 بوصة","جريل هواء 20×20","جريل هواء 30×30",
            "جريل هواء 40×40","Linear Slot Diffuser","Square Diffuser","مروحة شفط 20 سم","مروحة شفط 25 سم","شفاط حمام","شفاط مطبخ"
        ).forEach { add(item("تكييف وتهوية", it, if (it.contains("Duct") || it.contains("خرطوم") || it.contains("كابل")) "م" else "عدد")) }

        val tileSizes = listOf("30×60","60×60","60×120","80×80","80×160","100×100","120×120","120×240")
        tileSizes.forEach { s ->
            add(item("سيراميك وبورسلين", "سيراميك أرضيات $s سم", "م²"))
            add(item("سيراميك وبورسلين", "سيراميك حوائط $s سم", "م²"))
            add(item("سيراميك وبورسلين", "بورسلين $s سم", "م²"))
        }
        listOf(
            "موزايكو","وزرة سيراميك","وزرة بورسلين","لاصق سيراميك عادي","لاصق سيراميك مرن","لاصق بورسلين",
            "جراوت فواصل أبيض","جراوت فواصل ملون","Tile Spacer 1.5 مم","Tile Spacer 2 مم","Tile Spacer 3 مم",
            "Leveling Clip 1 مم","Leveling Clip 1.5 مم","Leveling Wedge","بروفايل ألومنيوم نهايات","بروفايل ستانلس نهايات"
        ).forEach { add(item("سيراميك وبورسلين", it, if (it.contains("وزرة") || it.contains("بروفايل")) "م" else if (it.contains("لاصق")) "شيكارة" else "علبة")) }

        listOf(
            "رخام أرضيات","رخام حوائط","رخام سلالم","رخام وزرات","رخام مطابخ","رخام أحواض","جرانيت أرضيات","جرانيت سلالم",
            "جرانيت مطابخ","كوارتز مطابخ","كوارتز أحواض","حجر طبيعي","حجر صناعي","لاصق رخام","إيبوكسي رخام",
            "مادة تلميع رخام","سيلر حماية رخام"
        ).forEach { add(item("رخام وجرانيت وحجر", it, if (it.contains("لاصق") || it.contains("إيبوكسي") || it.contains("مادة") || it.contains("سيلر")) "كجم" else "م²")) }

        listOf(
            "لوح جبس بورد عادي","لوح جبس مقاوم للرطوبة","لوح جبس مقاوم للحريق","لوح أسمنتي Cement Board",
            "Stud 50 مم","Stud 70 مم","Stud 100 مم","Track 50 مم","Track 70 مم","Track 100 مم",
            "Main Channel","Furring Channel","Wall Angle","Shadow Gap","مسمار جبس 25 مم","مسمار جبس 35 مم","مسمار جبس 50 مم",
            "شريط فواصل ورق","شريط فواصل فيبر","معجون فواصل","Access Panel 30×30","Access Panel 40×40","Access Panel 60×60",
            "صوف صخري 50 مم","صوف صخري 100 مم","ألواح Acoustic Ceiling","T-Grid Main Tee","T-Grid Cross Tee"
        ).forEach { add(item("جبس بورد وأسقف", it, if (it.startsWith("لوح") || it.contains("Panel")) "عدد" else if (it.contains("صوف") || it.contains("Ceiling")) "م²" else "م")) }

        listOf(
            "سيلر مائي","سيلر زيتي","معجون داخلي","معجون خارجي","برايمر مائي","برايمر زيتي","دهان بلاستيك مط","دهان بلاستيك نصف لامع",
            "دهان بلاستيك لامع","دهان أكريليك داخلي","دهان أكريليك خارجي","دهان زيتي","لاكيه","إيبوكسي أرضيات",
            "دهان مقاوم للرطوبة","دهان مقاوم للحرارة","دهان أخشاب","ورنيش مط","ورنيش لامع","تنر","صنفرة 80","صنفرة 120","صنفرة 180",
            "صنفرة 220","شريط ماسكنج 24 مم","شريط ماسكنج 48 مم","رول دهان 10 سم","رول دهان 20 سم","رول دهان 25 سم",
            "فرشة دهان 1 بوصة","فرشة دهان 2 بوصة","فرشة دهان 3 بوصة","فرشة دهان 4 بوصة"
        ).forEach { add(item("دهانات", it, if (it.contains("صنفرة") || it.contains("رول") || it.contains("فرشة")) "عدد" else if (it.contains("شريط")) "لفة" else "جردل")) }

        listOf(
            "باب خشب مصمت","باب خشب مفرغ","باب HDF","باب WPC","حلق باب خشب","برواز باب","MDF 6 مم","MDF 12 مم","MDF 18 مم",
            "HDF 3 مم","Plywood 6 مم","Plywood 12 مم","Plywood 18 مم","كونتر 18 مم","ميلامين 18 مم","Laminate HPL",
            "قشرة طبيعية","مفصلة باب 3 بوصة","مفصلة باب 4 بوصة","مفصلة مخفية","كالون باب","سلندر كالون","يد باب",
            "Door Stop","Door Closer","مغناطيس باب","غراء خشب","مسمار خشب 25 مم","مسمار خشب 40 مم","مسمار خشب 60 مم"
        ).forEach { add(item("نجارة وأبواب", it, if (it.contains("MDF") || it.contains("HDF") || it.contains("Plywood") || it.contains("كونتر") || it.contains("ميلامين")) "لوح" else "عدد")) }

        listOf(
            "قطاع ألومنيوم شبابيك","قطاع ألومنيوم أبواب","قطاع ألومنيوم مطابخ","قطاع UPVC شبابيك","قطاع UPVC أبواب",
            "سلك ناموس فيبر","سلك ناموس ألومنيوم","جوان زجاج","كاوتش زجاج","سيليكون ألوميتال","مقبض شباك","مقبض باب ألوميتال",
            "مفصلة شباك","مفصلة باب ألوميتال","Roller شباك","Roller باب سحاب","كالون شباك","كالون باب ألوميتال","إكسسوارات Tilt & Turn"
        ).forEach { add(item("ألوميتال وUPVC", it, if (it.startsWith("قطاع") || it.contains("جوان") || it.contains("كاوتش")) "م" else "عدد")) }

        listOf(
            "زجاج شفاف 4 مم","زجاج شفاف 6 مم","زجاج شفاف 8 مم","زجاج شفاف 10 مم","زجاج سيكوريت 8 مم","زجاج سيكوريت 10 مم",
            "زجاج سيكوريت 12 مم","زجاج مصنفر 6 مم","زجاج دبل 18 مم","زجاج دبل 24 مم","مراية 4 مم","مراية 6 مم",
            "كلبسة زجاج","Spider زجاج","مقبض باب زجاج","مفصلة زجاج","سيليكون زجاج"
        ).forEach { add(item("زجاج ومرايات", it, if (it.startsWith("زجاج") || it.startsWith("مراية")) "م²" else "عدد")) }

        listOf(
            "وحدة مطبخ سفلية","وحدة مطبخ علوية","ضلفة مطبخ MDF","ضلفة مطبخ Acrylic","ضلفة مطبخ HPL",
            "مفصلة Soft Close 110°","مفصلة Soft Close 165°","سحابة درج 40 سم","سحابة درج 45 سم","سحابة درج 50 سم",
            "سلة أطباق","سلة زجاجات","سلة ركن","Magic Corner","وحدة Tall Unit","مقبض مطبخ","رجل مطبخ","وزرة مطبخ",
            "إضاءة تحت وحدات","منظم أدراج","سلة قمامة داخلية"
        ).forEach { add(item("مطابخ", it, if (it.startsWith("وحدة")) "م" else "عدد")) }

        listOf(
            "وحدة دولاب مفصلي","وحدة دولاب سحاب","ضلفة دولاب MDF","ضلفة دولاب زجاج","مفصلة Soft Close دولاب",
            "سحابة درج دولاب 40 سم","سحابة درج دولاب 45 سم","ماسورة شماعة","حامل ماسورة شماعة","سلة دولاب",
            "Pant Rack","Shoe Rack","Pull-down Hanger","مراية دولاب","إضاءة دولاب LED","مقبض دولاب"
        ).forEach { add(item("دواليب ودريسينج", it, if (it.startsWith("وحدة") || it.contains("ماسورة")) "م" else "عدد")) }

        listOf(
            "شتر ألومنيوم يدوي","شتر ألومنيوم موتور","شتر PVC يدوي","شتر PVC موتور","موتور شتر 40Nm","موتور شتر 60Nm",
            "موتور شتر 80Nm","ريموت شتر","مفتاح شتر","ريسيفر شتر","علبة شتر","دليل شتر جانبي","ريشة شتر"
        ).forEach { add(item("شتر", it, if (it.contains("شتر") && !it.contains("موتور") && !it.contains("ريموت") && !it.contains("مفتاح") && !it.contains("علبة")) "م²" else "عدد")) }

        listOf(
            "قطاع Box 20×20","قطاع Box 30×30","قطاع Box 40×40","قطاع Box 50×50","قطاع Box 60×40",
            "زاوية حديد 30×30","زاوية حديد 40×40","زاوية حديد 50×50","ماسورة حديد 1 بوصة","ماسورة حديد 1.5 بوصة",
            "ماسورة حديد 2 بوصة","صاج أسود 1 مم","صاج أسود 2 مم","صاج مجلفن 1 مم","ستانلس ستيل 304",
            "إلكترود لحام 2.5 مم","إلكترود لحام 3.2 مم","ديسك قطع 4 بوصة","ديسك قطع 7 بوصة","ديسك جلخ 4 بوصة","دهان مقاوم صدأ"
        ).forEach { add(item("حديد خفيف وإكسسوارات", it, if (it.startsWith("قطاع") || it.startsWith("زاوية") || it.startsWith("ماسورة")) "م" else if (it.startsWith("صاج") || it.startsWith("ستانلس")) "كجم" else "عدد")) }

        listOf(
            "HPL واجهات","كلادينج واجهات","GRC ديكور","GFRC ديكور","لوفر ألومنيوم","بروفايل واجهات","سيليكون واجهات",
            "إكسسوارات تثبيت واجهات","Anchor واجهات","براغي ستانلس واجهات"
        ).forEach { add(item("واجهات تشطيب", it, if (it.contains("واجهات") || it.contains("ديكور") || it.contains("لوفر")) "م²" else "عدد")) }

        listOf(
            "إنترلوك 6 سم","إنترلوك 8 سم","بردورة خرسانة","بلاط رصيف","زلط ديكوري أبيض","زلط ديكوري رمادي",
            "تربة زراعية","نجيلة طبيعية","نجيلة صناعية","ماسورة ري 20 مم","ماسورة ري 25 مم","ماسورة ري 32 مم",
            "رشاش Pop-up","نقاطة ري","محبس ري","Timer ري","وحدة إضاءة حديقة","Bollard Light","غطاء غرفة تفتيش"
        ).forEach { add(item("أعمال خارجية", it, if (it.contains("ماسورة") || it.contains("بردورة")) "م" else if (it.contains("إنترلوك") || it.contains("بلاط") || it.contains("نجيلة")) "م²" else "عدد")) }

        listOf(
            "فيشر بلاستيك 6 مم","فيشر بلاستيك 8 مم","فيشر بلاستيك 10 مم","فيشر معدني M6","فيشر معدني M8","فيشر معدني M10",
            "Chemical Anchor","مسمار صاج 13 مم","مسمار صاج 25 مم","مسمار خشب 25 مم","مسمار خشب 40 مم","مسمار خشب 60 مم",
            "مسمار خرسانة","صامولة M6","صامولة M8","صامولة M10","وردة M6","وردة M8","وردة M10","Rivet 3.2 مم","Rivet 4 مم",
            "U-Bolt","Clamp","Bracket","Threaded Rod M8","Threaded Rod M10","Threaded Rod M12"
        ).forEach { add(item("مثبتات وإكسسوارات", it, if (it.contains("Rod")) "م" else "علبة")) }

        listOf(
            "سيليكون شفاف","سيليكون أبيض","سيليكون أسود","Acrylic Sealant","PU Sealant","Epoxy شفاف","Epoxy رمادي",
            "Super Glue","Contact Adhesive","غراء خشب","لاصق PVC","منظف PVC","Rust Remover","Degreaser","Cleaner زجاج",
            "Cleaner ستانلس","شحم سيليكون","WD-40"
        ).forEach { add(item("مواد لاصقة وكيماويات", it, if (it.contains("Cleaner") || it.contains("Remover") || it.contains("Degreaser")) "لتر" else "عبوة")) }

        listOf(
            "نايلون حماية","كرتون حماية","مشمع حماية","شريط تحذير","جوانتي قماش","جوانتي لاتكس","ماسك غبار","ماسك فلتر",
            "نظارة حماية","سدادة أذن","ديسك صنفرة 4 بوصة","ريشة خرسانة 6 مم","ريشة خرسانة 8 مم","ريشة خرسانة 10 مم",
            "ريشة حديد 4 مم","ريشة حديد 6 مم","ريشة خشب 6 مم","شفرة كتر","شفرة منشار","قلم تعليم","ماركر دائم",
            "متر 5 م","متر 8 م","ميزان مياه 60 سم","ميزان مياه 100 سم"
        ).forEach { add(item("مستهلكات موقع", it, if (it.contains("نايلون") || it.contains("كرتون") || it.contains("مشمع") || it.contains("شريط")) "لفة" else "عدد")) }
    }
}
