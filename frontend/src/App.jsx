import { useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { AUTH_STORAGE_KEY, postAuth, readStoredAuth } from "./api.js";
import { AuthForm, ForgotPasswordPage, ResetPasswordPage, VerifyEmailPage } from "./auth/AuthPages.jsx";
import { AccountPage } from "./dashboard/AccountPage.jsx";

export default function App() {
  const [auth, setAuth] = useState(readStoredAuth);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    function handleAuthRefreshed(event) {
      setAuth(event.detail);
    }

    function handleSessionExpired() {
      setAuth(null);
      navigate("/login", {
        replace: true,
        state: { notice: "Your session expired. Please sign in again." },
      });
    }

    window.addEventListener("subtrack-auth-refreshed", handleAuthRefreshed);
    window.addEventListener("subtrack-session-expired", handleSessionExpired);
    return () => {
      window.removeEventListener("subtrack-auth-refreshed", handleAuthRefreshed);
      window.removeEventListener("subtrack-session-expired", handleSessionExpired);
    };
  }, [navigate]);

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
      <Route path="/forgot-password" element={auth ? <Navigate to="/account" replace /> : <ForgotPasswordPage />} />
      <Route path="/reset-password" element={auth ? <Navigate to="/account" replace /> : <ResetPasswordPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route
        path="/account"
        element={auth ? <AccountPage auth={auth} onLogout={handleLogout} /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
