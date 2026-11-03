# Nool Proguard Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

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
