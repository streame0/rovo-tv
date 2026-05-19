-- Run this in Supabase Dashboard > SQL Editor
-- Creates all tables needed for TV data sync (idempotent - safe to re-run)

-- 1. watch_history
CREATE TABLE IF NOT EXISTS public.watch_history (
    id text NOT NULL,
    title text NOT NULL,
    poster text,
    background text,
    logo text,
    position bigint NOT NULL DEFAULT 0,
    duration bigint NOT NULL DEFAULT 0,
    last_watched bigint NOT NULL DEFAULT 0,
    type text NOT NULL,
    watched boolean NOT NULL DEFAULT false,
    scrobbled boolean NOT NULL DEFAULT false,
    profile_id integer NOT NULL DEFAULT 1,
    updated_at bigint NOT NULL DEFAULT 0,
    CONSTRAINT watch_history_pkey PRIMARY KEY (id, profile_id)
);

ALTER TABLE public.watch_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_read_own_watch_history" ON public.watch_history;
CREATE POLICY "users_read_own_watch_history" ON public.watch_history
    FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "users_insert_own_watch_history" ON public.watch_history;
CREATE POLICY "users_insert_own_watch_history" ON public.watch_history
    FOR INSERT TO authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "users_update_own_watch_history" ON public.watch_history;
CREATE POLICY "users_update_own_watch_history" ON public.watch_history
    FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "users_delete_own_watch_history" ON public.watch_history;
CREATE POLICY "users_delete_own_watch_history" ON public.watch_history
    FOR DELETE TO authenticated USING (true);

-- 2. watchlist
CREATE TABLE IF NOT EXISTS public.watchlist (
    id text NOT NULL,
    type text NOT NULL,
    title text NOT NULL,
    poster text,
    added_at bigint NOT NULL DEFAULT 0,
    profile_id integer NOT NULL DEFAULT 1,
    updated_at bigint NOT NULL DEFAULT 0,
    CONSTRAINT watchlist_pkey PRIMARY KEY (id, profile_id)
);

ALTER TABLE public.watchlist ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_read_own_watchlist" ON public.watchlist;
CREATE POLICY "users_read_own_watchlist" ON public.watchlist
    FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "users_insert_own_watchlist" ON public.watchlist;
CREATE POLICY "users_insert_own_watchlist" ON public.watchlist
    FOR INSERT TO authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "users_update_own_watchlist" ON public.watchlist;
CREATE POLICY "users_update_own_watchlist" ON public.watchlist
    FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "users_delete_own_watchlist" ON public.watchlist;
CREATE POLICY "users_delete_own_watchlist" ON public.watchlist
    FOR DELETE TO authenticated USING (true);

-- 3. hub_rows
CREATE TABLE IF NOT EXISTS public.hub_rows (
    id text NOT NULL,
    title text NOT NULL DEFAULT 'Hub Row',
    shape text NOT NULL,
    show_in_home boolean NOT NULL DEFAULT false,
    show_in_movies boolean NOT NULL DEFAULT false,
    show_in_series boolean NOT NULL DEFAULT false,
    home_order integer NOT NULL DEFAULT 999,
    movies_order integer NOT NULL DEFAULT 999,
    series_order integer NOT NULL DEFAULT 999,
    created_at bigint NOT NULL DEFAULT 0,
    updated_at bigint NOT NULL DEFAULT 0,
    CONSTRAINT hub_rows_pkey PRIMARY KEY (id)
);

ALTER TABLE public.hub_rows ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_all_hub_rows" ON public.hub_rows;
CREATE POLICY "users_all_hub_rows" ON public.hub_rows
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- 4. hub_row_items
CREATE TABLE IF NOT EXISTS public.hub_row_items (
    hub_row_id text NOT NULL,
    config_unique_id text NOT NULL,
    title text NOT NULL,
    custom_image_url text,
    item_order integer NOT NULL DEFAULT 0,
    updated_at bigint NOT NULL DEFAULT 0,
    CONSTRAINT hub_row_items_pkey PRIMARY KEY (hub_row_id, config_unique_id)
);

ALTER TABLE public.hub_row_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_all_hub_row_items" ON public.hub_row_items;
CREATE POLICY "users_all_hub_row_items" ON public.hub_row_items
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- 5. addons
CREATE TABLE IF NOT EXISTS public.addons (
    transport_url text NOT NULL,
    id text NOT NULL,
    name text NOT NULL,
    version text NOT NULL,
    description text,
    icon_url text,
    is_trusted boolean NOT NULL DEFAULT false,
    is_enabled boolean NOT NULL DEFAULT true,
    nickname text,
    catalogs_json text NOT NULL DEFAULT '[]',
    supports_meta boolean NOT NULL DEFAULT false,
    supports_stream boolean NOT NULL DEFAULT true,
    types_json text NOT NULL DEFAULT '[]',
    id_prefixes_json text NOT NULL DEFAULT '[]',
    sort_order integer NOT NULL DEFAULT 999,
    updated_at bigint NOT NULL DEFAULT 0,
    CONSTRAINT addons_pkey PRIMARY KEY (id)
);

ALTER TABLE public.addons ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_all_addons" ON public.addons;
CREATE POLICY "users_all_addons" ON public.addons
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- 6. series_next_up
CREATE TABLE IF NOT EXISTS public.series_next_up (
    series_id text NOT NULL,
    title text NOT NULL,
    poster text,
    next_season integer NOT NULL DEFAULT 1,
    next_episode integer NOT NULL DEFAULT 1,
    next_episode_title text,
    next_released text,
    is_complete boolean NOT NULL DEFAULT false,
    is_new_episode boolean NOT NULL DEFAULT false,
    updated_at bigint NOT NULL DEFAULT 0,
    CONSTRAINT series_next_up_pkey PRIMARY KEY (series_id)
);

ALTER TABLE public.series_next_up ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_all_series_next_up" ON public.series_next_up;
CREATE POLICY "users_all_series_next_up" ON public.series_next_up
    FOR ALL TO authenticated USING (true) WITH CHECK (true);
