import React, { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, parseApiError } from "../api/gateway";
import { exportExamReportCsv, ExamResult, getExamReport, getExamViolationEvents, ViolationEvent } from "../api/quiz";
import { shortenId } from "../utils/display";

type UserLite = {
  userId: string;
  username?: string;
  email?: string;
};

export default function ExamReportPage() {
  const { examId } = useParams();
  const [items, setItems] = useState<ExamResult[]>([]);
  const [userMap, setUserMap] = useState<Record<string, UserLite>>({});
  const [openUserId, setOpenUserId] = useState<string | null>(null);
  const [events, setEvents] = useState<ViolationEvent[]>([]);
  const [eventsLoading, setEventsLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sorted = useMemo(
    () => [...items].sort((a, b) => (b.score ?? -1) - (a.score ?? -1)),
    [items],
  );

  async function loadData() {
    if (!examId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getExamReport(examId);
      setItems(data);

      const ids = Array.from(new Set((data ?? []).map((x) => x.userId).filter(Boolean)));
      if (ids.length > 0) {
        const results = await Promise.allSettled(
          ids.map(async (userId) => {
            const res = await api.get<any, any>(`/api/auth/lookup-user-by-id`, { params: { userId } });
            const payload = res.data?.data ?? res.data;
            return payload as UserLite;
          }),
        );
        const map: Record<string, UserLite> = {};
        for (const r of results) {
          if (r.status === "fulfilled" && r.value?.userId) {
            map[r.value.userId] = r.value;
          }
        }
        setUserMap(map);
      } else {
        setUserMap({});
      }
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [examId]);

  const onDownload = async () => {
    if (!examId) return;
    setDownloading(true);
    setError(null);
    try {
      const blob = await exportExamReportCsv(examId);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `exam-${examId}-report.csv`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setDownloading(false);
    }
  };

  const onToggleEvents = async (userId: string) => {
    if (!examId) return;
    if (openUserId === userId) {
      setOpenUserId(null);
      setEvents([]);
      return;
    }
    setOpenUserId(userId);
    setEvents([]);
    setEventsLoading(true);
    setError(null);
    try {
      const data = await getExamViolationEvents(examId, userId);
      setEvents(data);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setEventsLoading(false);
    }
  };

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <div>
          <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>Exam report</h2>
          <p className="hint" style={{ marginTop: 4 }}>Scores and integrity violations per exam</p>
        </div>
        <div className="row">
          <Link className="btn" to="/exam/manage">
            Manage exams
          </Link>
          <button className="btn" onClick={() => loadData().catch(() => undefined)}>
            Refresh
          </button>
          <button className="btn btn-solid" onClick={onDownload} disabled={downloading || !examId}>
            {downloading ? "Downloading CSV…" : "Export CSV"}
          </button>
        </div>
      </div>

      {error ? <div className="error">{error}</div> : null}

      <div className="card">
        {!examId ? (
          <div className="hint">Invalid exam id.</div>
        ) : loading ? (
          <div className="hint">Loading…</div>
        ) : sorted.length === 0 ? (
          <div className="hint">No submissions yet.</div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ textAlign: "left", color: "#94a3b8" }}>
                  <th style={{ padding: "8px 6px" }}>User</th>
                  <th style={{ padding: "8px 6px" }}>Score</th>
                  <th style={{ padding: "8px 6px" }}>Questions</th>
                  <th style={{ padding: "8px 6px" }}>Violations</th>
                  <th style={{ padding: "8px 6px" }}>Submitted</th>
                  <th style={{ padding: "8px 6px" }}></th>
                </tr>
              </thead>
              <tbody>
                {sorted.flatMap((row) => {
                  const isOpen = openUserId === row.userId;
                  return [
                    <tr key={row.id} style={{ borderTop: "1px solid var(--border)" }}>
                      <td style={{ padding: "9px 6px", minWidth: 230 }}>
                        <div style={{ display: "flex", flexDirection: "column", gap: 2, minWidth: 0 }}>
                          <span
                            style={{
                              color: "var(--text)",
                              fontWeight: 700,
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap",
                            }}
                            title={userMap[row.userId]?.username || userMap[row.userId]?.email || row.userId}
                          >
                            {userMap[row.userId]?.username || userMap[row.userId]?.email || shortenId(row.userId)}
                          </span>
                          {userMap[row.userId]?.email && userMap[row.userId]?.username ? (
                            <span style={{ fontSize: 11, color: "#64748b" }}>{userMap[row.userId]?.email}</span>
                          ) : null}
                        </div>
                      </td>
                      <td style={{ padding: "9px 6px" }}>{row.score ?? "Pending"}</td>
                      <td style={{ padding: "9px 6px" }}>{row.totalQuestions ?? "-"}</td>
                      <td style={{ padding: "9px 6px" }}>{row.violationCount}</td>
                      <td style={{ padding: "9px 6px" }}>
                        {row.submittedAt ? new Date(row.submittedAt).toLocaleString("en-US") : "-"}
                      </td>
                      <td style={{ padding: "9px 6px" }}>
                        <button className="btn" onClick={() => onToggleEvents(row.userId)}>
                          {isOpen ? "Hide log" : "View log"}
                        </button>
                      </td>
                    </tr>,
                    isOpen ? (
                      <tr key={`${row.id}-events`}>
                        <td colSpan={6} style={{ padding: "10px 6px" }}>
                          <div
                            style={{
                              border: "1px solid var(--border)",
                              borderRadius: 10,
                              padding: 10,
                              background: "rgba(15,23,42,.35)",
                            }}
                          >
                            {eventsLoading ? (
                              <div className="hint">Loading violation log…</div>
                            ) : events.length === 0 ? (
                              <div className="hint">No violation events stored.</div>
                            ) : (
                              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                                {events.map((e) => (
                                  <div
                                    key={e.id}
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      justifyContent: "space-between",
                                      gap: 12,
                                      padding: "6px 8px",
                                      borderRadius: 8,
                                      background: "rgba(2,6,23,.35)",
                                      border: "1px solid rgba(148,163,184,.18)",
                                    }}
                                  >
                                    <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                                      <span style={{ fontWeight: 800, color: "var(--text)" }}>{e.type}</span>
                                      <span className="hint" style={{ fontFamily: "monospace" }}>
                                        #{e.id}
                                      </span>
                                    </div>
                                    <div className="hint">
                                      {e.createdAt ? new Date(e.createdAt).toLocaleString("en-US") : "-"}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        </td>
                      </tr>
                    ) : null,
                  ].filter(Boolean) as any;
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
