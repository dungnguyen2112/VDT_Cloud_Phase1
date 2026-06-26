import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, parseApiError } from "../api/gateway";
import { validateClassName, validateEmailSearch } from "../util/formValidation";
import { Exam, ExamResult, getMyResults, listExams, listMyClassExams } from "../api/quiz";
import { useAuth } from "../auth/AuthContext";

type ClassResponse = {
  id: string;
  name: string;
  teacherId?: string;
  joinCode?: string;
  joinUrl?: string | null;
};

type StudentResponse = { userId: string };
type StudentProfile = { userId: string; username: string; email: string; role?: string };

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: "0.08em",
        textTransform: "uppercase",
        color: "#64748b",
        marginBottom: 8,
      }}
    >
      {children}
    </div>
  );
}

export default function ClassesPage() {
  const { profile } = useAuth();
  const { mode } = useParams();
  const nav = useNavigate();

  const role = profile?.role;
  const isStudent = role === "STUDENT";
  const isTeacher = role === "INSTRUCTOR" || role === "ADMIN";

  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<ClassResponse | null>(null);
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [studentsLoading, setStudentsLoading] = useState(false);
  const [studentProfiles, setStudentProfiles] = useState<Record<string, StudentProfile>>({});
  const [teacherProfile, setTeacherProfile] = useState<StudentProfile | null>(null);

  const [classExams, setClassExams] = useState<Exam[]>([]);
  const [examsLoading, setExamsLoading] = useState(false);
  const [myResults, setMyResults] = useState<ExamResult[]>([]);

  const [createName, setCreateName] = useState("");
  const [createLoading, setCreateLoading] = useState(false);

  const selectedJoinCode = selected?.joinCode ?? "";
  const selectedActionEnabled = useMemo(() => !!selectedId && !!selected, [selectedId, selected]);

  const [lookupEmail, setLookupEmail] = useState("");
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupUser, setLookupUser] = useState<{ userId: string; userEmail: string } | null>(null);
  const [addUserLoading, setAddUserLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  async function loadTeacherClasses() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get<any, any>(`/api/class/classes`);
      const data = res.data?.data ?? res.data;
      setClasses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  async function loadStudentClasses() {
    if (!profile?.id) return;
    setLoading(true);
    setError(null);
    try {
      const res = await api.get<any, any>(`/api/class/users/${profile.id}/classes`);
      const data = res.data?.data ?? res.data;
      setClasses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  async function loadSelectedDetailsAndStudents(classId: string) {
    setActionError(null);
    setActionSuccess(null);
    setSelectedId(classId);
    setSelected(null);
    setStudents([]);
    setStudentProfiles({});
    try {
      const res = await api.get<any, any>(`/api/class/classes/${classId}`);
      const details = res.data?.data ?? res.data;
      setSelected(details);

      setStudentsLoading(true);
      const res2 = await api.get<any, any>(`/api/class/classes/${classId}/students`);
      const data2 = res2.data?.data ?? res2.data;
      const list: StudentResponse[] = Array.isArray(data2) ? data2 : [];
      setStudents(list);

      // Hydrate student profile so UI can show name/email instead of only userId.
      const ids = list.map((s) => s.userId).filter(Boolean);
      if (ids.length > 0) {
        const results = await Promise.allSettled(
          ids.map(async (userId) => {
            const r = await api.get<any, any>(`/api/auth/lookup-user-by-id`, { params: { userId } });
            const p = r.data?.data ?? r.data;
            return p as StudentProfile;
          })
        );
        const map: Record<string, StudentProfile> = {};
        for (const item of results) {
          if (item.status === "fulfilled" && item.value?.userId) {
            map[item.value.userId] = item.value;
          }
        }
        setStudentProfiles(map);
      }
    } catch (err) {
      setActionError(parseApiError(err).message);
    } finally {
      setStudentsLoading(false);
    }
  }

  useEffect(() => {
    if (!role) return;
    if (isStudent) loadStudentClasses().catch(() => undefined);
    else if (isTeacher) loadTeacherClasses().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role]);

  useEffect(() => {
    // Make instructor UX smoother: when coming back to this page,
    // auto-select the first class so right panel shows join code + exams.
    if (!isTeacher) return;
    if (selectedId) return;
    if (classes.length === 0) return;
    setSelectedId(classes[0].id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isTeacher, classes, selectedId]);

  useEffect(() => {
    if (!isStudent) return;
    if (selectedId) return;
    if (classes.length === 0) return;
    setSelectedId(classes[0].id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isStudent, classes, selectedId]);

  useEffect(() => {
    if (!isTeacher || !selectedId) return;
    loadSelectedDetailsAndStudents(selectedId).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId, isTeacher]);

  useEffect(() => {
    if (!isStudent || !selectedId) return;
    setActionError(null);
    setSelected(null);
    setStudents([]);
    setStudentProfiles({});
    setTeacherProfile(null);
    setStudentsLoading(true);

    async function loadStudentClassDetails() {
      try {
        const res = await api.get<any, any>(`/api/class/classes/${selectedId}`);
        const details = res.data?.data ?? res.data;
        setSelected(details);

        const res2 = await api.get<any, any>(`/api/class/classes/${selectedId}/students`);
        const data2 = res2.data?.data ?? res2.data;
        const list: StudentResponse[] = Array.isArray(data2) ? data2 : [];
        setStudents(list);

        const ids = list.map((s) => s.userId).filter(Boolean);
        if (details?.teacherId) ids.push(details.teacherId);
        const uniqueIds = Array.from(new Set(ids));

        if (uniqueIds.length > 0) {
          const results = await Promise.allSettled(
            uniqueIds.map(async (userId) => {
              const r = await api.get<any, any>(`/api/auth/lookup-user-by-id`, { params: { userId } });
              const p = r.data?.data ?? r.data;
              return p as StudentProfile;
            }),
          );
          const map: Record<string, StudentProfile> = {};
          for (const item of results) {
            if (item.status === "fulfilled" && item.value?.userId) {
              map[item.value.userId] = item.value;
            }
          }
          setStudentProfiles(map);
          if (details?.teacherId && map[details.teacherId]) {
            setTeacherProfile(map[details.teacherId]);
          }
        }
      } catch (err) {
        setActionError(parseApiError(err).message);
      } finally {
        setStudentsLoading(false);
      }
    }

    loadStudentClassDetails().catch(() => undefined);
  }, [isStudent, selectedId]);

  useEffect(() => {
    if (!isTeacher || !selectedId) return;
    setExamsLoading(true);
    setClassExams([]);
    listExams()
      .then((all) => {
        const filtered = all
          .filter((e) => e.classId === selectedId)
          .sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""));
        setClassExams(filtered);
      })
      .catch((err) => {
        // Best-effort: exams list is a convenience UI, don't break class member management.
        setClassExams([]);
        // eslint-disable-next-line no-console
        console.warn("Failed to load class exams", err);
      })
      .finally(() => setExamsLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId, isTeacher]);

  useEffect(() => {
    if (!isStudent || !selectedId) return;
    setExamsLoading(true);
    setClassExams([]);
    Promise.all([listMyClassExams(), getMyResults()])
      .then(([all, results]) => {
        const filtered = all
          .filter((e) => e.classId === selectedId)
          .sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""));
        setClassExams(filtered);
        setMyResults(results ?? []);
      })
      .catch(() => {
        setClassExams([]);
        setMyResults([]);
      })
      .finally(() => setExamsLoading(false));
  }, [isStudent, selectedId]);

  const onCreateClass = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionError(null);
    setActionSuccess(null);
    const nameErr = validateClassName(createName);
    if (nameErr) {
      setActionError(nameErr);
      return;
    }
    setCreateLoading(true);
    try {
      const res = await api.post<any, any>(`/api/class/classes`, { name: createName.trim() });
      const data = res.data?.data ?? res.data;
      if (data?.id) {
        setCreateName("");
        await loadTeacherClasses();
        setSelectedId(data.id);
      } else {
        setActionSuccess("Class created.");
      }
    } catch (err) {
      setActionError(parseApiError(err).message);
    } finally {
      setCreateLoading(false);
    }
  };

  const onRegenerateJoinCode = async () => {
    if (!selectedId) return;
    setActionError(null);
    setActionSuccess(null);
    setStudentsLoading(true);
    try {
      const res = await api.post<any, any>(`/api/class/classes/${selectedId}/regenerate-join-code`, {});
      const data = res.data?.data ?? res.data;
      setSelected(data);
      setActionSuccess("New join code generated.");
    } catch (err) {
      setActionError(parseApiError(err).message);
    } finally {
      setStudentsLoading(false);
    }
  };

  const onCopyJoinCode = async () => {
    try {
      await navigator.clipboard.writeText(selectedJoinCode);
      setActionSuccess("Join code copied.");
      setActionError(null);
    } catch {
      setActionError("Could not copy. Copy manually.");
      setActionSuccess(null);
    }
  };

  const onLookupUser = async () => {
    setActionError(null);
    setActionSuccess(null);
    const email = lookupEmail.trim();
    const emailErr = validateEmailSearch(email);
    if (emailErr) {
      setActionError(emailErr);
      return;
    }
    setLookupLoading(true);
    setLookupUser(null);
    try {
      const res = await api.get<any, any>(`/api/auth/lookup-user`, { params: { email } });
      const data = res.data?.data ?? res.data;
      setLookupUser({ userId: data.userId, userEmail: data.email ?? email });
      setActionSuccess("User found.");
    } catch (err) {
      setActionError(parseApiError(err).message);
    } finally {
      setLookupLoading(false);
    }
  };

  const onAddUserToClass = async () => {
    if (!selectedId || !lookupUser) { setActionError("Look up a user first."); return; }
    setActionError(null);
    setActionSuccess(null);
    setAddUserLoading(true);
    try {
      await api.post<any, any>(`/api/class/classes/${selectedId}/add-user`, {
        userId: lookupUser.userId,
        userEmail: lookupUser.userEmail,
      });
      setActionSuccess("Member added to class.");
      setStudentsLoading(true);
      const res = await api.get<any, any>(`/api/class/classes/${selectedId}/students`);
      const data = res.data?.data ?? res.data;
      const list: StudentResponse[] = Array.isArray(data) ? data : [];
      setStudents(list);

      const ids = list.map((s) => s.userId).filter(Boolean);
      if (ids.length > 0) {
        const results = await Promise.allSettled(
          ids.map(async (userId) => {
            const r = await api.get<any, any>(`/api/auth/lookup-user-by-id`, { params: { userId } });
            const p = r.data?.data ?? r.data;
            return p as StudentProfile;
          })
        );
        const map: Record<string, StudentProfile> = {};
        for (const item of results) {
          if (item.status === "fulfilled" && item.value?.userId) {
            map[item.value.userId] = item.value;
          }
        }
        setStudentProfiles(map);
      } else {
        setStudentProfiles({});
      }
    } catch (err) {
      setActionError(parseApiError(err).message);
    } finally {
      setAddUserLoading(false);
      setStudentsLoading(false);
    }
  };

  if (!role) {
    return <div className="card">Loading profile…</div>;
  }

  /* ─── Student view ─── */
  if (isStudent) {
    return (
      <div className="grid" style={{ gap: 16 }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div>
            <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>My Classes</h2>
            <p className="hint" style={{ marginTop: 4 }}>Classes you are enrolled in</p>
          </div>
          <Link className="btn btn-solid" to="/join">
            + Join class
          </Link>
        </div>

        {loading ? (
          <div className="card" style={{ color: "#64748b" }}>Loading…</div>
        ) : error ? (
          <div className="error">{error}</div>
        ) : classes.length === 0 ? (
          <div
            className="card"
            style={{
              textAlign: "center",
              padding: "40px 24px",
              color: "#64748b",
              borderStyle: "dashed",
            }}
          >
            <div style={{ fontSize: 36, marginBottom: 12 }}>🎓</div>
            <div style={{ fontWeight: 600, color: "#94a3b8" }}>You are not in any class yet</div>
            <div style={{ fontSize: 13, marginTop: 6 }}>
              Use <b>Join class</b> with the join code from your instructor
            </div>
          </div>
        ) : (
          <div className="grid grid-2" style={{ alignItems: "start", gap: 16 }}>
            <div className="card">
              <SectionTitle>My classes ({classes.length})</SectionTitle>
              <div className="grid" style={{ gap: 6, marginTop: 4 }}>
                {classes.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setSelectedId(c.id)}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "10px 14px",
                      border: `1px solid ${selectedId === c.id ? "var(--primary-border)" : "var(--border)"}`,
                      borderLeft: `3px solid ${selectedId === c.id ? "var(--primary)" : "transparent"}`,
                      borderRadius: 10,
                      background: selectedId === c.id ? "var(--primary-light)" : "var(--surface2)",
                      cursor: "pointer",
                      textAlign: "left",
                      width: "100%",
                      color: "var(--text)",
                    }}
                  >
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 700, fontSize: 14 }}>{c.name}</div>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <div className="card">
              {!selected ? (
                <div className="hint">Select a class on the left to see details.</div>
              ) : (
                <div className="grid" style={{ gap: 16 }}>
                  <div>
                    <h3 style={{ margin: 0, fontSize: 20, fontWeight: 800 }}>{selected.name}</h3>
                    <div
                      className="hint"
                      style={{
                        marginTop: 6,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                      title={teacherProfile?.username || teacherProfile?.email || selected.teacherId}
                    >
                      Instructor: {teacherProfile?.username || teacherProfile?.email || "—"}
                    </div>
                  </div>

                  <div style={{ height: 1, background: "var(--border)" }} />

                  <div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        marginBottom: 10,
                      }}
                    >
                      <SectionTitle>Class members</SectionTitle>
                      <span
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          color: "#60a5fa",
                          background: "rgba(59,130,246,.15)",
                          padding: "2px 9px",
                          borderRadius: 999,
                        }}
                      >
                        {students.length}
                      </span>
                    </div>

                    {studentsLoading ? (
                      <div className="hint">Loading members…</div>
                    ) : students.length === 0 ? (
                      <div className="hint">No members in this class yet.</div>
                    ) : (
                      <div style={{ display: "flex", flexDirection: "column", gap: 6, maxHeight: 180, overflowY: "auto" }}>
                        {students.map((s) => (
                          <div
                            key={s.userId}
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: 8,
                              padding: "7px 12px",
                              background: "var(--surface2)",
                              borderRadius: 8,
                              border: "1px solid var(--border)",
                            }}
                          >
                            <span
                              style={{
                                width: 6,
                                height: 6,
                                borderRadius: "50%",
                                background: "#34d399",
                                flexShrink: 0,
                              }}
                            />
                            <span style={{ display: "flex", flexDirection: "column", gap: 2, minWidth: 0 }}>
                              <span
                                style={{
                                  color: "var(--text)",
                                  fontWeight: 700,
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  whiteSpace: "nowrap",
                                  maxWidth: 240,
                                }}
                              >
                                {studentProfiles[s.userId]?.username ||
                                  studentProfiles[s.userId]?.email ||
                                  "Member"}
                              </span>
                              {studentProfiles[s.userId]?.email && studentProfiles[s.userId]?.username ? (
                                <span style={{ fontSize: 11, color: "#64748b" }}>
                                  {studentProfiles[s.userId]?.email}
                                </span>
                              ) : null}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div style={{ height: 1, background: "var(--border)" }} />

                  <div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        marginBottom: 10,
                      }}
                    >
                      <SectionTitle>Class exams</SectionTitle>
                      <span
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          color: "#a78bfa",
                          background: "rgba(167,139,250,.15)",
                          padding: "2px 9px",
                          borderRadius: 999,
                        }}
                      >
                        {classExams.length}
                      </span>
                    </div>

                    {examsLoading ? (
                      <div className="hint">Loading exams…</div>
                    ) : classExams.length === 0 ? (
                      <div className="hint">No published exams for this class.</div>
                    ) : (
                      <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                        {classExams.map((exam) => {
                          const attempts = myResults.filter((r) => r.examId === exam.id).length;
                          const maxAttempts = exam.maxAttempts ?? 1;
                          const reachedLimit = attempts >= maxAttempts;
                          return (
                            <div
                              key={exam.id}
                              style={{
                                padding: "10px 12px",
                                borderRadius: 10,
                                background: "rgba(30,37,54,0.35)",
                                border: "1px solid var(--border)",
                              }}
                            >
                              <div style={{ fontWeight: 800 }}>{exam.title}</div>
                              <div className="hint" style={{ marginTop: 4 }}>
                                Duration: {exam.duration}m · Attempts: {maxAttempts} · Taken: {attempts}
                              </div>
                              {reachedLimit ? (
                                <div className="hint" style={{ marginTop: 8, color: "#fca5a5" }}>
                                  You have used all attempts for this exam.
                                </div>
                              ) : null}
                              <div className="row" style={{ marginTop: 10 }}>
                                {reachedLimit ? (
                                  <button className="btn" disabled>
                                    No attempts left
                                  </button>
                                ) : (
                                  <Link className="btn btn-solid" to={`/exam/${exam.id}/take`}>
                                    Take exam
                                  </Link>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  {actionError ? <div className="error">{actionError}</div> : null}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    );
  }

  if (!isTeacher) {
    return <div className="error">This page is only for instructors.</div>;
  }

  /* ─── Teacher view ─── */
  return (
    <div className="grid" style={{ gap: 16 }}>
      <div>
        <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>Manage classes</h2>
        <p className="hint" style={{ marginTop: 4 }}>
          Create classes, manage join codes and members
        </p>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start", gap: 16 }}>
        {/* Left column */}
        <div className="grid" style={{ gap: 14 }}>
          {/* Create class */}
          <div className="card">
            <SectionTitle>Create class</SectionTitle>
            <form noValidate onSubmit={onCreateClass} className="grid" style={{ gap: 12 }}>
              <input
                className="input"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
                placeholder="Class name, e.g. CS101"
              />
              {actionError ? <div className="error">{actionError}</div> : null}
              {actionSuccess && !selectedId ? <div className="success">{actionSuccess}</div> : null}
              <button
                className="btn btn-solid"
                type="submit"
                disabled={createLoading || !createName.trim()}
                style={{ width: "100%" }}
              >
                {createLoading ? "Creating…" : "+ Create class"}
              </button>
            </form>
          </div>

          {/* Class list */}
          <div className="card">
            <SectionTitle>Your classes ({classes.length})</SectionTitle>
            {loading ? (
              <div style={{ color: "#64748b", fontSize: 13 }}>Loading…</div>
            ) : error ? (
              <div className="error">{error}</div>
            ) : classes.length === 0 ? (
              <div style={{ color: "#64748b", fontSize: 13, padding: "12px 0" }}>
                No classes yet.
              </div>
            ) : (
              <div className="grid" style={{ gap: 6, marginTop: 4 }}>
                {classes.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setSelectedId(c.id)}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "10px 14px",
                      border: `1px solid ${selectedId === c.id ? "var(--primary-border)" : "var(--border)"}`,
                      borderLeft: `3px solid ${selectedId === c.id ? "var(--primary)" : "transparent"}`,
                      borderRadius: 10,
                      background: selectedId === c.id ? "var(--primary-light)" : "var(--surface2)",
                      cursor: "pointer",
                      textAlign: "left",
                      transition: "all 0.15s",
                      width: "100%",
                      color: "var(--text)",
                    }}
                  >
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 700, fontSize: 14 }}>{c.name}</div>
                      <div
                        style={{
                          fontSize: 11,
                          color: "#64748b",
                          marginTop: 2,
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                          fontFamily: "monospace",
                        }}
                      >
                        {c.id}
                      </div>
                    </div>
                    {selectedId === c.id && (
                      <span style={{ color: "#60a5fa", fontSize: 12 }}>›</span>
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right column — details */}
        <div className="card">
          {!selected ? (
            <div
              style={{
                textAlign: "center",
                padding: "40px 24px",
                color: "#64748b",
              }}
            >
              <div style={{ fontSize: 36, marginBottom: 12 }}>👈</div>
              <div style={{ fontWeight: 600, color: "#94a3b8" }}>Select a class on the left</div>
              <div style={{ fontSize: 13, marginTop: 6 }}>to view details and manage members</div>
            </div>
          ) : (
            <div className="grid" style={{ gap: 16 }}>
              {/* Class name + ID */}
              <div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: 4,
                  }}
                >
                  <h3 style={{ margin: 0, fontSize: 20, fontWeight: 800 }}>{selected.name}</h3>
                  <div style={{ display: "flex", gap: 6, flexWrap: "wrap", justifyContent: "flex-end" }}>
                    <button
                      className="btn btn-solid"
                      style={{ fontSize: 12, padding: "6px 12px" }}
                      onClick={() => nav(`/exam/manage?classId=${encodeURIComponent(selected.id)}`)}
                    >
                      + Create exam
                    </button>
                    <button
                      className="btn"
                      style={{ fontSize: 12, padding: "5px 10px" }}
                      onClick={() => nav("/notifications")}
                    >
                      🔔 Notifications
                    </button>
                  </div>
                </div>
                <div
                  style={{
                    fontSize: 11,
                    color: "#64748b",
                    fontFamily: "monospace",
                  }}
                >
                  {selected.id}
                </div>
              </div>

              <div style={{ height: 1, background: "var(--border)" }} />

              {/* Join code */}
              <div>
                <SectionTitle>Join Code</SectionTitle>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <div
                    style={{
                      flex: 1,
                      background: "rgba(15,20,35,0.8)",
                      border: "1px solid var(--border-strong)",
                      borderRadius: 10,
                      padding: "10px 16px",
                      fontFamily: "monospace",
                      fontSize: 22,
                      fontWeight: 800,
                      letterSpacing: "0.2em",
                      color: "#60a5fa",
                      textAlign: "center",
                    }}
                  >
                    {selectedJoinCode || "—"}
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                    <button
                      className="btn btn-primary"
                      style={{ fontSize: 12 }}
                      onClick={onCopyJoinCode}
                      disabled={!selectedActionEnabled}
                    >
                      Copy
                    </button>
                    <button
                      className="btn"
                      style={{ fontSize: 12 }}
                      onClick={onRegenerateJoinCode}
                      disabled={!selectedActionEnabled || studentsLoading}
                    >
                      Regenerate
                    </button>
                  </div>
                </div>
              </div>

              {actionSuccess ? <div className="success">{actionSuccess}</div> : null}
              {actionError ? <div className="error">{actionError}</div> : null}

              <div style={{ height: 1, background: "var(--border)" }} />

              {/* Students */}
              <div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: 10,
                  }}
                >
                  <SectionTitle>Members</SectionTitle>
                  <span
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      color: "#60a5fa",
                      background: "rgba(59,130,246,.15)",
                      padding: "2px 9px",
                      borderRadius: 999,
                    }}
                  >
                    {students.length}
                  </span>
                </div>
                {studentsLoading ? (
                  <div style={{ fontSize: 13, color: "#64748b" }}>Loading…</div>
                ) : students.length === 0 ? (
                  <div
                    style={{
                      fontSize: 13,
                      color: "#64748b",
                      textAlign: "center",
                      padding: "14px",
                      border: "1px dashed var(--border)",
                      borderRadius: 8,
                    }}
                  >
                    No members yet
                  </div>
                ) : (
                  <div
                    style={{
                      maxHeight: 180,
                      overflowY: "auto",
                      display: "flex",
                      flexDirection: "column",
                      gap: 6,
                    }}
                  >
                    {students.map((s) => (
                      <div
                        key={s.userId}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 8,
                          padding: "7px 12px",
                          background: "var(--surface2)",
                          borderRadius: 8,
                          border: "1px solid var(--border)",
                          fontSize: 12,
                          fontFamily: "monospace",
                          color: "#94a3b8",
                        }}
                      >
                        <span
                          style={{
                            width: 6,
                            height: 6,
                            borderRadius: "50%",
                            background: "#34d399",
                            flexShrink: 0,
                          }}
                        />
                        <span style={{ display: "flex", flexDirection: "column", gap: 2, minWidth: 0 }}>
                          <span
                            style={{
                              color: "var(--text)",
                              fontWeight: 700,
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap",
                              maxWidth: 220,
                            }}
                          >
                            {studentProfiles[s.userId]?.username ||
                              studentProfiles[s.userId]?.email ||
                              "Member"}
                          </span>
                          {studentProfiles[s.userId]?.email && studentProfiles[s.userId]?.username ? (
                            <span
                              style={{
                                fontSize: 11,
                                color: "#64748b",
                                overflow: "hidden",
                                textOverflow: "ellipsis",
                                whiteSpace: "nowrap",
                                maxWidth: 220,
                              }}
                            >
                              {studentProfiles[s.userId]?.email}
                            </span>
                          ) : null}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div style={{ height: 1, background: "var(--border)" }} />

              {/* Exams in class (teacher convenience) */}
              <div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: 10,
                  }}
                >
                  <SectionTitle>Exams in class</SectionTitle>
                  <span
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      color: "#a78bfa",
                      background: "rgba(167,139,250,.15)",
                      padding: "2px 9px",
                      borderRadius: 999,
                    }}
                  >
                    {classExams.length}
                  </span>
                </div>

                {examsLoading ? (
                  <div style={{ fontSize: 13, color: "#64748b" }}>Loading exams…</div>
                ) : classExams.length === 0 ? (
                  <div style={{ fontSize: 13, color: "#64748b", padding: "12px 0" }}>
                    No exams in this class yet.
                    <div style={{ marginTop: 6 }}>
                      <Link className="btn" to="/exam/manage">
                        + Create exam
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                    {classExams.map((exam) => (
                      <div
                        key={exam.id}
                        style={{
                          padding: "10px 12px",
                          borderRadius: 10,
                          background: "rgba(30,37,54,0.35)",
                          border: "1px solid var(--border)",
                        }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                          <div style={{ minWidth: 0 }}>
                            <div style={{ fontWeight: 800, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                              {exam.title}
                            </div>
                            <div className="hint" style={{ marginTop: 4 }}>
                              Status: {exam.status ?? "DRAFT"} · Duration: {exam.duration}m
                            </div>
                          </div>
                          <div style={{ display: "flex", flexDirection: "column", gap: 6, alignItems: "flex-end" }}>
                            <div className="pill" style={{ borderColor: "rgba(148,163,184,0.22)" }}>
                              {exam.status === "PUBLISHED" ? "PUBLISHED" : "DRAFT"}
                            </div>
                            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" }}>
                              <Link className="btn" to="/exam/manage">
                                Manage
                              </Link>
                              {exam.id ? (
                                <Link className="btn" to={`/exam/reports/${exam.id}`}>
                                  View report
                                </Link>
                              ) : null}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div style={{ height: 1, background: "var(--border)" }} />

              {/* Add user */}
              <div>
                <SectionTitle>Add member</SectionTitle>
                <div style={{ display: "flex", gap: 8 }}>
                  <input
                    className="input"
                    value={lookupEmail}
                    onChange={(e) => setLookupEmail(e.target.value)}
                    placeholder="Student email"
                    style={{ flex: 1 }}
                    onKeyDown={(e) => e.key === "Enter" && onLookupUser()}
                  />
                  <button
                    className="btn btn-primary"
                    onClick={onLookupUser}
                    disabled={lookupLoading || !lookupEmail.trim()}
                    style={{ whiteSpace: "nowrap" }}
                  >
                    {lookupLoading ? "Search…" : "Search"}
                  </button>
                </div>

                {lookupUser ? (
                  <div
                    style={{
                      marginTop: 10,
                      padding: "10px 14px",
                      background: "rgba(16,185,129,.08)",
                      border: "1px solid rgba(16,185,129,.3)",
                      borderRadius: 8,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      gap: 10,
                    }}
                  >
                    <div>
                      <div style={{ fontSize: 12, color: "#64748b" }}>Found</div>
                      <div style={{ fontSize: 13, color: "var(--text)", fontWeight: 600 }}>
                        {lookupUser.userEmail}
                      </div>
                    </div>
                    <button
                      className="btn btn-solid"
                      onClick={onAddUserToClass}
                      disabled={!selectedActionEnabled || addUserLoading}
                      style={{ fontSize: 12 }}
                    >
                      {addUserLoading ? "Adding…" : "+ Add to class"}
                    </button>
                  </div>
                ) : null}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
