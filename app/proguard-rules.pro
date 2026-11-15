# CherryClip ProGuard / R8 规则
# 数据模型通过 Gson 反射反序列化，需保留字段名
-keep class com.cherryclip.app.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Gson
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ZXing
-dontwarn com.google.zxing.**

# 快捷设置磁贴 / 前台服务由系统反射实例化
-keep class com.cherryclip.app.clipboard.ClipboardTileService { *; }
-keep class com.cherryclip.app.clipboard.ClipboardMonitorService { *; }
