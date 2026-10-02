package com.fayroz.requests.data.repository

/**
 * أسعار منشورة من مواقع توريدات/موزعين في مصر.
 * كل سجل مرتبط بصنف الـMaster + الماركة + المورد + رابط المصدر الدقيق.
 *
 * ملاحظة: لا نضيف سعرًا لمواسير PPR/UPVC العامة إذا كان السعر المنشور
 * مربوطًا بـ PN/SDR/سمك غير موجود في تعريف الصنف الحالي.
 */
object OnlineMarketPriceSeed {

    data class Entry(
        val itemName: String,
        val brand: String,
        val supplierName: String,
        val supplierSpecialty: String,
        val listName: String,
        val listSourceUrl: String,
        val sourceUrl: String,
        val priceDate: String,
        val listPrice: Double,
        val netPrice: Double,
        val priceUnit: String,
        val notes: String = "",
    ) {
        val discountPercent: Double
            get() = if (listPrice <= 0.0 || netPrice >= listPrice) 0.0
            else ((listPrice - netPrice) / listPrice * 100.0).coerceIn(0.0, 100.0)
    }

    private const val ELSEWEDY_SOURCE = "https://elmahal.com/elsewedy-cables/"
    private const val TAWREDAAT_PPR = "https://tawredaat.com/ar-eg/collections/water-fittings/br"
    private const val TAWREDAAT_ELECTRICAL = "https://tawredaat.com/ar-eg/collections/electrical/new-ega"
    private const val MAHGOUB_BANNINGER = "https://www.mahgoub.com/ar/%D8%A7%D9%84%D9%85%D8%B5%D8%A7%D9%86%D8%B9/%D8%A8%D8%A7%D9%86%D9%86%D8%AC%D8%B1/"
    private const val SCHNEIDER_ESHOP = "https://eshop.se.com/eg/"
    private const val ELECTRICITY_STORE_CABLES = "https://electricity-store.com/ar/product-category/electrical-materials-ar/%D8%A7%D9%84%D8%A3%D8%B3%D9%84%D8%A7%D9%83-%D9%88%D8%A7%D9%84%D9%83%D8%A7%D8%A8%D9%84%D8%A7%D8%AA-ar/"
    private const val MAHGOUB_ELSHERIF = "https://www.mahgoub.com/ar/%D9%85%D8%B3%D8%AA%D9%84%D8%B2%D9%85%D8%A7%D8%AA/%D9%84%D9%88%D8%A7%D8%B2%D9%85-%D8%B3%D8%A8%D8%A7%D9%83%D8%A9/%D8%A7%D9%84%D8%B4%D8%B1%D9%8A%D9%81/"
    private const val MAHGOUB_AQUATHERM = "https://www.mahgoub.com/ar/%D8%A7%D9%84%D9%85%D8%B5%D8%A7%D9%86%D8%B9/%D8%A3%D9%83%D9%88%D8%A7%D8%AB%D9%8A%D8%B1%D9%85/"

