const SESSION_EXPIRY_MS = 10 * 60 * 1000

async function createSession(action, data = {}) {
  const code = Math.random().toString(36).substring(2, 8).toUpperCase()
  const now = Date.now()
  const { error } = await supabase.from('companion_sessions').insert({
    session_code: code,
    action: action,
    data: data,
    status: 'pending',
    created_at: now,
    expires_at: now + SESSION_EXPIRY_MS,
    updated_at: now
  })
  if (error) throw error
  return code
}

async function getSession(code) {
  const { data, error } = await supabase
    .from('companion_sessions')
    .select('*')
    .eq('session_code', code)
    .single()
  if (error) throw error
  return data
}

async function updateSession(code, updates) {
  updates.updated_at = Date.now()
  const { error } = await supabase
    .from('companion_sessions')
    .update(updates)
    .eq('session_code', code)
  if (error) throw error
}

function subscribeToSession(code, onUpdate) {
  return supabase
    .channel(`session-${code}`)
    .on('postgres_changes',
      {
        event: 'UPDATE',
        schema: 'public',
        table: 'companion_sessions',
        filter: `session_code=eq.${code}`
      },
      (payload) => onUpdate(payload.new)
    )
    .subscribe()
}

function getActionTitle(action) {
  switch (action) {
    case 'avatar': return 'Upload Avatar'
    case 'hub': return 'Manage Hub Images'
    case 'auth': return 'Cloud Sign In'
    case 'stremio': return 'Connect Stremio'
    default: return 'Rovo Companion'
  }
}

function startCountdown(expiresAt, elementId) {
  const el = document.getElementById(elementId)
  if (!el) return
  function tick() {
    const left = Math.max(0, expiresAt - Date.now())
    const sec = Math.ceil(left / 1000)
    if (sec <= 0) {
      el.textContent = 'Session expired — please go back to the TV'
      return
    }
    const m = Math.floor(sec / 60)
    const s = sec % 60
    el.textContent = `Session expires in ${m}:${s.toString().padStart(2, '0')}`
    setTimeout(tick, 1000)
  }
  tick()
}

async function compressImage(file, maxWidth = 1024, quality = 0.8) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      let w = img.width, h = img.height
      if (w > maxWidth || h > maxWidth) {
        const ratio = Math.min(maxWidth / w, maxWidth / h)
        w = Math.round(w * ratio); h = Math.round(h * ratio)
      }
      const canvas = document.createElement('canvas')
      canvas.width = w; canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, w, h)
      canvas.toBlob(blob => {
        if (blob) resolve(blob)
        else resolve(file)
      }, 'image/webp', quality)
    }
    img.onerror = () => resolve(file)
    const url = URL.createObjectURL(file)
    img.src = url
  })
}
