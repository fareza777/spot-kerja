# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.spotkerja.**$$serializer { *; }
-keepclassmembers class com.spotkerja.** { *** Companion; }
-keepclasseswithmembers class com.spotkerja.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclasseswithmembers class com.spotkerja.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep serializable models
-keep class com.spotkerja.data.** { *; }
