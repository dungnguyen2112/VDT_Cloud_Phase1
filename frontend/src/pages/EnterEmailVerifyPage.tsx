import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function EnterEmailVerifyPage() {
  const nav = useNavigate();
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setError("Email is required");
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setError("Please enter a valid email");
      return;
    }

    nav(`/verify-email?email=${encodeURIComponent(trimmedEmail)}`);
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
              background: "linear-gradient(135deg, #60a5fa, #a78bfa)",
              fontSize: 24,
              marginBottom: 14,
              boxShadow: "0 8px 24px rgba(59,130,246,0.35)",
            }}
          >
            ✉️
          </div>
          <h1
            style={{
              margin: 0,
              fontSize: 24,
              fontWeight: 800,
              letterSpacing: "-0.03em",
              background: "linear-gradient(90deg, #60a5fa, #a78bfa)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
            }}
          >
            Verify Your Email
          </h1>
          <p style={{ margin: "8px 0 0", color: "#64748b", fontSize: 14 }}>
            Enter your email to receive the verification code
          </p>
        </div>

        <div className="card" style={{ padding: 28 }}>
          <form noValidate onSubmit={onSubmit} className="grid" style={{ gap: 16 }}>
            <div className="form-field">
              <div className="label">Email</div>
              <input
                className="input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your.email@example.com"
                autoComplete="email"
              />
            </div>

            {error ? <div className="error">{error}</div> : null}

            <button
              className="btn btn-solid"
              type="submit"
              style={{ width: "100%", padding: "11px", fontSize: 15 }}
            >
              Continue to Verification
            </button>

            <button
              className="btn"
              type="button"
              style={{ width: "100%" }}
              onClick={() => nav("/login")}
            >
              ← Back to login
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
