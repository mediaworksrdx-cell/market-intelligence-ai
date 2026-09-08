
# Keep rules for Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep class * extends kotlinx.serialization.internal.GeneratedSerializer { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}

# Keep rules for Retrofit & OkHttp
-dontwarn retrofit2.Platform$Java8
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.jakewharton.retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# OkHttp - GraalVM warnings (Fix for missing classes in R8)
-dontwarn okhttp3.internal.graal.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn org.graalvm.nativeimage.**

# Kotlin Parcelize (Amplify might use this)
-dontwarn kotlinx.parcelize.Parcelize

# Keep rules for Hilt and Dagger
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class com.example.marketintelligence.Hilt_MarketIntelligenceApplication { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.android.internal.lifecycle.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.android.internal.modules.** { *; }
-keep class dagger.hilt.internal.generatesrootinput.codegen.** { *; }
-keep class dagger.hilt.android.HiltAndroidApp { *; }
-keep class com.example.marketintelligence.MarketIntelligenceApplication { *; }

# Keep rules for Google AI (Gemini) SDK and its dependencies (gRPC, Protobuf)
-keep,allowobfuscation,allowshrinking class com.google.protobuf.** { *; }
-keep class io.grpc.internal.** { *; }
-keepclassmembers class * {
    @io.grpc.BindableService *;
}
-keep class com.google.common.util.concurrent.** { *; }
-keep class com.google.api.gax.** { *; }
-keep class com.google.longrunning.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }

# Zerodha Kite Connect
-keep class com.zerodhatech.** { *; }
-keep class com.google.gson.** { *; }
