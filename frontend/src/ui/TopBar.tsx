import React from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const roleColor: Record<string, string> = {
  INSTRUCTOR: "#a78bfa",
  ADMIN: "#f472b6",
  STUDENT: "#34d399",
};

function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  const { pathname } = useLocation();
  const active = pathname.startsWith(to);
  return (
    <Link
      to={to}
      style={{
        padding: "6px 12px",
        borderRadius: 8,
        fontSize: 14,
        fontWeight: active ? 600 : 400,
        color: active ? "#e2e8f0" : "#94a3b8",
        background: active ? "rgba(255,255,255,0.08)" : "transparent",
        transition: "color 0.15s, background 0.15s",
        textDecoration: "none",
      }}
    >
      {children}
    </Link>
  );
}

export default function TopBar() {
  const { profile, logout } = useAuth();
  const nav = useNavigate();
  const role = profile?.role;
  const rc = roleColor[role ?? ""] ?? "#94a3b8";

  return (
    <header
      style={{
        position: "sticky",
        top: 0,
        zIndex: 20,
        backdropFilter: "blur(16px)",
        WebkitBackdropFilter: "blur(16px)",
        background: "rgba(13,17,23,0.82)",
        borderBottom: "1px solid rgba(148,163,184,0.1)",
      }}
    >
      <div
        className="container"
        style={{
          paddingTop: 0,
          paddingBottom: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          height: 56,
        }}
      >
        {/* Brand + nav */}
        <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <Link
            to="/"
            style={{
              fontWeight: 800,
              fontSize: 16,
              letterSpacing: "-0.02em",
              color: "#e2e8f0",
              textDecoration: "none",
              marginRight: 12,
              background: "linear-gradient(90deg, #60a5fa, #a78bfa)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
            }}
          >
            E-Mid Quiz
          </Link>

          {role === "STUDENT" ? (
            <>
              <NavLink to="/classes/student">My classes</NavLink>
              <NavLink to="/join">Join class</NavLink>
              <NavLink to="/exam/my-classes">Exams</NavLink>
              <NavLink to="/results/me">Results</NavLink>
            </>
          ) : role ? (
            <>
              <NavLink to="/classes/teacher">Classes</NavLink>
              <NavLink to="/question-bank">Question bank</NavLink>
              <NavLink to="/exam/manage">Exams</NavLink>
            </>
          ) : null}

          {profile ? <NavLink to="/notifications">Notifications</NavLink> : null}
        </div>

        {/* Right side */}
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          {profile ? (
            <>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  padding: "5px 12px",
                  borderRadius: 8,
                  background: "rgba(255,255,255,0.05)",
                  border: "1px solid rgba(148,163,184,0.12)",
                }}
              >
                <span style={{ fontSize: 13, color: "#e2e8f0" }}>
                  {profile.username}
                </span>
                <span
                  style={{
                    fontSize: 11,
                    fontWeight: 700,
                    letterSpacing: "0.05em",
                    color: rc,
                    background: `${rc}1a`,
                    padding: "2px 7px",
                    borderRadius: 999,
                    border: `1px solid ${rc}44`,
                  }}
                >
                  {role}
                </span>
              </div>
              <button
                className="btn btn-danger"
                style={{ fontSize: 13, padding: "6px 14px" }}
                onClick={() => {
                  logout();
                  nav("/login");
                }}
              >
                Log out
              </button>
            </>
          ) : (
            <Link to="/login" className="btn btn-solid" style={{ fontSize: 13 }}>
              Log in
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
