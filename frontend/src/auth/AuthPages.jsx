import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { postAuth } from "../api.js";
import { Brand } from "../components/Brand.jsx";

export function AuthLayout({ children, mode }) {
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
export function AuthForm({ mode, onAuthenticated, notice }) {
  const isRegister = mode === "register";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [hasLoginFailed, setHasLoginFailed] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (isRegister && password !== confirmPassword) {
      setError("Those passwords don’t match. Please try again.");
      return;
    }

    setIsSubmitting(true);
    try {
      if (isRegister) {
        const message = await postAuth("register", { email, password });
        setSuccess(typeof message === "string" ? message : "Check your email for a link to verify your account.");
        setPassword("");
        setConfirmPassword("");
      } else {
        const auth = await postAuth("login", { email, password });
        onAuthenticated(auth);
      }
    } catch (requestError) {
      if (!isRegister) {
        setHasLoginFailed(true);
      }
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
      {success && <div className="form-notice" role="status">{success}</div>}

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

      {!isRegister && hasLoginFailed && (
        <div className="form-switch form-forgot">
          <Link to="/forgot-password">Forgot your password?</Link>
        </div>
      )}

      <div className="form-switch">
        {isRegister ? "Already have an account?" : "New to SubTrack?"}{" "}
        <Link to={isRegister ? "/login" : "/register"}>
          {isRegister ? "Sign in" : "Create an account"}
        </Link>
      </div>
    </AuthLayout>
  );
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    setIsSubmitting(true);
    try {
      const response = await postAuth("forgot-password", { email });
      setMessage(typeof response === "string"
        ? response
        : "If an account exists for this email, a password reset link will be sent.");
      setEmail("");
    } catch (requestError) {
      setError(requestError.message || "We couldn’t send a reset link. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout mode="login">
      <div className="form-heading">
        <span className="form-kicker">PASSWORD RESET</span>
        <h2>Forgot your password?</h2>
        <p>Enter your email and we’ll send you a secure reset link.</p>
      </div>
      {message && <div className="form-notice" role="status">{message}</div>}
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
        {error && <div className="form-error" role="alert">{error}</div>}
        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? "Please wait…" : "Send reset link"}</span>
        </button>
      </form>
      <div className="form-switch"><Link to="/login">Back to sign in</Link></div>
    </AuthLayout>
  );
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    if (password !== confirmPassword) {
      setError("Those passwords don’t match. Please try again.");
      return;
    }

    const token = searchParams.get("token");
    if (!token) {
      setError("This reset link is missing its token.");
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await postAuth("reset-password", { token, password });
      setMessage(typeof response === "string"
        ? response
        : "Password reset successfully. You can now sign in.");
      setPassword("");
      setConfirmPassword("");
    } catch (requestError) {
      setError(requestError.message || "We couldn’t reset your password. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout mode="login">
      <div className="form-heading">
        <span className="form-kicker">PASSWORD RESET</span>
        <h2>Choose a new password</h2>
        <p>Enter and confirm your new password.</p>
      </div>
      {message && <div className="form-notice" role="status">{message}</div>}
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>New password</span>
          <input
            type="password"
            name="password"
            placeholder="At least 6 characters"
            autoComplete="new-password"
            minLength={6}
            maxLength={100}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        <label className="field">
          <span>Confirm new password</span>
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
        {error && <div className="form-error" role="alert">{error}</div>}
        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? "Please wait…" : "Reset password"}</span>
        </button>
      </form>
      <div className="form-switch"><Link to="/login">Back to sign in</Link></div>
    </AuthLayout>
  );
}

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState({ loading: true, success: false, message: "" });
  const [email, setEmail] = useState("");
  const [resending, setResending] = useState(false);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    const token = searchParams.get("token");
    if (!token) {
      setStatus({ loading: false, success: false, message: "This verification link is missing its token." });
      return;
    }

    postAuth("verify-email", { token })
      .then((message) => setStatus({
        loading: false,
        success: true,
        message: typeof message === "string" ? message : "Your email is verified. You can now sign in.",
      }))
      .catch((requestError) => setStatus({
        loading: false,
        success: false,
        message: requestError.message || "We couldn't verify your email. Please request a new link.",
      }));
  }, [searchParams]);

  return (
    <AuthLayout mode="login">
      <div className="form-heading">
        <span className="form-kicker">EMAIL VERIFICATION</span>
        <h2>{status.loading ? "Verifying your email…" : "Verification update"}</h2>
        {!status.loading && <p role="status">{status.message}</p>}
      </div>
      {!status.loading && !status.success && (
        <form
          className="auth-form"
          onSubmit={async (event) => {
            event.preventDefault();
            setResending(true);
            try {
              const message = await postAuth("resend-verification", { email });
              setStatus({ loading: false, success: false, message });
            } catch (requestError) {
              setStatus({ loading: false, success: false, message: requestError.message });
            } finally {
              setResending(false);
            }
          }}
        >
          <label className="field">
            <span>Email address</span>
            <input
              type="email"
              name="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </label>
          <button className="primary-button" type="submit" disabled={resending}>
            <span>{resending ? "Please wait…" : "Send a new verification link"}</span>
          </button>
        </form>
      )}
      {!status.loading && <div className="form-switch"><Link to="/login">Go to sign in</Link></div>}
    </AuthLayout>
  );
}
