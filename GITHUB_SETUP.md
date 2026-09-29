# GitHub setup — FAYROZ Requests

الاسم المقترح للريبو: `Fayroz-Requests`

## أول نسخة ترفع

- Version: `0.5.0`
- Tag لاحقًا: `v0.5.0`
- Commit مقترح: `Initial FAYROZ Requests v0.5 foundation`

## ملفات لا تُرفع

ملف `.gitignore` الحالي يستبعد:
- build outputs
- Android Studio local state
- `local.properties`
- APK/AAB generated files
- keystores / signing files
- secrets files

## Branching بسيط

لا نحتاج Git Flow معقد الآن.

- `main`: النسخة المستقرة.
- feature branch فقط عند تعديل كبير، مثل `feature/backup` أو `feature/ui-polish`.

## ملاحظة Gradle Wrapper

المشروع يحتوي إعداد `gradle-wrapper.properties`. عند رفع الريبو سنضيف ملفات Gradle Wrapper الرسمية كاملة (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) حتى يمكن البناء بعد Clone مباشرة.
