# Nool Proguard Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Koin Dependency Injection & ViewModels
-keep class io.insert.koin.** { *; }
-dontwarn io.insert.koin.**
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.nool.app.ui.** { *; }
-keep class com.nool.app.di.** { *; }

# App Enums (Preserve values(), valueOf(), entries for DataStore & UI)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static **[] getEntries();
    **[] $VALUES;
}
-keep enum com.nool.app.** { *; }
-keep class com.nool.app.settings.** { *; }

# SQLDelight & SQLite Index
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**
-keep class com.nool.app.db.** { *; }
-keep class com.nool.app.domain.** { *; }

# DataStore & Preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose Runtime & UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

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
