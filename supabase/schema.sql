-- =====================================================================
-- FoundIt — Supabase database schema and security rules
-- =====================================================================
-- Safe to run more than once (idempotent). Works on:
--   * a brand-new Supabase project (creates everything), and
--   * an existing FoundIt project (adds the new security rules).
-- Run in: Supabase Dashboard > SQL Editor > New query > paste > Run.
--
-- Security model
--   * Every data request must come from a logged-in user who has passed
--     Google Authenticator 2FA (JWT claim aal = 'aal2'). Enforced by
--     RESTRICTIVE policies, so it applies on top of every other policy.
--   * Users can only insert/update/delete their own posts and comments.
--   * user_id, user_name and created_at are set by the server, not trusted
--     from the app.
--   * CHECK constraints repeat the app's input validation on the server.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Tables
-- ---------------------------------------------------------------------
create table if not exists public.items (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid() references auth.users(id) on delete cascade,
    name        text not null,
    description text,
    category    text,
    location    text,
    type        text not null,
    status      text not null default 'OPEN',
    image_url   text,
    created_at  timestamptz not null default now()
);

create table if not exists public.comments (
    id         uuid primary key default gen_random_uuid(),
    item_id    uuid not null references public.items(id) on delete cascade,
    user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
    user_name  text,
    text       text not null,
    is_claim   boolean not null default false,
    created_at timestamptz not null default now()
);

-- Finder contact details (added later; safe to re-run on existing projects).
-- Shown publicly on FOUND posts so the owner can reach the finder.
alter table public.items add column if not exists contact_email text;
alter table public.items add column if not exists contact_info  text;

create index if not exists items_created_at_idx on public.items (created_at desc);
create index if not exists items_user_id_idx    on public.items (user_id);
create index if not exists comments_item_id_idx on public.comments (item_id, created_at);


-- ---------------------------------------------------------------------
-- 2. Server-side validation (mirrors app/security/Validators.kt)
--    NOT VALID = enforced for new/changed rows without failing on old data.
-- ---------------------------------------------------------------------
do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'items_type_check') then
        alter table public.items add constraint items_type_check
            check (type in ('LOST', 'FOUND')) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'items_status_check') then
        alter table public.items add constraint items_status_check
            check (status in ('OPEN', 'CLAIMED')) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'items_name_len') then
        alter table public.items add constraint items_name_len
            check (char_length(btrim(name)) between 2 and 60) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'items_location_len') then
        alter table public.items add constraint items_location_len
            check (location is null or char_length(btrim(location)) between 2 and 80) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'items_description_len') then
        alter table public.items add constraint items_description_len
            check (description is null or char_length(description) <= 500) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'items_category_check') then
        alter table public.items add constraint items_category_check
            check (category is null or category in
                ('Bags', 'IDs & Cards', 'Electronics', 'Clothing', 'Books', 'Keys', 'Others')) not valid;
    end if;
    -- Contact email must be a CIIT address, and only FOUND posts may have one.
    -- ("Required on new FOUND posts" is enforced by the items_set_owner trigger,
    -- so older FOUND posts without an email can still be edited / marked claimed.)
    if not exists (select 1 from pg_constraint where conname = 'items_contact_email_check') then
        alter table public.items add constraint items_contact_email_check
            check (
                contact_email is null
                or (type = 'FOUND' and contact_email ~* '^[A-Za-z0-9._%+-]+@ciit\.edu\.ph$'
                    and char_length(contact_email) <= 254)
            ) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'items_contact_info_len') then
        alter table public.items add constraint items_contact_info_len
            check (contact_info is null or char_length(contact_info) <= 150) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'comments_text_len') then
        alter table public.comments add constraint comments_text_len
            check (char_length(btrim(text)) between 1 and 300) not valid;
    end if;
end $$;

-- Prevent duplicate records: one user cannot have two OPEN posts with the
-- same name + location + type. (Skipped with a notice if old duplicates exist.)
do $$
begin
    create unique index if not exists items_no_duplicate_open
        on public.items (user_id, lower(btrim(name)), lower(btrim(coalesce(location, ''))), type)
        where status = 'OPEN';
exception when unique_violation then
    raise notice 'Duplicate OPEN posts already exist; remove them and re-run to enable the duplicate check.';
end $$;


