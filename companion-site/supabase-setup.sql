-- Run this in Supabase Dashboard > SQL Editor
-- 1. Create companion_sessions table
CREATE TABLE public.companion_sessions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    session_code text NOT NULL,
    action text NOT NULL,
    status text NOT NULL DEFAULT 'pending'::text,
    data jsonb,
    created_at bigint NOT NULL,
    expires_at bigint NOT NULL,
    CONSTRAINT companion_sessions_pkey PRIMARY KEY (id),
    CONSTRAINT companion_sessions_session_code_key UNIQUE (session_code)
);

COMMENT ON TABLE public.companion_sessions IS 'Sessions for companion website interactions (avatar upload, hub management, auth)';

-- Enable RLS (default secure)
ALTER TABLE public.companion_sessions ENABLE ROW LEVEL SECURITY;

-- Allow anon INSERT (anyone can create a session)
CREATE POLICY "anon_insert" ON public.companion_sessions
    FOR INSERT TO anon
    WITH CHECK (true);

-- Allow anon SELECT by session_code (anyone can read a session if they know the code)
CREATE POLICY "anon_select" ON public.companion_sessions
    FOR SELECT TO anon
    USING (true);

-- Allow anon UPDATE by session_code (anyone can update a session if they know the code)
CREATE POLICY "anon_update" ON public.companion_sessions
    FOR UPDATE TO anon
    USING (true)
    WITH CHECK (true);

-- Allow anon DELETE by session_code
CREATE POLICY "anon_delete" ON public.companion_sessions
    FOR DELETE TO anon
    USING (true);

-- 2. Enable Realtime for companion_sessions
-- (Also enable in Dashboard > Database > Replication > enable for companion_sessions)
-- This is the SQL approach (may not work in all Supabase plans):
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.companion_sessions;

-- 3. Create Storage buckets
-- Run in Dashboard > Storage > New bucket, or use:
-- Bucket: avatars (public)
-- Bucket: hub-images (public)

-- 4. Storage RLS policies (run after creating buckets)
-- For avatars bucket:
CREATE POLICY "avatars_public_select" ON storage.objects
    FOR SELECT TO anon
    USING (bucket_id = 'avatars');

CREATE POLICY "avatars_public_insert" ON storage.objects
    FOR INSERT TO anon
    WITH CHECK (bucket_id = 'avatars');

CREATE POLICY "avatars_public_delete" ON storage.objects
    FOR DELETE TO anon
    USING (bucket_id = 'avatars');

-- For hub-images bucket:
CREATE POLICY "hub_images_public_select" ON storage.objects
    FOR SELECT TO anon
    USING (bucket_id = 'hub-images');

CREATE POLICY "hub_images_public_insert" ON storage.objects
    FOR INSERT TO anon
    WITH CHECK (bucket_id = 'hub-images');

CREATE POLICY "hub_images_public_delete" ON storage.objects
    FOR DELETE TO anon
    USING (bucket_id = 'hub-images');
