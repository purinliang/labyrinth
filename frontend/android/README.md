# Labyrinth Android

A minimal Kotlin Android application with a welcome screen using XML views.
The package and application ID are `com.purin.labyrinth`. Backend integration
can be added as the application develops.

## Open and run

1. In Android Studio, choose **File > Open** and select `frontend/android`.
2. Allow Gradle sync to finish. Use the bundled JDK (17 or newer).
3. In **Tools > SDK Manager**, install Android SDK Platform 36 and
   Android SDK Build-Tools 36.0.0. Accept the SDK license prompts.
4. Select the `app` configuration and an emulator or connected Android
   device running Android 8.0 (API 26) or newer, then click **Run**.

Android Studio creates the ignored `local.properties` with your SDK path.
For terminal builds, set `ANDROID_HOME` to your SDK installation or create
`local.properties` containing `sdk.dir=C:/path/to/Android/Sdk`.
Set `JAVA_HOME` to a JDK installation if Java is not on your path.

## Build and check

From this directory on Windows:

```powershell
.\gradlew.bat assembleDebug lintDebug
```

On macOS or Linux:

```sh
sh gradlew assembleDebug lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
The first build downloads Gradle and its dependencies and needs internet
access. The wrapper pins Gradle 9.1.0; the build uses Android Gradle Plugin
9.0.1 with built-in Kotlin support. Gradle 9.1 supports the JDK 25 bundled
with Android Studio 2026.2.1.

## Start editing

- `app/src/main/java/com/purin/labyrinth/MainActivity.kt`: entry activity.
- `app/src/main/res/layout/activity_main.xml`: welcome screen layout.
- `app/src/main/res/values/strings.xml`: user-visible text.
- `app/src/main/AndroidManifest.xml`: application and activity declarations.

For future backend integration, the standard Android emulator reaches the
host at `10.0.2.2`, rather than `localhost`. The backend listens on port
8080. This starter does not yet request network access or allow cleartext
HTTP; configure these deliberately when adding a development API client.
