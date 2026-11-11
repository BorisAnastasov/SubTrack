import { useState } from "react";
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";
const AUTH_STORAGE_KEY = "subtrack-auth";

async function postAuth(path, payload, accessToken) {
  const response = await fetch(`${API_BASE_URL}/api/auth/${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    ...(payload ? { body: JSON.stringify(payload) } : {}),
  });

  const responseText = await response.text();
  let responseBody;
  try {
    responseBody = responseText ? JSON.parse(responseText) : null;
  } catch {
    responseBody = responseText;
  }

  if (!response.ok) {
    const message =
      responseBody?.message ||
      responseBody?.detail ||
      (typeof responseBody === "string" && responseBody) ||
      `Request failed (${response.status})`;
    throw new Error(message);
  }

  return responseBody;
}

function readStoredAuth() {
  try {
    const auth = JSON.parse(sessionStorage.getItem(AUTH_STORAGE_KEY));
    return auth?.accessToken && auth?.refreshToken ? auth : null;
  } catch {
    return null;
  }
}

function Brand({ compact = false }) {
  return (
    <Link className={`brand${compact ? " brand--compact" : ""}`} to="/">
      <span className="brand-mark" aria-hidden="true">
        <span />
        <span />
        <span />
      </span>
      <span>subtrack</span>
    </Link>
  );
}

function AuthLayout({ children, mode }) {
  return (
    <main className="auth-layout">
      <section className="story-panel" aria-label="About SubTrack">
        <Brand />
        <div className="story-copy">
          <span className="eyebrow"><span className="eyebrow-dot" /> YOUR MONEY, IN RHYTHM</span>
          <h1>Small charges.<br />A clearer picture.</h1>
          <p>Bring every recurring payment into view and make room for what matters.</p>
        </div>
        <div className="preview-card" aria-hidden="true">
          <div className="preview-top">
            <span className="preview-label">MONTHLY OVERVIEW</span>
            <span className="preview-period">THIS MONTH <span>⌄</span></span>
          </div>
          <div className="preview-total">$128<span>.50</span></div>
          <div className="preview-caption">across 6 subscriptions</div>
          <div className="preview-chart">
            {[35, 54, 42, 74, 52, 88, 63, 48, 69, 100, 72, 84].map((height, index) => (
              <span
                key={index}
                className={index === 9 ? "chart-bar chart-bar--active" : "chart-bar"}
                style={{ height: `${height}%` }}
              />
            ))}
          </div>
          <div className="preview-footer"><span>JUN</span><span>JUL</span><span>AUG</span><span>SEP</span><span>OCT</span><span>NOV</span></div>
        </div>
        <div className="story-footer"><span>Less guesswork, more control.</span><span>01 — 03</span></div>
      </section>

      <section className="form-panel">
        <div className="mobile-brand"><Brand compact /></div>
        <div className="form-wrap">
          {children}
          <p className="form-legal">Your account information stays private and secure.</p>
        </div>
        <div className="form-corner-note">{mode === "register" ? "A better view starts here." : "Welcome back to a clearer view."}</div>
      </section>
    </main>
  );
}

function AuthForm({ mode, onAuthenticated, notice }) {
  const isRegister = mode === "register";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    if (isRegister && password !== confirmPassword) {
      setError("Those passwords don’t match. Please try again.");
      return;
    }

    setIsSubmitting(true);
    try {
      const auth = await postAuth(isRegister ? "register" : "login", { email, password });
      onAuthenticated(auth);
    } catch (requestError) {
      setError(requestError.message || "We couldn’t complete your request. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout mode={mode}>
      <div className="form-heading">
        <span className="form-kicker">{isRegister ? "GET STARTED" : "YOUR ACCOUNT"}</span>
        <h2>{isRegister ? "Create your account" : "Welcome back"}</h2>
        <p>{isRegister ? "Start seeing your subscriptions in a new way." : "Sign in to pick up right where you left off."}</p>
      </div>

      {notice && <div className="form-notice" role="status">{notice}</div>}

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Email address</span>
          <input
            type="email"
            name="email"
            placeholder="you@example.com"
            autoComplete="email"
            maxLength={255}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>

        <label className="field">
          <span>Password</span>
          <input
            type="password"
            name="password"
            placeholder={isRegister ? "At least 6 characters" : "Enter your password"}
            autoComplete={isRegister ? "new-password" : "current-password"}
            minLength={isRegister ? 6 : undefined}
            maxLength={100}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>

        {isRegister && (
          <label className="field">
            <span>Confirm password</span>
            <input
              type="password"
              name="confirmPassword"
              placeholder="Enter your password again"
              autoComplete="new-password"
              minLength={6}
              maxLength={100}
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
            />
          </label>
        )}

        {error && <div className="form-error" role="alert">{error}</div>}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? "Please wait…" : isRegister ? "Create account" : "Sign in"}</span>
          {!isSubmitting && <span className="button-arrow" aria-hidden="true">↗</span>}
        </button>
      </form>

      <div className="form-switch">
        {isRegister ? "Already have an account?" : "New to SubTrack?"}{" "}
        <Link to={isRegister ? "/login" : "/register"}>
          {isRegister ? "Sign in" : "Create an account"}
        </Link>
      </div>
    </AuthLayout>
  );
}

function AccountPage({ auth, onLogout }) {
  return (
    <main className="account-page">
      <header className="account-header">
        <Brand compact />
        <span className="account-status"><span /> ACCOUNT</span>
      </header>
      <section className="account-card">
        <div className="account-icon" aria-hidden="true">{auth.email?.slice(0, 1).toUpperCase() || "S"}</div>
        <span className="form-kicker">YOU’RE SIGNED IN</span>
        <h1>Your subscriptions,<br />all in one place.</h1>
        <p className="account-email">{auth.email}</p>
        <button className="secondary-button" type="button" onClick={onLogout}>
          <span>Sign out</span><span aria-hidden="true">↗</span>
        </button>
      </section>
      <p className="account-footnote">More of your financial picture, coming into focus.</p>
    </main>
  );
}

export default function App() {
  const [auth, setAuth] = useState(readStoredAuth);
  const navigate = useNavigate();
  const location = useLocation();

  function handleAuthenticated(nextAuth) {
    sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextAuth));
    setAuth(nextAuth);
    navigate("/account", { replace: true });
  }

  async function handleLogout() {
    let logoutNotice = "You’re signed out.";
    try {
      await postAuth("logout", null, auth.accessToken);
    } catch {
      logoutNotice = "Signed out here, but the server could not confirm token revocation.";
    } finally {
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
      setAuth(null);
      navigate("/login", { replace: true, state: { notice: logoutNotice } });
    }
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate to={auth ? "/account" : "/login"} replace />} />
      <Route path="/login" element={auth ? <Navigate to="/account" replace /> : <AuthForm mode="login" onAuthenticated={handleAuthenticated} notice={location.state?.notice} />} />
      <Route path="/register" element={auth ? <Navigate to="/account" replace /> : <AuthForm mode="register" onAuthenticated={handleAuthenticated} />} />
      <Route
        path="/account"
        element={auth ? <AccountPage auth={auth} onLogout={handleLogout} /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
