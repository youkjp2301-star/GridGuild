# Official Vehicle uses runtime permission metadata; NaviHelper exposes Binder/Parcelable APIs.
# Preserve the SDK boundary conservatively while removing unused UI/library code.
-keep class ai.pleos.** { *; }
# Keep the small app/domain boundary stable for release instrumentation and saved-state code.
# The main size reduction comes from unused library classes and legacy resources.
-keep class com.gridguild.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
