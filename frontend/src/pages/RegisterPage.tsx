import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { parseApiError } from "../api/gateway";
import { useAuth } from "../auth/AuthContext";
import { UserRole } from "../auth/types";
import { validateRegister } from "../util/formValidation";

const roles: { value: UserRole; label: string; desc: string }[] = [
  { value: "STUDENT", label: "Student", desc: "Join classes and take exams" },
  { value: "INSTRUCTOR", label: "Instructor", desc: "Manage classes and exams" },
];

export default function RegisterPage() {
  const { register, loading } = useAuth();
  const nav = useNavigate();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<UserRole>("STUDENT");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const clientErr = validateRegister({ username, email, password });
    if (clientErr) {
      setError(clientErr);
      return;
    }
    try {
      const result = await register({
        username: username.trim(),
        email: email.trim(),
        password,
        role,
      });
      nav(`/verify-email?email=${encodeURIComponent(result.email)}`);
    } catch (err) {
      setError(parseApiError(err).message);
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
      <div style={{ width: "100%", maxWidth: 440 }}>
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
            Create account
          </h1>
          <p style={{ margin: "8px 0 0", color: "#64748b", fontSize: 14 }}>
            Register to start using E-Mid Quiz
          </p>
        </div>

        <div className="card" style={{ padding: 28 }}>
          <form noValidate onSubmit={onSubmit} className="grid" style={{ gap: 16 }}>
            <div className="grid-2 grid" style={{ gap: 14 }}>
              <div className="form-field">
                <div className="label">Username</div>
                <input
                  className="input"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. student1"
                  autoComplete="username"
                  minLength={3}
                  maxLength={50}
                />
              </div>
              <div className="form-field">
                <div className="label">Email</div>
                <input
                  className="input"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  autoComplete="email"
                  type="email"
                  maxLength={100}
                />
              </div>
            </div>

            {/* Role selector cards */}
            <div className="form-field">
              <div className="label">Role</div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 2 }}>
                {roles.map((r) => (
                  <label
                    key={r.value}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "10px 14px",
                      borderRadius: 10,
                      border: `1px solid ${role === r.value ? "var(--primary-border)" : "var(--border)"}`,
                      background: role === r.value ? "var(--primary-light)" : "transparent",
                      cursor: "pointer",
                      transition: "border-color 0.15s, background 0.15s",
                    }}
                  >
                    <input
                      type="radio"
                      name="role"
                      value={r.value}
                      checked={role === r.value}
                      onChange={() => setRole(r.value)}
                      style={{ accentColor: "var(--primary)" }}
                    />
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 600, color: "#e2e8f0" }}>{r.label}</div>
                      <div style={{ fontSize: 12, color: "#64748b" }}>{r.desc}</div>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div className="form-field">
              <div className="label">Password</div>
              <input
                className="input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 6 characters"
                autoComplete="new-password"
                minLength={6}
                maxLength={100}
              />
            </div>

            {error ? <div className="error">{error}</div> : null}

            <button
              className="btn btn-solid"
              type="submit"
              disabled={loading}
              style={{ width: "100%", padding: "11px", fontSize: 15 }}
            >
              {loading ? "Creating account…" : "Register"}
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
            Already have an account?{" "}
            <Link to="/login" style={{ color: "#60a5fa", fontWeight: 600 }}>
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
