# FoundIt Security, 2FA and UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Supabase TOTP 2FA, encrypted session storage, backend enforcement, validation and a bottom-nav UI to FoundIt.

**Architecture:** Compose single-activity app; `Repository` wraps supabase-kt; `AppViewModel` holds state; auth state machine routes Login → 2FA Setup/Verify → Main tabs. RLS requires `aal2`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, supabase-kt 3.0.3 (auth MFA, postgrest, storage, realtime), ZXing core, Android Keystore.

**Spec:** `docs/superpowers/specs/2026-11-15-foundit-security-2fa-ui-design.md`

## Global Constraints
- minSdk 26, compileSdk/targetSdk 37; no new permissions beyond INTERNET and POST_NOTIFICATIONS.
- Supabase URL/key only from `local.properties` (`SUPABASE_URL`, `SUPABASE_KEY`) via BuildConfig.
- No `Log.*` of credentials, tokens, OTPs or emails. User-facing errors never show raw exception text.
- Validation limits: email format; password ≥8 with letter + digit; name 2–60; location 2–80; description ≤500; comment 1–300; OTP exactly 6 digits.

## Review Focus
- OTP with spaces/letters → rejected before network call (Validators test).
- Session restored with aal1 → must land on Verify, never Main.
- Keystore decrypt failure (key wiped) → session treated as absent, no crash.
- Sign-up when email confirmation is on (no session returned) → message to confirm email, no crash.
- Editing an item while keeping old values → duplicate check must not flag the item itself.

---

### Task 1: Project hygiene & config
**Files:** `.gitignore`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `res/xml/network_security_config.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`, `SupabaseProvider.kt`
- [ ] Remove junk lines from `.gitignore`.
- [ ] Add `buildConfig = true`; read `SUPABASE_URL`/`SUPABASE_KEY` from local.properties into BuildConfig; add `com.google.zxing:core:3.5.3`.
- [ ] Manifest: `allowBackup=false`, `networkSecurityConfig`; config with `cleartextTrafficPermitted="false"`.
- [ ] Compile: `./gradlew :app:compileDebugKotlin` → success.

### Task 2: Security utilities (TDD)
**Files:** `security/Validators.kt`, `security/SecureSessionManager.kt`, `security/ErrorMapper.kt`, test `app/src/test/.../ValidatorsTest.kt`
**Produces:** `Validators.email(String): String?`, `password(String)`, `confirm(String,String)`, `itemName`, `location`, `description`, `comment`, `otp` — each returns error text or null. `SecureSessionManager(context): SessionManager`. `friendlyError(Throwable): String`.
- [ ] Tests: blank email → "Email is required"; "a@b" → invalid; "abc12345" ok, "abcdefgh" → needs number; otp "12 345" → invalid, "123456" → null; comment of 301 chars → error.
- [ ] Run `./gradlew :app:testDebugUnitTest` → FAIL, implement, → PASS.

### Task 3: Supabase security SQL
**Files:** `supabase/security_update.sql`
- [ ] Restrictive aal2 policies on items, comments, storage.objects (item-images); upload own-folder; CHECK constraints; user_name trigger; unique partial index on open posts.

### Task 4: Repository + ViewModel auth/2FA
**Files:** `data/Repository.kt`, `ui/AppViewModel.kt`
**Produces:** `AuthStage { Loading, LoggedOut, NeedsEnroll, NeedsVerify, Ready }`; `vm.stage`; `vm.startEnroll()` exposing `enrollUri`, `enrollSecret`; `vm.verifyCode(code)`; `vm.logout()`.

### Task 5: UI rewrite
**Files:** `ui/theme/Theme.kt`, `ui/components/*.kt`, `ui/screens/{Auth,TwoFactorSetup,TwoFactorVerify,Home,Detail,PostItem,MyReports,Account}Screen.kt`, `MainActivity.kt`
- [ ] Bottom nav tabs Home/Post/My Reports/Account; confirmation dialogs for delete, claim, logout, discard; snackbar confirmations after saves.

### Task 6: Verify
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL.
