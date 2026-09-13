# Gson usa reflection sobre los DTOs de data/remote/dto — no ofuscar sus campos.
-keep class com.cadeteria.cadete.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