    val entries: List<Entry> = buildList {
        // Elsewedy — published 10/09/2026 on Elmahal / Samah Gibril.
        val stranded = mapOf(
            "1.5" to 1541.28,
            "2" to 2049.72,
            "2.5" to 2496.60,
            "3" to 2985.66,
            "4" to 3878.28,
            "6" to 5839.08,
            "10" to 9593.10,
            "16" to 15354.66,
        )
        val flexible = mapOf(
            "1.5" to 1717.98,
            "2" to 2248.08,
            "2.5" to 2811.24,
            "3" to 3298.02,
            "4" to 4344.54,
            "6" to 6421.62,
            "10" to 11366.94,
            "16" to 17903.70,
        )
        stranded.forEach { (section, price) ->
            add(
                Entry(
                    itemName = "H07V-R 450/750V 1×$section مم²",
                    brand = "Elsewedy Cables",
                    supplierName = "سامح جبريل",
                    supplierSpecialty = "أسلاك وكابلات كهرباء",
                    listName = "أسعار أسلاك السويدي - 10/09/2026",
                    listSourceUrl = ELSEWEDY_SOURCE,
                    sourceUrl = ELSEWEDY_SOURCE,
                    priceDate = "2026-09-10",
                    listPrice = price,
                    netPrice = price,
                    priceUnit = "لفة",
                    notes = "لفة 100 متر • مجدول 7 شعرة • السعر المنشور بدون بيان واضح للضريبة أو الشحن",
                )
            )
        }
        flexible.forEach { (section, price) ->
            add(
                Entry(
                    itemName = "H07V-K 450/750V 1×$section مم²",
                    brand = "Elsewedy Cables",
                    supplierName = "سامح جبريل",
                    supplierSpecialty = "أسلاك وكابلات كهرباء",
                    listName = "أسعار أسلاك السويدي - 10/09/2026",
                    listSourceUrl = ELSEWEDY_SOURCE,
                    sourceUrl = ELSEWEDY_SOURCE,
                    priceDate = "2026-09-10",
                    listPrice = price,
                    netPrice = price,
                    priceUnit = "لفة",
                    notes = "لفة 100 متر • شعر ناعم • السعر المنشور بدون بيان واضح للضريبة أو الشحن",
                )
            )
        }


        // Egyptian Cables — Electricity Store, 100 m rolls.
        val egyptianCablesStranded = mapOf(
            "1.5" to 886.70,
            "2" to 1175.00,
            "2.5" to 1425.20,
            "3" to 1698.00,
            "4" to 2197.00,
            "6" to 3291.30,
            "10" to 5383.50,
            "16" to 8582.70,
            "25" to 15446.20,
            "35" to 20944.00,
        )
        val egyptianCablesFlexible = mapOf(
            "1.5" to 1005.90,
            "2" to 1329.20,
            "2.5" to 1630.90,
            "3" to 1900.90,
            "4" to 2492.80,
            "6" to 3664.90,
            "10" to 6458.80,
            "16" to 10147.20,
            "25" to 18587.80,
            "35" to 25918.20,
        )
        egyptianCablesStranded.forEach { (section, price) ->
            add(
                Entry(
                    itemName = "H07V-R 450/750V 1×$section مم²",
                    brand = "الكابلات المصرية",
                    supplierName = "Electricity Store",
                    supplierSpecialty = "توريدات كهربائية",
                    listName = "Electricity Store - الكابلات المصرية - 02/10/2026",
                    listSourceUrl = ELECTRICITY_STORE_CABLES,
                    sourceUrl = if (section == "2.5") {
                        "https://electricity-store.com/ar/product/%D8%A7%D9%84%D9%83%D8%A7%D8%A8%D9%84%D8%A7%D8%AA-%D8%A7%D9%84%D9%85%D8%B5%D8%B1%D9%8A%D8%A9-%D8%B3%D9%84%D9%83-%D9%83%D9%87%D8%B1%D8%A8%D8%A7%D8%A1-%D9%86%D8%AD%D8%A7%D8%B3-%D9%85%D8%B5%D9%85%D8%AA-10/"
                    } else ELECTRICITY_STORE_CABLES,
                    priceDate = "2026-10-02",
                    listPrice = price,
                    netPrice = price,
                    priceUnit = "لفة",
                    notes = "لفة 100 متر • السعر الظاهر بالموقع وقت الفحص • لا يوجد بيان واضح للضريبة أو الشحن",
                )
            )
        }
        egyptianCablesFlexible.forEach { (section, price) ->
            add(
                Entry(
                    itemName = "H07V-K 450/750V 1×$section مم²",
                    brand = "الكابلات المصرية",
                    supplierName = "Electricity Store",
                    supplierSpecialty = "توريدات كهربائية",
                    listName = "Electricity Store - الكابلات المصرية - 02/10/2026",
                    listSourceUrl = ELECTRICITY_STORE_CABLES,
                    sourceUrl = ELECTRICITY_STORE_CABLES,
                    priceDate = "2026-10-02",
                    listPrice = price,
                    netPrice = price,
                    priceUnit = "لفة",
                    notes = "لفة 100 متر • نحاس شعر • السعر الظاهر بالموقع وقت الفحص • لا يوجد بيان واضح للضريبة أو الشحن",
                )
            )
        }

        // Schneider Electric Egypt eShop — exact seller offers where the seller is explicitly shown.
        add(
            Entry(
                itemName = "RCCB 2P 25 A 30mA",
                brand = "Schneider Electric",
                supplierName = "MAS Electric",
                supplierSpecialty = "لوحات وحماية كهربائية",
                listName = "Schneider eShop - 02/10/2026",
                listSourceUrl = SCHNEIDER_ESHOP,
                sourceUrl = "https://eshop.se.com/eg/residual-current-circuit-breaker-rccb-resi9-2p-25a-ac-type-30ma.html",
                priceDate = "2026-10-02",
                listPrice = 2095.51,
                netPrice = 2095.51,
                priceUnit = "عدد",
                notes = "Schneider Resi9 • SKU R9R51225 • السعر لا يشمل الضريبة • الشحن يحدده البائع",
            )
        )
        listOf("MAS Electric", "Gila AL Tawakol Electric", "New Light Electric").forEach { seller ->
            add(
                Entry(
                    itemName = "RCCB 2P 40 A 30mA",
                    brand = "Schneider Electric",
                    supplierName = seller,
                    supplierSpecialty = "لوحات وحماية كهربائية",
                    listName = "Schneider eShop - 02/10/2026",
                    listSourceUrl = SCHNEIDER_ESHOP,
                    sourceUrl = "https://eshop.se.com/eg/residual-current-circuit-breaker-rccb-resi9-2p-40a-ac-type-30ma.html",
                    priceDate = "2026-10-02",
                    listPrice = 3772.61,
                    netPrice = 2376.74,
                    priceUnit = "عدد",
                    notes = "Schneider Resi9 • SKU R9R51240 • السعر لا يشمل الضريبة • الشحن يحدده البائع",
                )
            )
        }

        // New Ega electrical fittings from Tawredaat.
        add(
            Entry(
                itemName = "كوع ماسورة كهرباء 20 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/ar-eg/products/bend-round-conduit-20-mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 4.63,
                netPrice = 3.64,
                priceUnit = "عدد",
                notes = "New Ega • كود NGD/B20 • السعر المنشور على توريدات",
            )
        )
        add(
            Entry(
                itemName = "كوع ماسورة كهرباء 25 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/ar-eg/products/bend-round-conduit-25-mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 7.18,
                netPrice = 5.64,
                priceUnit = "عدد",
                notes = "New Ega • كود NGD/B25 • السعر المنشور على توريدات",
            )
        )
        add(
            Entry(
                itemName = "جلبة ماسورة كهرباء 25 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/ar-eg/collections/conduits-fittings/products/coupler-25-mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 3.19,
                netPrice = 2.64,
                priceUnit = "عدد",
                notes = "New Ega • كود NGE/C25 • السعر المنشور على توريدات",
            )
        )
        add(
            Entry(
                itemName = "كلبسة ماسورة كهرباء 25 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/ar-eg/products/strap-bar-saddle-25-mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 7.90,
                netPrice = 6.21,
                priceUnit = "عدد",
                notes = "New Ega • كود NGB/S25 • أفيز/كلبسة بالقاعدة • السعر المنشور على توريدات",
            )
        )


        add(
            Entry(
                itemName = "جلبة ماسورة كهرباء 20 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/en-eg/products/coupler-20-mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 2.87,
                netPrice = 2.26,
                priceUnit = "عدد",
                notes = "New Ega • كود NGE/C20 • السعر المنشور على توريدات",
            )
        )
        add(
            Entry(
                itemName = "كلبسة ماسورة كهرباء 20 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/en-eg/products/clip-saddle-20mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 2.07,
                netPrice = 1.63,
                priceUnit = "عدد",
                notes = "New Ega • كود NGS/S20 • كلبسة PVC • السعر المنشور على توريدات",
            )
        )
        add(
            Entry(
                itemName = "كلبسة ماسورة كهرباء 40 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/en-eg/products/clip-saddle-40mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 3.75,
                netPrice = 3.11,
                priceUnit = "عدد",
                notes = "New Ega • كود NGS/S40 • كلبسة PVC • السعر المنشور على توريدات",
            )
        )
        add(
            Entry(
                itemName = "كوع ماسورة كهرباء 40 مم",
                brand = "New Ega",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat كهرباء - 02/10/2026",
                listSourceUrl = TAWREDAAT_ELECTRICAL,
                sourceUrl = "https://tawredaat.com/ar-eg/products/bend-round-conduit-40-mm-new-ega",
                priceDate = "2026-10-02",
                listPrice = 19.95,
                netPrice = 15.68,
                priceUnit = "عدد",
                notes = "New Ega • كود NGD/B40 • السعر المنشور على توريدات",
            )
        )

        // BR / Bänninger PPR fittings — only fittings with unambiguous size/type.
        add(
            Entry(
                itemName = "كوع PPR 25 مم",
                brand = "BR",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat سباكة BR - 02/10/2026",
                listSourceUrl = TAWREDAAT_PPR,
                sourceUrl = "https://tawredaat.com/ar-eg/products/br-water-supply-fitting-elbow-90-25mm-351020002",
                priceDate = "2026-10-02",
                listPrice = 17.50,
                netPrice = 15.53,
                priceUnit = "عدد",
                notes = "BR/Bänninger • كوع لحام 90° أخضر • كود 351020002",
            )
        )
        add(
            Entry(
                itemName = "تي PPR 25 مم",
                brand = "BR",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat سباكة BR - 02/10/2026",
                listSourceUrl = TAWREDAAT_PPR,
                sourceUrl = "https://tawredaat.com/ar-eg/collections/water-fittings/br?page=18",
                priceDate = "2026-10-02",
                listPrice = 23.75,
                netPrice = 21.08,
                priceUnit = "عدد",
                notes = "BR/Bänninger • تي لحام 90° أخضر • كود 351050002",
            )
        )
        add(
            Entry(
                itemName = "تي PPR 32 مم",
                brand = "BR",
                supplierName = "Tawredaat",
                supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                listName = "Tawredaat سباكة BR - 02/10/2026",
                listSourceUrl = TAWREDAAT_PPR,
                sourceUrl = "https://tawredaat.com/ar-eg/collections/water-fittings/br?page=18",
                priceDate = "2026-10-02",
                listPrice = 43.25,
                netPrice = 38.38,
                priceUnit = "عدد",
                notes = "BR/Bänninger • تي لحام 90° أخضر • كود 351050003",
            )
        )


        listOf(
            Triple("20", 18.50, 16.42),
            Triple("40", 85.00, 75.43),
            Triple("50", 136.25, 120.91),
            Triple("63", 196.50, 174.37),
        ).forEach { (size, regular, sale) ->
            add(
                Entry(
                    itemName = "تي PPR $size مم",
                    brand = "BR",
                    supplierName = "Tawredaat",
                    supplierSpecialty = "توريدات مواد بناء وتشطيب B2B",
                    listName = "Tawredaat سباكة BR - 02/10/2026",
                    listSourceUrl = TAWREDAAT_PPR,
                    sourceUrl = "https://tawredaat.com/ar-eg/collections/water-fittings/br?page=18",
                    priceDate = "2026-10-02",
                    listPrice = regular,
                    netPrice = sale,
                    priceUnit = "عدد",
                    notes = "BR/Bänninger • تي لحام 90° أخضر • السعر المنشور على توريدات",
                )
            )
        }


        // El Sherif — Mahgoub. Only exact PPR weld fittings mapped to existing master items.
        listOf(
            Triple("جلبة PPR 20 مم", 13.75, 13.75),
            Triple("جلبة PPR 25 مم", 15.05, 15.05),
            Triple("كوع PPR 25 مم", 18.45, 18.45),
            Triple("تي PPR 25 مم", 25.21, 25.21),
        ).forEach { (itemName, regular, sale) ->
            add(
                Entry(
                    itemName = itemName,
                    brand = "الشريف",
                    supplierName = "Mahgoub",
                    supplierSpecialty = "أدوات صحية ولوازم سباكة",
                    listName = "Mahgoub الشريف - 02/10/2026",
                    listSourceUrl = MAHGOUB_ELSHERIF,
                    sourceUrl = MAHGOUB_ELSHERIF,
                    priceDate = "2026-10-02",
                    listPrice = regular,
                    netPrice = sale,
                    priceUnit = "عدد",
                    notes = "الشريف • قطعة PPR لحام • السعر الظاهر بمحجوب وقت الفحص",
                )
            )
        }

        // Aquatherm — Mahgoub. Exact weld fittings only.
        listOf(
            Triple("جلبة PPR 20 مم", 36.73, 33.06),
            Triple("جلبة PPR 25 مم", 50.96, 45.86),
            Triple("كوع PPR 20 مم", 37.59, 33.83),
            Triple("كوع PPR 25 مم", 62.10, 55.89),
        ).forEach { (itemName, regular, sale) ->
            add(
                Entry(
                    itemName = itemName,
                    brand = "aquatherm",
                    supplierName = "Mahgoub",
                    supplierSpecialty = "أدوات صحية ولوازم سباكة",
                    listName = "Mahgoub Aquatherm - 02/10/2026",
                    listSourceUrl = MAHGOUB_AQUATHERM,
                    sourceUrl = MAHGOUB_AQUATHERM,
                    priceDate = "2026-10-02",
                    listPrice = regular,
                    netPrice = sale,
                    priceUnit = "عدد",
                    notes = "Aquatherm • قطعة PPR لحام • السعر الظاهر بمحجوب وقت الفحص",
                )
            )
        }

        // Same BR fittings from Mahgoub to preserve supplier-to-supplier comparison.
        add(
            Entry(
                itemName = "كوع PPR 25 مم",
                brand = "BR",
                supplierName = "Mahgoub",
                supplierSpecialty = "أدوات صحية ولوازم سباكة",
                listName = "Mahgoub Bänninger - 02/10/2026",
                listSourceUrl = MAHGOUB_BANNINGER,
                sourceUrl = "https://www.mahgoub.com/ar/%D9%85%D8%B3%D8%AA%D9%84%D8%B2%D9%85%D8%A7%D8%AA/%D9%84%D9%88%D8%A7%D8%B2%D9%85-%D8%B3%D8%A8%D8%A7%D9%83%D8%A9/%D8%A8%D8%A7%D9%86%D9%86%D8%AC%D8%B1/%D9%83%D9%88%D8%B9-%D9%84%D8%AD%D8%A7%D9%85-4%2F3-%D8%A8%D9%88%D8%B5%D9%87-%D8%B2%D8%A7%D9%88%D9%8A%D8%A9-90-1610000339.html",
                priceDate = "2026-10-02",
                listPrice = 22.00,
                netPrice = 21.12,
                priceUnit = "عدد",
                notes = "Bänninger/BR • 3/4 بوصة ≈ 25 مم • كود 351020002 • بالقطعة",
            )
        )
        add(
            Entry(
                itemName = "تي PPR 25 مم",
                brand = "BR",
                supplierName = "Mahgoub",
                supplierSpecialty = "أدوات صحية ولوازم سباكة",
                listName = "Mahgoub Bänninger - 02/10/2026",
                listSourceUrl = MAHGOUB_BANNINGER,
                sourceUrl = "https://www.mahgoub.com/ar/%D9%85%D8%B3%D8%AA%D9%84%D8%B2%D9%85%D8%A7%D8%AA/%D9%84%D9%88%D8%A7%D8%B2%D9%85-%D8%B3%D8%A8%D8%A7%D9%83%D8%A9/%D8%A8%D8%A7%D9%86%D9%86%D8%AC%D8%B1/%D8%AA%D9%89-%D9%84%D8%AD%D8%A7%D9%85-%D8%A8%D9%88%D9%84%D9%89-%D8%A8%D8%B1%D9%88%D8%A8%D9%84%D9%8A%D9%86-43-%D8%A8%D9%88%D8%B5%D9%87-1610000386.html",
                priceDate = "2026-10-02",
                listPrice = 29.75,
                netPrice = 28.56,
                priceUnit = "عدد",
                notes = "Bänninger/BR • 3/4 بوصة ≈ 25 مم • كود 351050002 • بالقطعة",
            )
        )
        add(
            Entry(
                itemName = "تي PPR 32 مم",
                brand = "BR",
                supplierName = "Mahgoub",
                supplierSpecialty = "أدوات صحية ولوازم سباكة",
                listName = "Mahgoub Bänninger - 02/10/2026",
                listSourceUrl = MAHGOUB_BANNINGER,
                sourceUrl = "https://www.mahgoub.com/ar/%D9%85%D8%B3%D8%AA%D9%84%D8%B2%D9%85%D8%A7%D8%AA/%D9%84%D9%88%D8%A7%D8%B2%D9%85-%D8%B3%D8%A8%D8%A7%D9%83%D8%A9/%D8%A8%D8%A7%D9%86%D9%86%D8%AC%D8%B1/%D8%AA%D9%8A-%D9%84%D8%AD%D8%A7%D9%85-%D8%A8%D9%88%D9%84%D9%89-%D8%A8%D8%B1%D9%88%D8%A8%D9%84%D9%8A%D9%86-1-%D8%A8%D9%88%D8%B5%D9%87-1610000387.html",
                priceDate = "2026-10-02",
                listPrice = 53.75,
                netPrice = 51.60,
                priceUnit = "عدد",
                notes = "Bänninger/BR • 1 بوصة ≈ 32 مم • كود 351050003 • بالقطعة",
            )
        )
        add(
            Entry(
                itemName = "جلبة PPR 25 مم",
                brand = "BR",
                supplierName = "Mahgoub",
                supplierSpecialty = "أدوات صحية ولوازم سباكة",
                listName = "Mahgoub Bänninger - 02/10/2026",
                listSourceUrl = MAHGOUB_BANNINGER,
                sourceUrl = "https://www.mahgoub.com/ar/%D9%85%D8%B3%D8%AA%D9%84%D8%B2%D9%85%D8%A7%D8%AA/%D9%84%D9%88%D8%A7%D8%B2%D9%85-%D8%B3%D8%A8%D8%A7%D9%83%D8%A9/%D8%A8%D8%A7%D9%86%D9%86%D8%AC%D8%B1/%D8%AC%D9%84%D8%A8%D9%87-%D9%84%D8%AD%D8%A7%D9%85-%D8%A8%D9%88%D9%84%D9%89-%D8%A8%D8%B1%D9%88%D8%A8%D9%84%D9%8A%D9%86-4%2F3-%D8%A8%D9%88%D8%B5%D9%87-1610000427.html",
                priceDate = "2026-10-02",
                listPrice = 17.75,
                netPrice = 17.04,
                priceUnit = "عدد",
                notes = "Bänninger/BR • 3/4 بوصة ≈ 25 مم • كود 351070002 • بالقطعة",
            )
        )
        add(
            Entry(
                itemName = "كوع PPR 32 مم",
                brand = "BR",
                supplierName = "Mahgoub",
                supplierSpecialty = "أدوات صحية ولوازم سباكة",
                listName = "Mahgoub Bänninger - 02/10/2026",
                listSourceUrl = MAHGOUB_BANNINGER,
                sourceUrl = "https://www.mahgoub.com/ar/%D9%85%D8%B3%D8%AA%D9%84%D8%B2%D9%85%D8%A7%D8%AA/%D9%84%D9%88%D8%A7%D8%B2%D9%85-%D8%B3%D8%A8%D8%A7%D9%83%D8%A9/%D8%A8%D8%A7%D9%86%D9%86%D8%AC%D8%B1/%D9%83%D9%88%D8%B9-%D9%84%D8%AD%D8%A7%D9%85-%D8%A8%D9%88%D9%84%D9%89-%D8%A8%D8%B1%D9%88%D8%A8%D9%84%D9%8A%D9%86-1-%D8%A8%D9%88%D8%B5%D9%87-%D8%B2%D8%A7%D9%88%D9%8A%D8%A9-90-%D8%A3%D8%AE%D8%B6%D8%B1-1610000340.html",
                priceDate = "2026-10-02",
                listPrice = 42.00,
                netPrice = 40.32,
                priceUnit = "عدد",
                notes = "Bänninger/BR • 1 بوصة ≈ 32 مم • كود 351020003 • بالقطعة",
            )
        )
    }
}
