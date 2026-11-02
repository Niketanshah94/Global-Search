# Hilt, Room, WorkManager, Compose and kotlinx.serialization ship their own consumer rules.
# Add app-specific keep rules below only when a release build proves they are needed.

# MediaPipe Tasks loads classes and protos reflectively from native code.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**

# Never keep or log query text: no rules here may reference analytics/crash reporting of queries.
