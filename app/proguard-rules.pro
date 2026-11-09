# Keep Android basic components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep all project classes (com.eslam.azanapp)
-keep class com.eslam.azanapp.** { *; }
-keepclassmembers class com.eslam.azanapp.** { *; }

# Keep Adhan calculation library classes
-keep class com.batoulapps.adhan.** { *; }
-keepclassmembers class com.batoulapps.adhan.** { *; }

# Keep attributes required for reflection, Compose, and JSON serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep R class fields for resource resolution
-keepclassmembers class **.R$* {
    public static <fields>;
}
