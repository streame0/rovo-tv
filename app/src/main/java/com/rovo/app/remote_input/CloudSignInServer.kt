package com.rovo.app.remote_input

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import java.util.UUID

/**
 * HTTP server that serves a mobile-friendly sign-in/sign-up form
 * for Supabase cloud accounts. The user scans a QR code on their TV,
 * opens the URL on their phone, and enters email + password.
 *
 * On success, the server forwards credentials back to the app.
 */
class CloudSignInServer(
    port: Int,
    private val onSignIn: (email: String, password: String) -> Unit,
    private val onSignUp: (email: String, password: String) -> Unit
) : NanoHTTPD(port) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val csrfToken = UUID.randomUUID().toString()

    override fun serve(session: IHTTPSession): Response {
        if (session.uri == "/ping") return DisconnectBanner.pingResponse()
        return when {
            session.method == Method.GET && session.uri == "/" -> serveForm()
            session.method == Method.POST && session.uri == "/signin" -> handleSignIn(session)
            session.method == Method.POST && session.uri == "/signup" -> handleSignUp(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun serveForm(): Response {
        val html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Rovo Cloud Sync</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background-color: #121212;
                    color: #ffffff;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 20px;
                }
                .container {
                    background-color: #1e1e1e;
                    border-radius: 16px;
                    padding: 32px 24px;
                    width: 100%;
                    max-width: 400px;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.3);
                }
                .logo {
                    text-align: center;
                    margin-bottom: 16px;
                }
                .logo span { font-size: 48px; }
                h1 {
                    color: #fff;
                    font-size: 1.4rem;
                    font-weight: 600;
                    margin-bottom: 4px;
                    text-align: center;
                }
                p {
                    color: #aaaaaa;
                    font-size: 14px;
                    text-align: center;
                    margin-bottom: 24px;
                }
                input {
                    width: 100%;
                    padding: 16px;
                    font-size: 16px;
                    border: 2px solid #333;
                    border-radius: 12px;
                    background: rgba(0, 0, 0, 0.3);
                    color: #fff;
                    outline: none;
                    transition: border-color 0.2s;
                    margin-bottom: 12px;
                }
                input:focus { border-color: #555; }
                input::placeholder { color: rgba(255, 255, 255, 0.4); }
                button {
                    width: 100%;
                    padding: 14px 24px;
                    font-size: 1rem;
                    font-weight: 600;
                    border: none;
                    border-radius: 24px;
                    cursor: pointer;
                    transition: transform 0.1s, opacity 0.2s;
                    margin-top: 4px;
                }
                button:active { transform: scale(0.98); }
                button:disabled { opacity: 0.6; cursor: not-allowed; }
                .btn-primary {
                    background-color: #ffffff;
                    color: #000000;
                }
                .btn-secondary {
                    background-color: transparent;
                    color: #888;
                    border: 1px solid #333;
                    margin-top: 8px;
                }
                .success {
                    text-align: center;
                    color: #10b981;
                    padding: 40px 0;
                }
                .success svg { width: 64px; height: 64px; margin-bottom: 16px; }
                .error {
                    background: rgba(239, 68, 68, 0.15);
                    border-radius: 8px;
                    padding: 12px;
                    color: #ef4444;
                    font-size: 14px;
                    text-align: center;
                    margin-bottom: 16px;
                    display: none;
                }
                .hidden { display: none; }
                .spinner {
                    width: 20px; height: 20px;
                    border: 2px solid rgba(0,0,0,0.3);
                    border-top-color: #000;
                    border-radius: 50%;
                    animation: spin 0.8s linear infinite;
                    display: inline-block;
                    margin-right: 8px;
                    vertical-align: middle;
                }
                @keyframes spin { to { transform: rotate(360deg); } }
                .tabs {
                    display: flex;
                    margin-bottom: 20px;
                    border-bottom: 1px solid #333;
                }
                .tab {
                    flex: 1;
                    padding: 12px;
                    text-align: center;
                    color: #888;
                    cursor: pointer;
                    font-weight: 500;
                    transition: color 0.2s, border-color 0.2s;
                    border-bottom: 2px solid transparent;
                }
                .tab.active {
                    color: #fff;
                    border-bottom-color: #fff;
                }
                .info {
                    background: rgba(255,255,255,0.05);
                    border-radius: 8px;
                    padding: 12px;
                    margin-top: 20px;
                }
                .info p { margin: 0; font-size: 13px; color: rgba(255,255,255,0.5); }
            </style>
        </head>
        <body>
            <div class="container">
                <div id="form-container">
                    <div class="logo"><span>☁️</span></div>
                    <h1>Rovo Cloud Sync</h1>
                    <p>Sign in to sync your data across devices</p>

                    <div class="tabs">
                        <div class="tab active" id="tab-signin" onclick="switchTab('signin')">Sign In</div>
                        <div class="tab" id="tab-signup" onclick="switchTab('signup')">Create Account</div>
                    </div>

                    <div class="error" id="error-msg"></div>

                    <form id="signin-form">
                        <input type="hidden" name="csrf_token" value="$csrfToken">
                        <input type="email" name="email" placeholder="Email" autocomplete="email" required>
                        <input type="password" name="password" placeholder="Password" autocomplete="current-password" required>
                        <button type="submit" class="btn-primary">Sign In</button>
                    </form>

                    <form id="signup-form" class="hidden">
                        <input type="hidden" name="csrf_token" value="$csrfToken">
                        <input type="email" name="email" placeholder="Email" autocomplete="email" required>
                        <input type="password" name="password" placeholder="Password" autocomplete="new-password" required>
                        <input type="password" name="confirm" placeholder="Confirm Password" autocomplete="new-password" required>
                        <button type="submit" class="btn-primary">Create Account</button>
                    </form>

                    <div class="info">
                        <p>🔒 Credentials are sent directly to your TV over your local network.</p>
                    </div>
                </div>

                <div class="success hidden" id="success-container">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                        <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                    <h1>Connected!</h1>
                    <p style="margin-top: 12px;">Your cloud account is now linked.<br>Check your TV to start syncing.</p>
                </div>
            </div>

            <script>
                function switchTab(tab) {
                    document.getElementById('tab-signin').classList.toggle('active', tab === 'signin');
                    document.getElementById('tab-signup').classList.toggle('active', tab === 'signup');
                    document.getElementById('signin-form').classList.toggle('hidden', tab !== 'signin');
                    document.getElementById('signup-form').classList.toggle('hidden', tab !== 'signup');
                    document.getElementById('error-msg').style.display = 'none';
                }

                document.getElementById('signin-form').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const btn = e.target.querySelector('button');
                    const errorEl = document.getElementById('error-msg');
                    btn.disabled = true;
                    btn.innerHTML = '<span class="spinner"></span>Signing in...';
                    errorEl.style.display = 'none';
                    try {
                        const formData = new FormData(e.target);
                        const resp = await fetch('/signin', { method: 'POST', body: formData });
                        const result = await resp.json();
                        if (result.success) {
                            document.getElementById('form-container').classList.add('hidden');
                            document.getElementById('success-container').classList.remove('hidden');
                        } else {
                            errorEl.textContent = result.error || 'Sign in failed';
                            errorEl.style.display = 'block';
                            btn.disabled = false;
                            btn.innerHTML = 'Sign In';
                        }
                    } catch (err) {
                        errorEl.textContent = 'Network error. Please try again.';
                        errorEl.style.display = 'block';
                        btn.disabled = false;
                        btn.innerHTML = 'Sign In';
                    }
                });

                document.getElementById('signup-form').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const btn = e.target.querySelector('button');
                    const errorEl = document.getElementById('error-msg');
                    const pw = e.target.querySelector('input[name="password"]').value;
                    const confirm = e.target.querySelector('input[name="confirm"]').value;
                    if (pw !== confirm) {
                        errorEl.textContent = 'Passwords do not match';
                        errorEl.style.display = 'block';
                        return;
                    }
                    if (pw.length < 6) {
                        errorEl.textContent = 'Password must be at least 6 characters';
                        errorEl.style.display = 'block';
                        return;
                    }
                    btn.disabled = true;
                    btn.innerHTML = '<span class="spinner"></span>Creating...';
                    errorEl.style.display = 'none';
                    try {
                        const formData = new FormData(e.target);
                        const resp = await fetch('/signup', { method: 'POST', body: formData });
                        const result = await resp.json();
                        if (result.success) {
                            document.getElementById('form-container').classList.add('hidden');
                            document.getElementById('success-container').classList.remove('hidden');
                        } else {
                            errorEl.textContent = result.error || 'Sign up failed';
                            errorEl.style.display = 'block';
                            btn.disabled = false;
                            btn.innerHTML = 'Create Account';
                        }
                    } catch (err) {
                        errorEl.textContent = 'Network error. Please try again.';
                        errorEl.style.display = 'block';
                        btn.disabled = false;
                        btn.innerHTML = 'Create Account';
                    }
                });
            </script>
            ${DisconnectBanner.htmlSnippet}
        </body>
        </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun handleSignIn(session: IHTTPSession): Response {
        try {
            val files = mutableMapOf<String, String>()
            session.parseBody(files)
            if (session.parms["csrf_token"] != csrfToken) {
                return jsonResponse(false, "Invalid request")
            }
            val email = session.parms["email"]
            val password = session.parms["password"]
            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                return jsonResponse(false, "Email and password required")
            }
            mainHandler.post { onSignIn(email, password) }
            return jsonResponse(true, null)
        } catch (e: Exception) {
            return jsonResponse(false, "Server error")
        }
    }

    private fun handleSignUp(session: IHTTPSession): Response {
        try {
            val files = mutableMapOf<String, String>()
            session.parseBody(files)
            if (session.parms["csrf_token"] != csrfToken) {
                return jsonResponse(false, "Invalid request")
            }
            val email = session.parms["email"]
            val password = session.parms["password"]
            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                return jsonResponse(false, "Email and password required")
            }
            if (password.length < 6) {
                return jsonResponse(false, "Password must be at least 6 characters")
            }
            mainHandler.post { onSignUp(email, password) }
            return jsonResponse(true, null)
        } catch (e: Exception) {
            return jsonResponse(false, "Server error")
        }
    }

    private fun jsonResponse(success: Boolean, error: String?): Response {
        val json = JsonObject().apply {
            addProperty("success", success)
            if (error != null) addProperty("error", error)
        }.toString()
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }
}
