package com.fayroz.requests.data.repository

/**
 * إضافات عملية مبنية على أصناف متداولة في السوق المصري.
 * تظل Idempotent: لا تنشئ دوبليكيت لو الصنف موجود بالاسم نفسه.
 */
object MarketCatalogExpansion {
    val categories = listOf(
        "مواد بناء ومباني",
        "خرسانة وحديد تسليح",
        "محارة وبياض",
        "عزل مائي وحراري",
        "صرف خارجي وشبكات",
        "مكافحة حريق",
        "أرضيات خشبية وبدائل",
        "شبكات مياه وري",
        "إنذار حريق",
        "مولدات وUPS",
    )

    val items: List<StarterItem> = buildList {
        fun put(
            category: String,
            name: String,
            unit: String,
            specification: String = "",
            marketName: String = "",
        ) {
            add(
                StarterItem(
                    category = category,
                    name = name,
                    unit = unit,
                    specification = specification,
                    marketName = marketName,
                )
            )
        }

        listOf(
            "أسمنت بورتلاندي عادي CEM I 42.5N",
            "أسمنت بورتلاندي عادي CEM I 52.5N",
            "أسمنت مقاوم للكبريتات SRC",
            "أسمنت بوزولاني",
            "أسمنت تشطيبات 32.5N",
        ).forEach { put("مواد بناء ومباني", it, "شيكارة") }

        listOf("رمل مباني","رمل محارة","رمل حرش","سن 1","سن 2","سن متدرج","زلط").forEach {
            put("مواد بناء ومباني", it, "م³")
        }

        listOf(
            "طوب أحمر طفلي 25×12×6 سم",
            "طوب أحمر طفلي 20×10×6 سم",
            "طوب أسمنتي مصمت",
            "طوب رملي",
        ).forEach { put("مواد بناء ومباني", it, "ألف طوبة") }

        listOf(10,12,15,20).forEach { t ->
            put("مواد بناء ومباني", "بلوك أسمنتي مفرغ ${t}×20×40 سم", "عدد")
            put("مواد بناء ومباني", "بلوك AAC خفيف سمك $t سم", "عدد")
        }

        listOf("جبس أبيض","جير مطفأ","مونة مباني جاهزة","مونة إصلاح أسمنتية").forEach {
            put("مواد بناء ومباني", it, "شيكارة")
        }

        listOf(8,10,12,14,16,18,20,22,25,28,32,36,40).forEach { d ->
            put("خرسانة وحديد تسليح", "حديد تسليح قطر $d مم", "طن")
        }

        listOf(150,200,250,300,350,400,450,500,550,600).forEach { grade ->
            put("خرسانة وحديد تسليح", "خرسانة جاهزة مقاومة $grade كجم/سم²", "م³")
        }

        listOf(
            "سلك رباط صلب أسود","بسكوت خرسانة 2.5 سم","بسكوت خرسانة 5 سم","بسكوت عجلة حائط",
            "شبك حديد ملحوم 6 مم","شبك حديد ملحوم 8 مم","شبك ممدد معدني",
            "لوح كونتر شدات 18 مم","لوح بلايوود فيلم فيس 18 مم","خشب عروق شدات","خشب موسكي شدات",
            "مسمار نجارة 2 بوصة","مسمار نجارة 3 بوصة","مسمار نجارة 4 بوصة","زيت فك شدات",
        ).forEach { name ->
            val unit = when {
                name.startsWith("سلك") -> "كجم"
                name.startsWith("بسكوت") -> "عدد"
                name.startsWith("شبك") -> "م²"
                name.startsWith("لوح") -> "لوح"
                name.startsWith("خشب") -> "م³"
                name.startsWith("مسمار") -> "كجم"
                else -> "جركن"
            }
            put("خرسانة وحديد تسليح", name, unit)
        }

        listOf(
            "شبك فيبر محارة 10 سم","شبك فيبر محارة 20 سم","شبك معدني ممدد محارة",
            "زاوية محارة مجلفنة 3 م","بروفايل بؤج محارة","مادة رابطة للخرسانة والمحارة",
            "طرطشة أسمنتية جاهزة","محارة أسمنتية جاهزة","معجون أسمنتي داخلي","معجون أسمنتي خارجي",
            "مونة ترميم وتعويض","مونة ملء شروخ غير منكمشة",
        ).forEach { name ->
            val unit = when {
                name.startsWith("شبك") -> "لفة"
                name.startsWith("زاوية") || name.startsWith("بروفايل") -> "عدد"
                name.startsWith("مادة") -> "جركن"
                else -> "شيكارة"
            }
            put("محارة وبياض", name, unit)
        }

        listOf(
            "لفائف عزل بيتومين 3 مم سادة","لفائف عزل بيتومين 4 مم سادة","لفائف عزل بيتومين 4 مم مكسوة",
            "برايمر بيتوميني","عزل بيتوميني بارد","عزل أسمنتي أحادي المكون","عزل أسمنتي ثنائي المكون",
            "عزل بولي يوريثان سائل","ممبرين PVC","سيلانت PU للفواصل","شريط بنتونايت Waterstop",
            "Waterstop PVC عرض 20 سم","Waterstop PVC عرض 25 سم",
            "Backer Rod 10 مم","Backer Rod 15 مم","Backer Rod 20 مم",
        ).forEach { name ->
            val unit = when {
                name.startsWith("لفائف") -> "لفة"
                name.startsWith("Waterstop") || name.startsWith("Backer") || name.startsWith("شريط") -> "م"
                name.startsWith("ممبرين") -> "م²"
                name.startsWith("برايمر") || name.startsWith("عزل بيتوميني") || name.startsWith("عزل بولي") -> "جردل"
                name.startsWith("سيلانت") -> "عبوة"
                else -> "شيكارة"
            }
            put("عزل مائي وحراري", name, unit)
        }

        listOf(2,3,5,7,10).forEach { t ->
            put("عزل مائي وحراري", "ألواح XPS سمك $t سم", "م²")
            put("عزل مائي وحراري", "ألواح EPS سمك $t سم", "م²")
        }

        listOf(25,50,75,100).forEach { t ->
            put("عزل مائي وحراري", "صوف صخري سمك $t مم", "م²")
        }

        listOf(110,160,200,250,315).forEach { s ->
            put("صرف خارجي وشبكات", "ماسورة UPVC صرف خارجي $s مم", "م")
            put("صرف خارجي وشبكات", "كوع UPVC صرف خارجي 45° $s مم", "عدد")
            put("صرف خارجي وشبكات", "كوع UPVC صرف خارجي 90° $s مم", "عدد")
            put("صرف خارجي وشبكات", "وصلة UPVC صرف خارجي $s مم", "عدد")
        }

        listOf(160,200,250,315).forEach { s ->
            put("صرف خارجي وشبكات", "تي UPVC صرف خارجي $s مم", "عدد")
            put("صرف خارجي وشبكات", "واي UPVC صرف خارجي $s مم", "عدد")
        }

        listOf(
            "غطاء غرفة تفتيش حديد زهر 60×60","غطاء غرفة تفتيش حديد زهر 80×80",
            "غطاء غرفة تفتيش Composite 60×60","جريلة صرف زهر 30×30","جريلة صرف زهر 40×40",
            "جريلة صرف خطية","سلم غرفة تفتيش GRP","وصلة مطاط Rubber Ring","رمل إحاطة مواسير",
        ).forEach { name ->
            put("صرف خارجي وشبكات", name, when {
                name.startsWith("جريلة صرف خطية") -> "م"
                name.startsWith("رمل") -> "م³"
                else -> "عدد"
            })
        }

        listOf("1","1.5","2","2.5","3","4","6","8").forEach { inch ->
            put("مكافحة حريق", "ماسورة حديد مجلفن حريق $inch بوصة", "م")
        }

        listOf(
            "رشاش حريق Pendent 68°C","رشاش حريق Upright 68°C","رشاش حريق Sidewall 68°C",
            "Flexible Sprinkler Hose","صمام بوابة OS&Y 2 بوصة","صمام بوابة OS&Y 4 بوصة",
            "صمام عدم رجوع حريق 2 بوصة","صمام عدم رجوع حريق 4 بوصة",
            "Flow Switch","Pressure Switch","Fire Hose Reel 1 بوصة","خرطوم حريق 2.5 بوصة",
            "Landing Valve 2.5 بوصة","Fire Department Connection","طفاية بودرة 6 كجم",
            "طفاية CO2 وزن 6 كجم","خزانة حريق مفردة","خزانة حريق مزدوجة","حامل مواسير حريق","U-Bolt حريق",
        ).forEach { put("مكافحة حريق", it, "عدد") }

        listOf(
            "باركيه HDF 8 مم","باركيه HDF 10 مم","باركيه HDF 12 مم",
            "SPC أرضيات 4 مم","SPC أرضيات 5 مم","LVT أرضيات","Vinyl Roll أرضيات",
            "وزرة HDF","وزرة PVC","Foam Underlay 2 مم","Foam Underlay 3 مم",
            "Moisture Barrier","Transition Profile","End Profile","لاصق أرضيات Vinyl",
        ).forEach { name ->
            val unit = when {
                name.startsWith("وزرة") || name.contains("Profile") -> "م"
                name.startsWith("لاصق") -> "جردل"
                else -> "م²"
            }
            put("أرضيات خشبية وبدائل", name, unit)
        }

        listOf(20,25,32,40,50,63).forEach { s ->
            put("سباكة - تغذية", "وصلة اتحاد PPR $s مم", "عدد")
            put("سباكة - تغذية", "فلنشة PPR $s مم", "عدد")
            put("سباكة - تغذية", "محبس كورة PPR $s مم", "عدد")
        }

        listOf(
            "فلتر Y نحاس 1/2 بوصة","فلتر Y نحاس 3/4 بوصة","فلتر Y نحاس 1 بوصة",
            "منظم ضغط 1/2 بوصة","منظم ضغط 3/4 بوصة","منظم ضغط 1 بوصة",
            "عداد ضغط مياه","وصلة خزان 1 بوصة","وصلة خزان 1.5 بوصة","وصلة خزان 2 بوصة",
        ).forEach { put("سباكة - تغذية", it, "عدد") }

        listOf(32,40,50,75,110,160).forEach { s ->
            put("سباكة - صرف", "مسّلوب صرف UPVC $s مم", "عدد")
            put("سباكة - صرف", "كلبسة صرف UPVC $s مم", "عدد")
        }

        listOf(
            "كروس صرف 110 مم","باب تسليك 110 مم","باب تسليك 160 مم","جراب حائط مواسير صرف",
            "سيفون غسالة","سيفون غسالة أطباق","وصلة مرنة صرف مرحاض",
        ).forEach { put("سباكة - صرف", it, "عدد") }

        listOf(
            "صندوق طرد ظاهر","صندوق طرد مدفون","زر طرد شاسيه دفن","شطاف يدوي","محبس شطاف","خرطوم شطاف",
            "قناة صرف شاور Linear Drain 60 سم","قناة صرف شاور Linear Drain 80 سم","قناة صرف شاور Linear Drain 100 سم",
            "صرف حوض Click-Clack","سيفون زجاجة Bottle Trap","طقم إكسسوارات حمام","حامل ورق تواليت","حامل فوط",
        ).forEach { put("أدوات صحية", it, "عدد") }

        listOf("25","35","50","70","95","120","150","185","240").forEach { section ->
            put("كهرباء - تأسيس", "كابل نحاس 4×$section مم²", "م")
        }

        // أسلاك نحاس مفردة 450/750V — فصل النوع عن المقاس طبقًا لتصنيفات H07V
        listOf("1.5","2","2.5","4","6","10").forEach { section ->
            put(
                category = "كهرباء - تأسيس",
                name = "H07V-U 450/750V 1×$section مم²",
                unit = "لفة",
                specification = "نحاس مصمت Class 1 • PVC • للتمديدات الثابتة داخل المواسير",
                marketName = "سلك نحاس مصمت $section مم²",
            )
        }

        listOf("0.5","0.75","1").forEach { section ->
            put(
                category = "كهرباء - تأسيس",
                name = "H05V-R 300/500V 1×$section مم²",
                unit = "لفة",
                specification = "نحاس مجدول Class 2 • PVC • دوائر وتحكم خفيفة",
                marketName = "سلك نحاس مجدول خفيف $section مم²",
            )
        }

        listOf("0.5","0.75","1").forEach { section ->
            put(
                category = "كهرباء - تأسيس",
                name = "H05V-K 300/500V 1×$section مم²",
                unit = "لفة",
                specification = "نحاس مرن Class 5 • PVC • لوحات وأجهزة وتحكم",
                marketName = "سلك نحاس شعر خفيف $section مم²",
            )
        }

        listOf("1.5","2","2.5","3","4","6","10","16","25","35","50","70","95","120","150","185","240","300","400","500","630").forEach { section ->
            put(
                category = "كهرباء - تأسيس",
                name = "H07V-R 450/750V 1×$section مم²",
                unit = "لفة",
                specification = "نحاس مجدول Class 2 • PVC • للتمديدات الثابتة",
                marketName = "سلك نحاس مجدول $section مم²",
            )
        }

        listOf("1.5","2","2.5","3","4","6","10","16","25","35","50","70","95","120","150","185","240","300").forEach { section ->
            put(
                category = "كهرباء - تأسيس",
                name = "H07V-K 450/750V 1×$section مم²",
                unit = "لفة",
                specification = "نحاس مرن دقيق الشعيرات Class 5 • PVC • للوحات والتمديدات التي تحتاج مرونة",
                marketName = "سلك نحاس شعر $section مم²",
            )
        }

        // سلك أرضي أخضر/أصفر بنفس مقاطع سلك المباني الشائعة
        listOf("1.5","2","2.5","3","4","6","10","16","25","35","50","70","95","120","150","185","240","300").forEach { section ->
            put(
                category = "كهرباء - تأسيس",
                name = "PE H07V-K 450/750V 1×$section مم² أخضر/أصفر",
                unit = "لفة",
                specification = "موصل حماية أرضي نحاس مرن • أخضر/أصفر",
                marketName = "سلك أرضي شعر $section مم²",
            )
        }

        listOf(16,25,35,50,70,95,120).forEach { section ->
            put("كهرباء - تأسيس", "سلك أرضي $section مم²", "م")
        }

        listOf(
            "قضيب تأريض نحاس 1.5 م","قضيب تأريض نحاس 3 م","غرفة تأريض","وصلة قضيب تأريض",
            "شريط نحاس تأريض 25×3 مم","Cable Gland M20","Cable Gland M25","Cable Gland M32","Cable Gland M40",
            "ترامل كابل 16 مم²","ترامل كابل 25 مم²","ترامل كابل 35 مم²","ترامل كابل 50 مم²","ترامل كابل 70 مم²",
        ).forEach { name ->
            put("كهرباء - تأسيس", name, if (name.startsWith("شريط")) "م" else "عدد")
        }

        listOf(16,20,25,32,40,63).forEach { amp ->
            put("كهرباء - لوحات وحماية", "RCBO 1P+N $amp A 30mA", "عدد")
        }

        listOf(
            "Isolator 2P 40A","Isolator 2P 63A","Isolator 4P 63A","ATS 4P 100A","ATS 4P 160A",
            "قاطع رئيسي 4P 63A","قاطع رئيسي 4P 100A","SPD Type 1+2","مقياس طاقة متعدد الوظائف",
        ).forEach { put("كهرباء - لوحات وحماية", it, "عدد") }

        listOf(
            "لوح جبس بورد عادي 9.5 مم","لوح جبس بورد عادي 12.5 مم","لوح جبس بورد عادي 15 مم",
            "لوح جبس مقاوم للرطوبة 12.5 مم","لوح جبس مقاوم للرطوبة 15 مم",
            "لوح جبس مقاوم للحريق 12.5 مم","لوح جبس مقاوم للحريق 15 مم",
            "لوح جبس مقاوم للحريق والرطوبة 12.5 مم","لوح Cement Board داخلي 12.5 مم",
            "CW Stud 50 مم","CW Stud 75 مم","CW Stud 100 مم",
            "UW Track 50 مم","UW Track 75 مم","UW Track 100 مم",
            "CD Channel 60/27","UD Channel 28/27",
        ).forEach { name ->
            put("جبس بورد وأسقف", name, if (name.startsWith("لوح")) "عدد" else "م")
        }

        listOf(
            "لاصق سيراميك C1","لاصق سيراميك C2","لاصق بورسلين مرن C2TE","لاصق بلاطات كبيرة C2TE S1",
            "جراوت أسمنتي فواصل","جراوت إيبوكسي فواصل","مونة جراوت غير منكمشة",
            "مادة إضافة خرسانة ملدنة","مادة تأخير شك خرسانة","مادة معالجة خرسانة Curing Compound",
            "إيبوكسي ربط خرسانة قديم بجديد","مونة إصلاح خرسانة بوليمرية",
        ).forEach { name ->
            put(
                "مواد لاصقة وكيماويات",
                name,
                if (name.contains("مادة") || name.startsWith("إيبوكسي")) "جركن" else "شيكارة",
            )
        }

        // مسارات كهرباء ولوحات فرعية
        listOf(50,75,100,150,200,300,400,600).forEach { width ->
            put("كهرباء - تأسيس", "Cable Tray مجلفن عرض $width مم", "م")
            put("كهرباء - تأسيس", "Cable Ladder مجلفن عرض $width مم", "م")
        }
        listOf(25,40,60,80,100,150).forEach { size ->
            put("كهرباء - تأسيس", "PVC Trunking مقاس $size مم", "م")
        }
        listOf(
            "غطاء Cable Tray","وصلة Cable Tray","كوع أفقي Cable Tray","كوع رأسي Cable Tray",
            "Tee Cable Tray","Cross Cable Tray","حامل Tray جداري","حامل Tray سقفي","Threaded Rod M10 للمسارات",
            "Busbar نحاس 100A","Busbar نحاس 160A","Busbar نحاس 250A",
        ).forEach { put("كهرباء - تأسيس", it, if (it.contains("Busbar")) "عدد" else "عدد") }

        // إنذار حريق
        listOf(
            "لوحة إنذار حريق Conventional 2 Zone","لوحة إنذار حريق Conventional 4 Zone",
            "لوحة إنذار حريق Conventional 8 Zone","لوحة إنذار حريق Addressable 1 Loop",
            "لوحة إنذار حريق Addressable 2 Loop","كاشف دخان Conventional","كاشف حرارة Conventional",
            "كاشف دخان Addressable","كاشف حرارة Addressable","كاشف Multi Sensor",
            "Manual Call Point","Sounder Beacon","Fire Bell","Input Module","Output Module",
            "Monitor Module","Control Module","Short Circuit Isolator","Repeater Panel",
            "Beam Detector","Duct Smoke Detector","قاعدة كاشف حريق","بطارية 12V 7Ah",
            "كابل إنذار حريق مقاوم للحريق 2×1.5 مم²","كابل إنذار حريق مقاوم للحريق 2×2.5 مم²",
        ).forEach { name ->
            put("إنذار حريق", name, if (name.startsWith("كابل")) "م" else "عدد")
        }

        // شبكات مياه وري وHDPE
        listOf(20,25,32,40,50,63,75,90,110,160,200,250,315).forEach { s ->
            put("شبكات مياه وري", "ماسورة HDPE PE100 $s مم", "م")
        }
        listOf(25,32,40,50,63,75,90,110).forEach { s ->
            put("شبكات مياه وري", "كوع Compression HDPE $s مم", "عدد")
            put("شبكات مياه وري", "تي Compression HDPE $s مم", "عدد")
            put("شبكات مياه وري", "وصلة Compression HDPE $s مم", "عدد")
            put("شبكات مياه وري", "محبس كورة HDPE $s مم", "عدد")
        }
        listOf(
            "محبس بوابة Ductile Iron DN50","محبس بوابة Ductile Iron DN80","محبس بوابة Ductile Iron DN100",
            "محبس فراشة DN100","محبس فراشة DN150","محبس عدم رجوع DN100","Air Valve DN50",
            "عداد مياه 1 بوصة","عداد مياه 2 بوصة","عداد مياه DN50","عداد مياه DN100",
            "رشاش ري Pop-up 10 سم","رشاش ري Rotor","Solenoid Valve 1 بوصة","Solenoid Valve 2 بوصة",
            "Controller ري 4 Zone","Controller ري 8 Zone","فلتر شبكي ري 1 بوصة","فلتر شبكي ري 2 بوصة",
            "نقاطة 4 لتر/ساعة","خرطوم تنقيط 16 مم",
        ).forEach { name ->
            put("شبكات مياه وري", name, if (name.startsWith("خرطوم") || name.startsWith("نقاطة")) "م" else "عدد")
        }

        // مولدات وUPS
        listOf(
            "UPS Online 1 kVA","UPS Online 2 kVA","UPS Online 3 kVA","UPS Online 6 kVA","UPS Online 10 kVA",
            "بطارية UPS 12V 7Ah","بطارية UPS 12V 9Ah","بطارية UPS 12V 18Ah","بطارية UPS 12V 26Ah",
            "مولد ديزل 30 kVA","مولد ديزل 50 kVA","مولد ديزل 100 kVA","مولد ديزل 150 kVA",
            "مولد ديزل 250 kVA","مولد ديزل 500 kVA","ATS مولد 100A","ATS مولد 250A","ATS مولد 400A",
            "شاحن بطارية مولد","سخان مياه جاكيت مولد","كابل بطارية مولد","خزان سولار يومي",
        ).forEach { put("مولدات وUPS", it, "عدد") }

    }
}
