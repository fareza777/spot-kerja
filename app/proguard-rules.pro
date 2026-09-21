# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.spotkerja.**$$serializer { *; }
-keepclassmembers class com.spotkerja.** { *** Companion; }
-keepclasseswithmembers class com.spotkerja.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclasseswithmembers class com.spotkerja.** { kotlinx.serialization.KSerializer serializer(...); }

# TensorFlow Lite — kelas interpreter & native bindings
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Keep serializable models
-keep class com.spotkerja.data.** { *; }
-keep class com.spotkerja.settings.ScanOptions { *; }
-keep class com.spotkerja.settings.ScanPreset { *; }
