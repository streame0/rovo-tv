-- Run this in Supabase Dashboard > SQL Editor after supabase-setup.sql

-- 1. Add updated_at column
ALTER TABLE public.companion_sessions
ADD COLUMN IF NOT EXISTS updated_at bigint NOT NULL DEFAULT 0;

-- 2. Index on session_code for faster lookups (complements UNIQUE constraint)
CREATE INDEX IF NOT EXISTS idx_companion_sessions_code ON public.companion_sessions (session_code);
CREATE INDEX IF NOT EXISTS idx_companion_sessions_expires ON public.companion_sessions (expires_at);

-- 3. Auto-cleanup function (run via cron or manually)
CREATE OR REPLACE FUNCTION public.cleanup_expired_sessions()
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    deleted_count integer;
BEGIN
    DELETE FROM public.companion_sessions
    WHERE expires_at < (SELECT EXTRACT(EPOCH FROM NOW()) * 1000)
       OR (status = 'completed' AND updated_at > 0
           AND updated_at < (SELECT EXTRACT(EPOCH FROM NOW()) * 1000) - 86400000);
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$;

-- Schedule via: SELECT cron.schedule('cleanup-sessions', '0 * * * *', 'SELECT cleanup_expired_sessions();');
-- Requires pg_cron extension. If not available, run manually via Dashboard SQL Editor occasionally.

-- 4. Auto-update updated_at on change
CREATE OR REPLACE FUNCTION public.update_companion_session_timestamp()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = (SELECT EXTRACT(EPOCH FROM NOW()) * 1000);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_companion_sessions_updated_at ON public.companion_sessions;
CREATE TRIGGER trg_companion_sessions_updated_at
    BEFORE UPDATE ON public.companion_sessions
    FOR EACH ROW
    EXECUTE FUNCTION public.update_companion_session_timestamp();
