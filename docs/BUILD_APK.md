# Building the FoundIt APK

There are two kinds of APK:

| | Debug APK | Release APK |
|---|---|---|
| Use for | Quick testing on your own phone | **Submission / sharing** (the course requires this) |
| Signed with | Android Studio's automatic debug key | **Your own keystore** |
| Output | `app/build/outputs/apk/debug/app-debug.apk` | `app/build/outputs/apk/release/app-release.apk` |

Before building either one, `local.properties` must contain `SUPABASE_URL` and
`SUPABASE_KEY` (see [SETUP.md](../SETUP.md), Step 5).

---

## Option A: Android Studio (easiest)

1. Open the project in Android Studio.
2. Go to **Build → Generate Signed App Bundle or APK…** → choose **APK** → **Next**.
3. Under *Key store path*, click **Create new…**:
   - **Key store path:** pick a location **outside the project folder**, e.g.
     `C:\Users\<you>\keys\foundit-release.jks`
   - Choose a strong **password** and confirm it.
   - **Alias:** `foundit`, with the same or another password.
   - **Validity:** 25 years. Fill in at least *First and Last Name*.
   - Click **OK**.
4. Click **Next**, select the **release** build variant, then click **Create**.
5. When it finishes, click **locate** in the popup. The APK is at
   `app/release/app-release.apk` (or `app/build/outputs/apk/release/`).

> **Keep the `.jks` file and its passwords safe.** You need the same key to publish
> updates. Never commit them to GitHub. `*.jks` and `keystore.properties` are already in
> `.gitignore`.

## Option B: Command line (repeatable)

1. Create the keystore once (run it in a terminal; it asks for passwords and your name):

   ```bash
   keytool -genkeypair -v -keystore C:/Users/<you>/keys/foundit-release.jks -alias foundit -keyalg RSA -keysize 2048 -validity 9125
   ```

   `keytool` comes with Android Studio: `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`.

2. In the project root (next to `settings.gradle.kts`), create **`keystore.properties`**:

   ```properties
   storeFile=C:/Users/<you>/keys/foundit-release.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=foundit
   keyPassword=YOUR_KEY_PASSWORD
   ```

   This file is git-ignored. `app/build.gradle.kts` reads it automatically.

3. Build:

   ```bash
   ./gradlew assembleRelease
   ```

   (On Windows `cmd`, use `gradlew assembleRelease`.) The output is
   `app/build/outputs/apk/release/app-release.apk`.

   If `keystore.properties` is missing, Gradle builds `app-release-unsigned.apk`
   instead. Android won't install that file.

---

## Installing the APK on a phone

**Option 1: copy the file**
1. Send `app-release.apk` to the phone (USB cable, Google Drive, Messenger, etc.).
2. Tap the file on the phone. If Android blocks it, allow **Install unknown apps** for the
   app you opened it with (Files, Drive, Chrome…).
3. Tap **Install**, then **Open**.

**Option 2: USB with adb** (enable *Developer options → USB debugging* first)

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

**If you get "App not installed":** uninstall any older FoundIt first. A debug build and a
release build are signed with different keys and can't replace each other.

## Test checklist after installing
- [ ] App opens to the login screen
- [ ] Sign up → scan the QR code → enter the code → Home appears
- [ ] Log out → log in → the 6-digit code is required
- [ ] Post, view, edit, delete an item; search and filter work
- [ ] A post from another account triggers the popup and notification
