# FoundIt — Setup Guide (Your Own Supabase Database)

This guide lets anyone run FoundIt against **their own** Supabase project, using the same
database structure and security rules as the original. Everything is free-tier friendly.
Allow about 15 minutes.

> **What you get:** an empty copy of the FoundIt database: the same tables, rules, photo
> storage and 2FA requirement. Posts and accounts are not copied (they belong to real users
> of the original project). You create your own by using the app.

---

## What you need

- **Android Studio** (latest stable), with an Android SDK installed
- An **Android phone or emulator** running Android 8.0 (API 26) or newer
- The **Google Authenticator** app on a phone
  ([Play Store](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2) /
  App Store)
- A free **Supabase** account: https://supabase.com
- This project's source code

---

## Step 1: Create a Supabase project

1. Sign in at https://supabase.com/dashboard and click **New project**.
2. Choose any name (e.g. `foundit`), set a database password (save it somewhere safe), pick
   the region closest to you, and click **Create new project**.
3. Wait 1–2 minutes until the project finishes setting up.

## Step 2: Create the database

1. In the left sidebar, open **SQL Editor** → **New query**.
2. Open the file `supabase/schema.sql` from this project, copy **all** of it, and paste it
   into the editor.
3. Click **Run**. You should see *"Success. No rows returned"*.
4. Check it worked:
   - **Table Editor** shows two tables: `items` and `comments`.
   - **Storage** shows a bucket named `item-images`.
   - **Authentication → Policies** lists the policies for `items` and `comments`, including
     *"items require 2fa"*.

The script is safe to run again. If you change something, just run it once more.

## Step 3: Configure sign-in

1. Go to **Authentication → Sign In / Providers** (called *Providers* in some versions) and
   make sure **Email** is enabled.
2. **Confirm email** (same page):
   - **On** (default): new users must click a link in their email before logging in.
   - **Off**: faster for class demos and testing. Users can log in right after signing up.
3. Go to **Authentication → Multi-Factor** (sometimes under *Sign In / Providers → Multi-Factor
   Authentication*) and make sure **TOTP (App Authenticator)** is **enabled**. It is on by
   default.

## Step 4: Get your project URL and key

1. Go to **Project Settings** (gear icon) → **API Keys** (or **Data API** for the URL).
2. Copy:
   - **Project URL**: looks like `https://abcdefghijklmnop.supabase.co`
   - **Publishable key** (`sb_publishable_...`). Older projects call this the
     **anon public** key (starts with `eyJ...`).

> ⚠️ **Never use the `service_role` or "secret" key in the app.** That key bypasses
> all security rules. The publishable/anon key is safe in an app because the database rules
> from Step 2 protect the data.

## Step 5: Connect the app to your project

1. Open the `FoundIt` folder in Android Studio and let Gradle sync once. If the sync fails
   because of missing Supabase values, that's expected: continue with the next step.
2. In the project's **root folder** (next to `settings.gradle.kts`), open or create the file
   **`local.properties`** and add these lines with your own values:

   ```properties
   sdk.dir=C\:/Users/YOUR_NAME/AppData/Local/Android/Sdk
   SUPABASE_URL=https://abcdefghijklmnop.supabase.co
   SUPABASE_KEY=sb_publishable_xxxxxxxxxxxxxxxxxxxx
   ```

   - `sdk.dir` is usually filled in automatically by Android Studio. On macOS it looks like
     `/Users/YOUR_NAME/Library/Android/sdk`.
   - On Windows, write the colon after the drive letter as `\:` exactly as shown.
   - `local.properties` is listed in `.gitignore`, so your key is not pushed to GitHub.
3. Click **File → Sync Project with Gradle Files**.

## Step 6: Run the app and create an account

1. Choose your phone or emulator and click **Run ▶**.
2. On the login screen, tap **Create account**, then enter an email and a password (at least
   8 characters, with a letter and a number).
   - If **Confirm email** is on, open the email Supabase sends, click the link, then come
     back and **Log in**.
3. **Set up two-step verification:**
   1. Open **Google Authenticator** → **+** → **Scan a QR code**, and scan the code on the
      screen. (On an emulator, use **Enter a setup key** and type the key shown under the
      QR code.)
   2. Type the 6-digit code from Google Authenticator and tap **Verify and turn on**.
4. You're in. Try reporting an item from the **Post** tab.

From now on, logging in asks for your password **and** the current 6-digit code.

## Step 7 (optional): Check that the security works

| Check | How | Expected |
|---|---|---|
| Data is protected without 2FA | Log in with your password, then stop at the code screen | No items are loaded; the main screens are unreachable |
| Users can't change others' posts | Create two accounts; open account A's post while logged in as B | No Edit/Delete buttons; the server also rejects the change |
| Passwords aren't readable | Dashboard → **Authentication → Users** | Only emails are shown; passwords are stored hashed |
| HTTPS only | `network_security_config.xml` | `cleartextTrafficPermitted="false"` |

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Build error: *Missing SUPABASE_URL in local.properties* | Do Step 5, then sync Gradle again. |
| *"Invalid email or password"* right after sign-up | Confirm your email first (Step 3), or turn off **Confirm email**. |
| *"Invalid or expired code"* | Make sure the phone's date and time are set to **automatic**. Codes change every 30 seconds, so enter the current one. |
| Lost the phone with Google Authenticator | Dashboard → **Authentication → Users** → select the user → delete their MFA factor (or the user). They'll be asked to set up 2FA again at the next login. |
| *"You are not allowed to do that"* / empty lists | The SQL from Step 2 wasn't run (or failed). Run `supabase/schema.sql` again and check for errors. |
| Photos don't upload | Check that the `item-images` bucket exists (Step 2). Photos must be JPEG/PNG/WebP under 5 MB. |
| No "new lost item" notifications | Allow notifications when the app asks. Notifications only appear for items posted by *other* users. |
| *"Can't connect"* on startup | Check the internet connection and that `SUPABASE_URL` is correct. |
