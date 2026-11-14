# FoundIt: Security, 2FA and UI Rewrite — Design

## Intent
Bring FoundIt (campus lost & found, Kotlin + Compose + Supabase) in line with the course
project instructions (Weeks 10–11): mandatory Google Authenticator 2FA, secure storage,
backend authorization, input validation, safe errors, and a clean, easy-to-navigate UI.

## Goals
- Google Authenticator (TOTP) 2FA on sign-up and login, verified server-side.
- Backend enforcement: RLS requires an `aal2` (2FA-passed) session for all data access.
- Encrypted local session (Android Keystore AES-GCM), cleared on logout.
- Field validation in the app plus matching DB constraints; duplicate open posts blocked.
- Generic, non-leaking error messages; no logging of secrets.
- Bottom-nav UI, 8 screens, consistent theme.

## Non-goals
- Pushing to GitHub (the user pushes manually).
- Test docs, release signing, DB description, Supabase setup guide — asked about after code.

## Approaches (2FA)
1. **Supabase built-in MFA TOTP (chosen):** Supabase verifies codes; RLS can require `aal2`.
2. Lecture-style app-side TOTP with secret in a DB column: closer to slides but client
   verifies itself and the secret must be readable by the client. Rejected as weaker.

## Decisions
- Auth flow: Login/Register → (no TOTP factor) 2FA Setup (ZXing QR + secret text + code)
  or (factor exists) 2FA Verify → Main tabs. App start with aal1 session → Verify.
- Session storage: custom `SessionManager` encrypting session JSON with a Keystore AES-GCM key.
- Config: `SUPABASE_URL` / `SUPABASE_KEY` read from `local.properties` into `BuildConfig`.
- Manifest: `allowBackup=false`, network security config with cleartext disabled.
- DB (`supabase/security_update.sql`): restrictive aal2 policies on items, comments, storage;
  upload only into own folder; CHECK constraints on type/status/lengths; trigger sets
  `comments.user_name` server-side; unique partial index for duplicate OPEN posts.
- Validation: email format; password ≥8 with letter+digit; name 2–60; location 2–80;
  description ≤500; comment 1–300; category/type required.
- UI: tabs Home / Post / My Reports / Account; Detail and Edit with back arrow.
- Code layout: `data/`, `security/`, `notify/`, `ui/screens`, `ui/components`, `ui/theme`.
- `.gitignore` cleaned (junk lines ignored `app/`, `gradle/` etc.).

## Left to the builder
Exact colors/typography, component structure, friendly error wording.