-- ---------------------------------------------------------------------
-- 3. Triggers: the server decides ownership and display name
-- ---------------------------------------------------------------------
create or replace function public.items_set_owner()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    if tg_op = 'INSERT' then
        new.user_id    := auth.uid();
        new.status     := 'OPEN';
        new.created_at := now();
        if new.type = 'FOUND' and coalesce(btrim(new.contact_email), '') = '' then
            raise exception 'A CIIT contact email is required when posting a found item'
                using errcode = 'check_violation';
        end if;
    else
        -- Ownership and creation time can never be changed by an update.
        new.user_id    := old.user_id;
        new.created_at := old.created_at;
    end if;
    return new;
end $$;

drop trigger if exists items_set_owner on public.items;
create trigger items_set_owner
    before insert or update on public.items
    for each row execute function public.items_set_owner();

create or replace function public.comments_set_author()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    new.user_id    := auth.uid();
    -- Display name comes from the verified login email, not from the app.
    new.user_name  := split_part(coalesce(auth.jwt() ->> 'email', 'user'), '@', 1);
    new.created_at := now();
    return new;
end $$;

drop trigger if exists comments_set_author on public.comments;
create trigger comments_set_author
    before insert on public.comments
    for each row execute function public.comments_set_author();


-- ---------------------------------------------------------------------
-- 4. Row Level Security
-- ---------------------------------------------------------------------
alter table public.items    enable row level security;
alter table public.comments enable row level security;

-- Items: any signed-in user can browse; only the owner can change their posts.
drop policy if exists "items read"       on public.items;
drop policy if exists "items insert own" on public.items;
drop policy if exists "items update own" on public.items;
drop policy if exists "items delete own" on public.items;

create policy "items read" on public.items
    for select to authenticated using (true);
create policy "items insert own" on public.items
    for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "items update own" on public.items
    for update to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);
create policy "items delete own" on public.items
    for delete to authenticated using ((select auth.uid()) = user_id);

-- Comments: anyone signed in can read and post; only the author can delete.
drop policy if exists "comments read"       on public.comments;
drop policy if exists "comments insert own" on public.comments;
drop policy if exists "comments delete own" on public.comments;

create policy "comments read" on public.comments
    for select to authenticated using (true);
create policy "comments insert own" on public.comments
    for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "comments delete own" on public.comments
    for delete to authenticated using ((select auth.uid()) = user_id);

-- 2FA REQUIRED: restrictive policies are AND-ed with the policies above,
-- so a password-only (aal1) session can read or write nothing.
drop policy if exists "items require 2fa"    on public.items;
drop policy if exists "comments require 2fa" on public.comments;

create policy "items require 2fa" on public.items
    as restrictive for all to authenticated
    using ((select auth.jwt() ->> 'aal') = 'aal2')
    with check ((select auth.jwt() ->> 'aal') = 'aal2');
create policy "comments require 2fa" on public.comments
    as restrictive for all to authenticated
    using ((select auth.jwt() ->> 'aal') = 'aal2')
    with check ((select auth.jwt() ->> 'aal') = 'aal2');


-- ---------------------------------------------------------------------
-- 5. Storage: item photos
-- ---------------------------------------------------------------------
-- Public read so photos load in the app; images only, max 5 MB.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('item-images', 'item-images', true, 5242880, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
    set public = excluded.public,
        file_size_limit = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "images read"       on storage.objects;
drop policy if exists "images upload"     on storage.objects;
drop policy if exists "images delete own" on storage.objects;

-- Photos are shown through public URLs, which do not need a SELECT policy.
-- Listing/reading objects through the API is limited to your own folder,
-- so nobody can enumerate other users' files.
create policy "images read" on storage.objects
    for select to authenticated using (
        bucket_id = 'item-images'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

-- Upload only into your own folder ("<your user id>/..."), and only after 2FA.
create policy "images upload" on storage.objects
    for insert to authenticated with check (
        bucket_id = 'item-images'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and (select auth.jwt() ->> 'aal') = 'aal2'
    );

create policy "images delete own" on storage.objects
    for delete to authenticated using (
        bucket_id = 'item-images'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and (select auth.jwt() ->> 'aal') = 'aal2'
    );


-- ---------------------------------------------------------------------
-- 6. Realtime (powers the "new lost item" notification bell)
-- ---------------------------------------------------------------------
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'items'
    ) then
        alter publication supabase_realtime add table public.items;
    end if;
end $$;
