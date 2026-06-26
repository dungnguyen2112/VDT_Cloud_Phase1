import React, { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { parseApiError } from "../api/gateway";
import { useAuth } from "../auth/AuthContext";
import { validateVerificationCode } from "../util/formValidation";

export default function VerifyEmailPage() {
  const { verifyEmail, resendVerificationCode } = useAuth();
  const nav = useNavigate();
  const [searchParams] = useSearchParams();

  const emailFromQuery = searchParams.get("email") ?? "";
  const email = useMemo(() => emailFromQuery.trim(), [emailFromQuery]);

  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!email) {
      setError("Missing email. Please register again.");
      return;
    }
    const codeErr = validateVerificationCode(code);
    if (codeErr) {
      setError(codeErr);
      return;
    }

    setLoading(true);
    try {
      await verifyEmail(email, code.replace(/\s+/g, ""));
      nav("/");
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  const onResend = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!email) {
      setError("Missing email.");
      return;
    }

    setResendLoading(true);
    try {
      await resendVerificationCode(email);
      setSuccess("Verification code resent! Check your email.");
      setCode("");
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setResendLoading(false);
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
              background: "linear-gradient(135deg, #60a5fa, #a78bfa)",
              fontSize: 24,
              marginBottom: 14,
              boxShadow: "0 8px 24px rgba(59,130,246,0.35)",
            }}
          >
            ✅
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
            Verify email
          </h1>
          <p style={{ margin: "8px 0 0", color: "#64748b", fontSize: 14 }}>
            Enter the 6-character code sent to your email
          </p>
        </div>

        <div className="card" style={{ padding: 28 }}>
          <form noValidate onSubmit={onSubmit} className="grid" style={{ gap: 16 }}>
            <div className="form-field">
              <div className="label">Email</div>
              <input className="input" value={email} readOnly style={{ fontFamily: "monospace" }} />
              <div className="hint">If this email is wrong, go back and register again.</div>
            </div>

            <div className="form-field">
              <div className="label">Verification code</div>
              <input
                className="input"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\s+/g, ""))}
                placeholder="000000"
                maxLength={6}
                style={{
                  letterSpacing: "0.25em",
                  textAlign: "center",
                  fontSize: 22,
                  fontWeight: 800,
                  fontFamily: "monospace",
                  padding: "14px",
                }}
              />
            </div>

            {error ? <div className="error">{error}</div> : null}
            {success ? <div className="success">{success}</div> : null}

            <button
              className="btn btn-solid"
              type="submit"
              disabled={loading || resendLoading || !email}
              style={{ width: "100%", padding: "11px", fontSize: 15 }}
            >
              {loading ? "Verifying…" : "Verify"}
            </button>

            <button
              className="btn"
              type="button"
              disabled={loading || resendLoading || !email}
              style={{ width: "100%" }}
              onClick={onResend}
            >
              {resendLoading ? "Resending…" : "Resend Code"}
            </button>

            <button
              className="btn"
              type="button"
              disabled={loading || resendLoading}
              style={{ width: "100%" }}
              onClick={() => nav("/register")}
            >
              ← Back to register
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

