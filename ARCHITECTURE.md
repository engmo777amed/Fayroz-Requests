# FAYROZ Requests Architecture — V0.5

## المبدأ

التطبيق Lean ومبني حول **Item**، وليس حول Dashboard أو نظام مخازن.

```text
Project
  → Request Sheet
    → Request Line
      → Item
      → Quantity / Unit / Usage

Supplier
  → Price List (version / date / source)
    → Supplier Price
      → Item
      → List Price
      → Applied Discount
      → Net Price
      → Price Date
```

كل سعر مورد يُحفظ كسجل تاريخي. لا يوجد "سعر وحيد" للصنف.

## قواعد الخصم

`PricingEngine` يختار الخصم بالترتيب:
1. خصم ITEM خاص بالمورد والصنف.
2. خصم CATEGORY خاص بالمورد والتصنيف.
3. `defaultDiscountPercent` للمورد.

في الاستيراد، إذا كان الصف يحتوي خصمًا صالحًا يتم استخدامه لذلك السجل، وإلا يتم الرجوع لمحرك الخصومات.

## استيراد الليستات

1. اختيار XLSX أو CSV.
2. قراءة الملف محليًا.
3. اكتشاف Header Row.
4. ربط الأعمدة.
5. مطابقة الأصناف: code → normalized name → suggestion.
6. مراجعة unresolved rows.
7. import transaction + upsert داخل نفس PriceList.
8. حفظ اسم الملف كـ sourceReference.

## تاريخ السعر — V0.5

لا يوجد جدول جديد لتاريخ الأسعار. التاريخ مبني على `supplier_prices` نفسه.

`observeItemPriceHistory(itemId)` ينفذ Join مع:
- `suppliers` لإظهار اسم المورد.
- `price_lists` لإظهار اسم الليستة إن كان السجل قادمًا من ليستة.

هذا يمنع ازدواج البيانات ويحافظ على Schema بسيط.

## Price Freshness — V0.5

`PriceFreshnessEngine` Pure Kotlin ولا يعتمد على Android:
- `0..30 days` → FRESH.
- `31..90 days` → REVIEW.
- `>90 days` → OLD.

هو مؤشر واجهة فقط ولا يغير السعر ولا يمنع استخدامه.

## تسعير الكشف

`SheetPricingScreen` يحصل على أحدث سعر لكل `Supplier + Item`.

لكل Line:
- List Price
- Discount
- Net Unit Price
- Quantity
- Line Total
- Price Date + Freshness

ثم يجمع الخطوط حسب المورد لإظهار التغطية والإجمالي.

`Best Mix` = مجموع أقل `Line Total` لكل بند، ويظهر فقط عند وجود سعر لكل البنود.

## التصدير — V0.5

### PDF
`PricingExport.writePdf` يستخدم `android.graphics.pdf.PdfDocument` مباشرة.

### XLSX
`PricingExport.writeXlsx` يكتب حزمة Open XML صغيرة بواسطة `ZipOutputStream`:
- `[Content_Types].xml`
- `_rels/.rels`
- `xl/workbook.xml`
- `xl/_rels/workbook.xml.rels`
- `xl/styles.xml`
- `xl/worksheets/sheet1.xml`

لا توجد مكتبة Apache POI أو مكتبة PDF خارجية، لتقليل حجم واعتماديات التطبيق.

## الشاشات حتى V0.5

1. Home
2. Projects
3. Request Sheets
4. Request Sheet Editor
5. Items
6. Item Price History
7. Suppliers
8. Supplier Price Lists
9. Price List Entries
10. Price List Import Wizard
11. Pricing Sheets
12. Sheet Pricing Comparison + Export

## حدود النسخة الحالية

غير موجود عمدًا:
- مخازن / صرف / اعتماد.
- مستخلصات صنايعية.
- Purchase Orders.
- Dashboard / Charts متقدمة.
- Backup UI كامل.

الهدف الحفاظ على مسار استخدام سريع وواضح قبل أي توسع إضافي.
