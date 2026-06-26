import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, parseApiError } from "../api/gateway";
import { useAuth } from "../auth/AuthContext";
import { validateJoinCode } from "../util/formValidation";

export default function JoinClassPage() {
  const { profile } = useAuth();
  const nav = useNavigate();

  const [joinCode, setJoinCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const joinValidation = validateJoinCode(joinCode);

  const onJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    if (profile?.role !== "STUDENT") {
      setError("Only students can join a class with a join code.");
      return;
    }
    const clientErr = validateJoinCode(joinCode);
    if (clientErr) {
      setError(clientErr);
      return;
    }
    setLoading(true);
    try {
      await api.post(`/api/class/classes/join`, { joinCode: joinCode.trim().toUpperCase() });
      setSuccess("Joined successfully. Redirecting…");
      setTimeout(() => nav("/classes/student"), 800);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
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
      <div style={{ width: "100%", maxWidth: 420 }}>
        <div style={{ textAlign: "center", marginBottom: 28 }}>
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              width: 52,
              height: 52,
              borderRadius: 16,
              background: "linear-gradient(135deg, #10b981, #3b82f6)",
              fontSize: 24,
              marginBottom: 14,
              boxShadow: "0 8px 24px rgba(16,185,129,0.3)",
            }}
          >
            🎓
          </div>
          <h1
            style={{
              margin: 0,
              fontSize: 24,
              fontWeight: 800,
              letterSpacing: "-0.03em",
              color: "#e2e8f0",
            }}
          >
            Join a class
          </h1>
          <p style={{ margin: "8px 0 0", color: "#64748b", fontSize: 14 }}>
            Enter the 8-character join code from your instructor
          </p>
        </div>

        <div className="card" style={{ padding: 28 }}>
          <form noValidate onSubmit={onJoin} className="grid" style={{ gap: 16 }}>
            <div className="form-field">
              <div className="label">Join Code</div>
              <input
                className="input"
                value={joinCode}
                onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                placeholder="XXXXXXXX"
                maxLength={8}
                style={{
                  letterSpacing: "0.25em",
                  textAlign: "center",
                  fontSize: 22,
                  fontWeight: 800,
                  fontFamily: "monospace",
                  padding: "14px",
                  color: "#60a5fa",
                }}
                autoComplete="off"
                autoCapitalize="characters"
              />
            </div>

            {error ? <div className="error">{error}</div> : null}
            {success ? <div className="success">{success}</div> : null}

            <button
              className="btn btn-solid"
              type="submit"
              disabled={loading || !!joinValidation}
              style={{ width: "100%", padding: "11px", fontSize: 15 }}
            >
              {loading ? "Joining…" : "Join class"}
            </button>

            <button
              className="btn"
              type="button"
              onClick={() => nav("/classes/student")}
              disabled={loading}
              style={{ width: "100%" }}
            >
              ← Back to my classes
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
