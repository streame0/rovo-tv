const SUPABASE_URL = 'https://pxbmknxpcrpupwocmsyf.supabase.co'
const SUPABASE_ANON_KEY = 'sb_publishable_mO364FVfYuRxykXUdUhbWQ_8AK7tBJ7'

const supabase = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
  realtime: {
    params: {
      eventsPerSecond: 10
    }
  }
})
