# R8 rules for the release build.

# Gson reads and writes these classes by field name. Rule and CurrencyOverride are saved as JSON in
# DataStore and have no @SerializedName, so a renamed field would make the saved data unreadable.
-keepclassmembers class com.fidriyanto.banktracker.data.datasource.remote.dto.** { <fields>; }
-keepclassmembers class com.fidriyanto.banktracker.data.model.** { <fields>; }
-keepclassmembers class com.fidriyanto.banktracker.domain.model.** { <fields>; }

# Gson and Retrofit read generic types such as TypeToken<List<Rule>> at run time.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# LiteRT-LM calls native code by class and method name.
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
