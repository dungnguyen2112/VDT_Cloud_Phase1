import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { parseApiError } from "../api/gateway";
import { useAuth } from "../auth/AuthContext";
import { validateLogin } from "../util/formValidation";

export default function LoginPage() {
  const { login, loading } = useAuth();
  const nav = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const clientErr = validateLogin(username, password);
    if (clientErr) {
      setError(clientErr);
      return;
    }
    try {
      await login(username.trim(), password);
      nav("/");
    } catch (err) {
      const message = parseApiError(err).message;
      if (message.includes("Email not verified")) {
        nav("/enter-email-verify", { replace: true });
        return;
      }
      setError(message);
    }
  };

  return (
    <div
      style={{
        minHeight: "calc(100vh - 56px)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "24px 16px",
      }}
    >
      <div style={{ width: "100%", maxWidth: 400 }}>
        {/* Logo / header */}
        <div style={{ textAlign: "center", marginBottom: 28 }}>
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              width: 52,
              height: 52,
              borderRadius: 16,
              background: "linear-gradient(135deg, #3b82f6, #8b5cf6)",
              fontSize: 24,
              marginBottom: 14,
              boxShadow: "0 8px 24px rgba(59,130,246,0.35)",
            }}
          >
            📝
          </div>
          <h1
            style={{
              margin: 0,
              fontSize: 26,
              fontWeight: 800,
              letterSpacing: "-0.03em",
              background: "linear-gradient(90deg, #60a5fa, #a78bfa)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
            }}
          >
            E-Mid Quiz
          </h1>
          <p style={{ margin: "8px 0 0", color: "#64748b", fontSize: 14 }}>
            Sign in to the quiz system
          </p>
        </div>

        <div className="card" style={{ padding: 28 }}>
          <form noValidate onSubmit={onSubmit} className="grid" style={{ gap: 18 }}>
            <div className="form-field">
              <div className="label">Username</div>
              <input
                className="input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. instructor1"
                autoComplete="username"
              />
            </div>
            <div className="form-field">
              <div className="label">Password</div>
              <input
                className="input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete="current-password"
              />
            </div>

            {error ? <div className="error">{error}</div> : null}

            <button
              className="btn btn-solid"
              type="submit"
              disabled={loading}
              style={{ width: "100%", padding: "11px", fontSize: 15 }}
            >
              {loading ? "Signing in…" : "Sign in"}
            </button>
          </form>

          <div
            style={{
              marginTop: 20,
              paddingTop: 18,
              borderTop: "1px solid var(--border)",
              textAlign: "center",
              fontSize: 13,
              color: "#64748b",
            }}
          >
            No account?{" "}
            <Link to="/register" style={{ color: "#60a5fa", fontWeight: 600 }}>
              Register
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
