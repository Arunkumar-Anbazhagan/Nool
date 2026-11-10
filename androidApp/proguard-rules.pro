# Nool Proguard Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep all Nool core models, indexing, and UI classes
-keep class com.nool.** { *; }
-keep interface com.nool.** { *; }
-keep enum com.nool.** { *; }

# Koin Dependency Injection & ViewModels
-keep class io.insert.koin.** { *; }
-dontwarn io.insert.koin.**
-keep class * extends androidx.lifecycle.ViewModel { *; }

# App Enums (Preserve values(), valueOf(), entries for DataStore & UI)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static **[] getEntries();
    **[] $VALUES;
}

# SQLDelight & SQLite Index
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# DataStore & Preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose Runtime & UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# JGit & SSH
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# JGit SPI and transport factories
-keep public class * extends org.eclipse.jgit.transport.SshSessionFactory { public <init>(); }
-keep public class * extends org.eclipse.jgit.transport.CredentialsProvider { *; }
-keep class org.eclipse.jgit.transport.sshd.** { *; }
-dontwarn org.eclipse.jgit.transport.sshd.**

# Bouncy Castle (used optionally by JSch / SSH)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Security Crypto & Tink
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.slf4j.impl.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
