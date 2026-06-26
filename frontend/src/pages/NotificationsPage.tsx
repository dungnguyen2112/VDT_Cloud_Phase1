import React, { useEffect, useState } from "react";
import { api, parseApiError } from "../api/gateway";
import { useAuth } from "../auth/AuthContext";

type Notification = {
  id: string;
  type: string;
  title: string;
  body: string;
  channel: string;
  createdAt?: string;
};

const typeIcon: Record<string, string> = {
  EXAM_SUBMITTED: "📊",
  EXAM_CREATED: "📝",
  CLASS_USER_ADDED: "🎓",
  USER_ADDED_TO_CLASS: "🎓",
};

const channelColor: Record<string, string> = {
  IN_APP: "#60a5fa",
  EMAIL: "#a78bfa",
};

export default function NotificationsPage() {
  const { token } = useAuth();
  const [items, setItems] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    setLoading(true);
    setError(null);
    api
      .get<any>(`/api/notification/me`)
      .then((res) => {
        const data = res.data?.data ?? [];
        setItems(Array.isArray(data) ? data : []);
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, [token]);

  return (
    <div className="grid" style={{ gap: 16, maxWidth: 680 }}>
      <div>
        <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>Notifications</h2>
        <p className="hint" style={{ marginTop: 4 }}>In-app messages from the system</p>
      </div>

      {loading ? (
        <div className="card" style={{ color: "#64748b" }}>Loading…</div>
      ) : error ? (
        <div className="error">{error}</div>
      ) : items.length === 0 ? (
        <div
          className="card"
          style={{
            textAlign: "center",
            padding: "48px 24px",
            borderStyle: "dashed",
            color: "#64748b",
          }}
        >
          <div style={{ fontSize: 40, marginBottom: 12 }}>🔔</div>
          <div style={{ fontWeight: 600, color: "#94a3b8" }}>No notifications yet</div>
        </div>
      ) : (
        items.map((n) => (
          <div
            key={n.id}
            className="card"
            style={{
              borderLeft: `3px solid ${channelColor[n.channel] ?? "#64748b"}`,
              padding: "16px 20px",
            }}
          >
            <div style={{ display: "flex", alignItems: "flex-start", gap: 14 }}>
              <div
                style={{
                  fontSize: 22,
                  lineHeight: 1,
                  marginTop: 2,
                  flexShrink: 0,
                }}
              >
                {typeIcon[n.type] ?? "🔔"}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    marginBottom: 4,
                    flexWrap: "wrap",
                  }}
                >
                  <span style={{ fontWeight: 700, fontSize: 14 }}>{n.title}</span>
                  <span
                    style={{
                      fontSize: 11,
                      fontWeight: 700,
                      letterSpacing: "0.04em",
                      color: channelColor[n.channel] ?? "#94a3b8",
                      background: `${channelColor[n.channel] ?? "#94a3b8"}18`,
                      padding: "2px 8px",
                      borderRadius: 999,
                    }}
                  >
                    {n.channel}
                  </span>
                </div>
                <div style={{ fontSize: 13, color: "#94a3b8", lineHeight: 1.6 }}>{n.body}</div>
                {n.createdAt ? (
                  <div style={{ marginTop: 8, fontSize: 11, color: "#64748b" }}>
                    {new Date(n.createdAt).toLocaleString("en-US")}
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
