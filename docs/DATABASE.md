# FoundIt — Database Description

**Database file:** [`supabase/schema.sql`](../supabase/schema.sql)
**Engine:** PostgreSQL, hosted by Supabase. The app talks to it over HTTPS through
Supabase's REST API (PostgREST), which turns every request into a parameterized query.

FoundIt keeps its data in the cloud, so posts are still there after the app restarts and
can be seen from any device. Only the login session is stored on the phone, encrypted with
an Android Keystore key.

---

## 1. Overview

```
auth.users (managed by Supabase Auth: email, hashed password, 2FA factors)
   │ 1
   │
   │ n
public.items ──────────────┐
   │ 1                     │ image_url → storage bucket "item-images"
   │                       │             (file path: <user id>/<random>.jpg)
   │ n                     │
public.comments            │
```

- One **user** can post many **items**.
- One **item** can have many **comments** (including ownership claims).
- Deleting a user deletes their items and comments. Deleting an item deletes its comments
  (`on delete cascade`).

Passwords and Google Authenticator secrets are **not** stored in FoundIt's tables. Supabase
Auth keeps them in the protected `auth` schema: passwords are bcrypt-hashed, and the app
can never read either one back.

---

## 2. Tables

### `public.items`: lost and found reports

| Column | Type | Rules | Description |
|---|---|---|---|
| `id` | uuid | primary key, auto-generated | Unique item ID |
| `user_id` | uuid | not null, FK → `auth.users.id`; **set by the server** | Who posted it |
| `name` | text | not null, 2–60 characters | Item name, e.g. "Blue umbrella" |
| `description` | text | optional, ≤ 500 characters | Color, brand, marks |
| `category` | text | one of: Bags, IDs & Cards, Electronics, Clothing, Books, Keys, Others | Used by the category filter |
| `location` | text | 2–80 characters | Where it was lost or found |
| `type` | text | `LOST` or `FOUND` | Used by the Lost/Found filter |
| `status` | text | `OPEN` or `CLAIMED`, default `OPEN` | Changed by the owner with "Mark as claimed" |
| `image_url` | text | optional | Public URL of the photo in storage |
| `created_at` | timestamptz | default `now()`; **set by the server** | Posting date |

**Duplicate prevention:** a unique index `items_no_duplicate_open` stops one user from
having two `OPEN` posts with the same name, location and type (case-insensitive).

### `public.comments`: comments and ownership claims

| Column | Type | Rules | Description |
|---|---|---|---|
| `id` | uuid | primary key, auto-generated | Unique comment ID |
| `item_id` | uuid | not null, FK → `items.id` (cascade delete) | Which post |
| `user_id` | uuid | not null, FK → `auth.users.id`; **set by the server** | Who wrote it |
| `user_name` | text | **set by the server** from the login email | Name shown in the app |
| `text` | text | not null, 1–300 characters | Comment text |
| `is_claim` | boolean | default `false` | `true` when sent with "This is mine — claim it" |
| `created_at` | timestamptz | default `now()` | Time posted |

---

## 3. Server-side protection

### Triggers (the server decides who owns a record)
| Trigger | Runs | What it does |
|---|---|---|
| `items_set_owner` | before insert/update on `items` | On insert: sets `user_id` to the logged-in user, `status` to `OPEN` and `created_at` to now. On update: ownership and creation date can never change. |
| `comments_set_author` | before insert on `comments` | Sets `user_id` and `user_name` from the verified login, so names can't be faked. |

### CHECK constraints (input validation, repeated on the server)
`items_type_check`, `items_status_check`, `items_name_len`, `items_location_len`,
`items_description_len`, `items_category_check`, `comments_text_len`.
These use the same limits as `app/.../security/Validators.kt`, so a request crafted outside
the app is still rejected.

### Row Level Security policies
RLS is **enabled** on both tables. Every request is checked against these rules:

| Table | Policy | Action | Rule |
|---|---|---|---|
| items | items read | SELECT | any signed-in user |
| items | items insert own | INSERT | `user_id` must be you |
| items | items update own | UPDATE | only your own posts |
| items | items delete own | DELETE | only your own posts |
| items | **items require 2fa** (restrictive) | ALL | session must have passed Google Authenticator (`aal2`) |
| comments | comments read | SELECT | any signed-in user |
| comments | comments insert own | INSERT | `user_id` must be you |
| comments | comments delete own | DELETE | only your own comments |
| comments | **comments require 2fa** (restrictive) | ALL | session must be `aal2` |

A *restrictive* policy is combined with AND on top of the other rules. A user who has only
typed their password (no authenticator code yet) therefore can't read or change anything,
even by calling the API directly.

### Storage bucket `item-images`
- Public read through the image URL (so photos load in the app). Images only (JPEG, PNG,
  WebP), max 5 MB.
- **Upload / delete:** only into your own folder (`<your user id>/...`) and only after 2FA.
- **Listing files through the API:** only your own folder.

### Realtime
The `items` table is in the `supabase_realtime` publication. The app is notified when a new
LOST item is posted and shows a notification (the bell icon). Realtime still follows the RLS
rules above.
