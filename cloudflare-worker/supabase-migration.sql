-- =============================================================================
-- Rovo Cloud Sync — Supabase SQL Migration
-- Run this in your Supabase SQL Editor (https://supabase.com/dashboard)
-- =============================================================================
-- How to use:
-- 1. Create a Supabase project at https://supabase.com
-- 2. Go to SQL Editor → New Query
-- 3. Paste and run this entire script
-- 4. Copy your Supabase URL and anon key from Settings → API
-- 5. Add them to your local.properties:
--      SUPABASE_URL=https://your-project.supabase.co
--      SUPABASE_ANON_KEY=your-anon-key-here
-- =============================================================================

-- Enable Row Level Security (RLS) for all tables
-- Each user can only access their own data via the user_id column

-- ── 1. Watch History ──
CREATE TABLE IF NOT EXISTS watch_history (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  poster TEXT,
  background TEXT,
  logo TEXT,
  position BIGINT NOT NULL DEFAULT 0,
  duration BIGINT NOT NULL DEFAULT 0,
  last_watched BIGINT NOT NULL,
  type TEXT NOT NULL,
  watched BOOLEAN DEFAULT false,
  scrobbled BOOLEAN DEFAULT false,
  profile_id INTEGER DEFAULT 1,
  updated_at BIGINT NOT NULL,
  user_id UUID REFERENCES auth.users(id) NOT NULL
);

ALTER TABLE watch_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read their own watch_history"
  ON watch_history FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own watch_history"
  ON watch_history FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own watch_history"
  ON watch_history FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own watch_history"
  ON watch_history FOR DELETE USING (auth.uid() = user_id);

-- ── 2. Watchlist ──
CREATE TABLE IF NOT EXISTS watchlist (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  title TEXT NOT NULL,
  poster TEXT,
  added_at BIGINT NOT NULL,
  profile_id INTEGER DEFAULT 1,
  updated_at BIGINT NOT NULL,
  user_id UUID REFERENCES auth.users(id) NOT NULL
);

ALTER TABLE watchlist ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read their own watchlist"
  ON watchlist FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own watchlist"
  ON watchlist FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own watchlist"
  ON watchlist FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own watchlist"
  ON watchlist FOR DELETE USING (auth.uid() = user_id);

-- ── 3. Hub Rows ──
CREATE TABLE IF NOT EXISTS hub_rows (
  id TEXT PRIMARY KEY,
  title TEXT DEFAULT 'Hub Row',
  shape TEXT NOT NULL,
  show_in_home BOOLEAN DEFAULT false,
  show_in_movies BOOLEAN DEFAULT false,
  show_in_series BOOLEAN DEFAULT false,
  home_order INTEGER DEFAULT 999,
  movies_order INTEGER DEFAULT 999,
  series_order INTEGER DEFAULT 999,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  user_id UUID REFERENCES auth.users(id) NOT NULL
);

ALTER TABLE hub_rows ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read their own hub_rows"
  ON hub_rows FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own hub_rows"
  ON hub_rows FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own hub_rows"
  ON hub_rows FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own hub_rows"
  ON hub_rows FOR DELETE USING (auth.uid() = user_id);

-- ── 4. Hub Row Items ──
CREATE TABLE IF NOT EXISTS hub_row_items (
  hub_row_id TEXT NOT NULL,
  config_unique_id TEXT NOT NULL,
  title TEXT NOT NULL,
  custom_image_url TEXT,
  item_order INTEGER DEFAULT 0,
  updated_at BIGINT NOT NULL,
  user_id UUID REFERENCES auth.users(id) NOT NULL,
  PRIMARY KEY (hub_row_id, config_unique_id)
);

ALTER TABLE hub_row_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read their own hub_row_items"
  ON hub_row_items FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own hub_row_items"
  ON hub_row_items FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own hub_row_items"
  ON hub_row_items FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own hub_row_items"
  ON hub_row_items FOR DELETE USING (auth.uid() = user_id);

-- ── 5. Addons ──
CREATE TABLE IF NOT EXISTS addons (
  transport_url TEXT PRIMARY KEY,
  id TEXT NOT NULL,
  name TEXT NOT NULL,
  version TEXT NOT NULL,
  description TEXT,
  icon_url TEXT,
  is_trusted BOOLEAN DEFAULT false,
  is_enabled BOOLEAN DEFAULT true,
  nickname TEXT,
  catalogs_json TEXT DEFAULT '[]',
  supports_meta BOOLEAN DEFAULT false,
  supports_stream BOOLEAN DEFAULT true,
  types_json TEXT DEFAULT '[]',
  id_prefixes_json TEXT DEFAULT '[]',
  sort_order INTEGER DEFAULT 999,
  updated_at BIGINT NOT NULL,
  user_id UUID REFERENCES auth.users(id) NOT NULL
);

ALTER TABLE addons ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read their own addons"
  ON addons FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own addons"
  ON addons FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own addons"
  ON addons FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own addons"
  ON addons FOR DELETE USING (auth.uid() = user_id);

-- ── 6. Series Next Up ──
CREATE TABLE IF NOT EXISTS series_next_up (
  series_id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  poster TEXT,
  next_season INTEGER NOT NULL,
  next_episode INTEGER NOT NULL,
  next_episode_title TEXT,
  next_released TEXT,
  is_complete BOOLEAN DEFAULT false,
  is_new_episode BOOLEAN DEFAULT false,
  updated_at BIGINT NOT NULL,
  user_id UUID REFERENCES auth.users(id) NOT NULL
);

ALTER TABLE series_next_up ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read their own series_next_up"
  ON series_next_up FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own series_next_up"
  ON series_next_up FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own series_next_up"
  ON series_next_up FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own series_next_up"
  ON series_next_up FOR DELETE USING (auth.uid() = user_id);

-- ── Indexes for performance ──
CREATE INDEX IF NOT EXISTS idx_watch_history_user_id ON watch_history(user_id);
CREATE INDEX IF NOT EXISTS idx_watch_history_updated ON watch_history(updated_at);
CREATE INDEX IF NOT EXISTS idx_watchlist_user_id ON watchlist(user_id);
CREATE INDEX IF NOT EXISTS idx_hub_rows_user_id ON hub_rows(user_id);
CREATE INDEX IF NOT EXISTS idx_hub_row_items_user_id ON hub_row_items(user_id);
CREATE INDEX IF NOT EXISTS idx_addons_user_id ON addons(user_id);
CREATE INDEX IF NOT EXISTS idx_series_next_up_user_id ON series_next_up(user_id);

-- ── Enable email + anonymous auth in Supabase Dashboard:
-- Settings → Authentication → Providers
-- ☑ Email (enable "Confirm email" if desired — set to off for auto-confirm to skip verification)
-- ☑ Anonymous (enable for instant sync without sign-up)
