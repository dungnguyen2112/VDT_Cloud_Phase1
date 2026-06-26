import React, { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, parseApiError } from "../api/gateway";
import {
  attachQuestionToExam,
  attachQuestionsBulk,
  createExam,
  deleteExam,
  Exam,
  ExamQuestion,
  getExamQuestions,
  getQuestionBank,
  listExams,
  publishExam,
  Question,
  updateExam,
} from "../api/quiz";
import { classLabel } from "../utils/display";

type CreateExamForm = {
  title: string;
  classId: string;
  duration: number;
  availableFrom: string;
  availableUntil: string;
  maxAttempts: number;
  showCorrectAnswers: boolean;
  showScoreImmediately: boolean;
};

const defaultExamForm: CreateExamForm = {
  title: "",
  classId: "",
  duration: 45,
  availableFrom: "",
  availableUntil: "",
  maxAttempts: 1,
  showCorrectAnswers: false,
  showScoreImmediately: true,
};

function toLocalInputValue(date: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  const yyyy = date.getFullYear();
  const mm = pad(date.getMonth() + 1);
  const dd = pad(date.getDate());
  const hh = pad(date.getHours());
  const mi = pad(date.getMinutes());
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}

function isoToLocalInput(iso: string | null | undefined): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  return toLocalInputValue(d);
}

export default function ExamsManagePage() {
  const [searchParams] = useSearchParams();
  const classIdFromQuery = searchParams.get("classId") ?? "";

  const [exams, setExams] = useState<Exam[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [classOptions, setClassOptions] = useState<Array<{ id: string; name: string }>>([]);
  const [examQuestions, setExamQuestions] = useState<ExamQuestion[]>([]);
  const [examQuestionsLoading, setExamQuestionsLoading] = useState(false);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [publishingId, setPublishingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [form, setForm] = useState<CreateExamForm>(defaultExamForm);
  const [selectedExamId, setSelectedExamId] = useState<string>("");
  const [singleQuestionId, setSingleQuestionId] = useState<string>("");
  const [bulkQuestionIds, setBulkQuestionIds] = useState<string[]>([]);
  const [attachSearchKeyword, setAttachSearchKeyword] = useState("");
  const [attachCategoryFilter, setAttachCategoryFilter] = useState("");
  const [questionCategories, setQuestionCategories] = useState<string[]>([]);
  const [attachBankLoading, setAttachBankLoading] = useState(false);
  const [attaching, setAttaching] = useState(false);
  const [showAttachedQuestions, setShowAttachedQuestions] = useState(false);
  const [showExamList, setShowExamList] = useState(true);
  /** Collapsible sections — default closed to reduce clutter */
  const [openCreate, setOpenCreate] = useState(false);
  const [openEdit, setOpenEdit] = useState(false);
  const [openAttach, setOpenAttach] = useState(false);
  const [editForm, setEditForm] = useState<CreateExamForm | null>(null);
  const [updating, setUpdating] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const selectedExam = useMemo(
    () => exams.find((exam) => exam.id === selectedExamId) ?? null,
    [exams, selectedExamId],
  );
  const computedDurationMinutes = useMemo(() => {
    if (!form.availableFrom || !form.availableUntil) return null;
    const start = new Date(form.availableFrom);
    const end = new Date(form.availableUntil);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return null;
    const diffMin = Math.floor((end.getTime() - start.getTime()) / 60000);
    return diffMin > 0 ? diffMin : null;
  }, [form.availableFrom, form.availableUntil]);

  const editComputedDurationMinutes = useMemo(() => {
    if (!editForm) return null;
    if (!editForm.availableFrom || !editForm.availableUntil) return null;
    const start = new Date(editForm.availableFrom);
    const end = new Date(editForm.availableUntil);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return null;
    const diffMin = Math.floor((end.getTime() - start.getTime()) / 60000);
    return diffMin > 0 ? diffMin : null;
  }, [editForm?.availableFrom, editForm?.availableUntil]);

  useEffect(() => {
    if (!selectedExam) {
      setEditForm(null);
      return;
    }
    setEditForm({
      title: selectedExam.title,
      classId: selectedExam.classId,
      duration: selectedExam.duration,
      availableFrom: isoToLocalInput(selectedExam.availableFrom),
      availableUntil: isoToLocalInput(selectedExam.availableUntil),
      maxAttempts: selectedExam.maxAttempts ?? 1,
      showCorrectAnswers: selectedExam.showCorrectAnswers ?? false,
      showScoreImmediately: selectedExam.showScoreImmediately !== false,
    });
  }, [selectedExam]);

  async function loadQuestionBankForAttach(keyword = attachSearchKeyword, category = attachCategoryFilter) {
    setAttachBankLoading(true);
    try {
      const [questionData, categoryRes] = await Promise.all([
        getQuestionBank(keyword, category),
        api.get<any>(`/api/question/bank/categories`),
      ]);
      setQuestions(questionData);
      const categoryData = categoryRes.data?.data ?? categoryRes.data ?? [];
      setQuestionCategories(Array.isArray(categoryData) ? categoryData : []);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setAttachBankLoading(false);
    }
  }

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const examData = await listExams();
      setExams(examData);
      await loadQuestionBankForAttach();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    async function loadClasses() {
      try {
        const res = await api.get<any, any>(`/api/class/classes`);
        const data = res.data?.data ?? res.data;
        const list = Array.isArray(data) ? data : [];
        setClassOptions(list.map((c: any) => ({ id: c.id, name: c.name })));

        if (classIdFromQuery) {
          setForm((prev) => ({ ...prev, classId: classIdFromQuery }));
        } else if (!form.classId && list.length > 0) {
          // Default to first class so instructor can create exam quickly.
          setForm((prev) => ({ ...prev, classId: list[0].id }));
        }
      } catch {
        // Convenience only: if class dropdown fails, exam create still may work (user can paste classId later).
      }
    }
    loadClasses().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refreshExamQuestions(examId: string) {
    if (!examId) return;
    setExamQuestionsLoading(true);
    try {
      const data = await getExamQuestions(examId);
      setExamQuestions(Array.isArray(data) ? data : []);
    } catch {
      setExamQuestions([]);
    } finally {
      setExamQuestionsLoading(false);
    }
  }

  useEffect(() => {
    if (!selectedExamId) return;
    refreshExamQuestions(selectedExamId).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedExamId]);

  useEffect(() => {
    if (singleQuestionId && !questions.some((q) => q.id === singleQuestionId)) {
      setSingleQuestionId("");
    }
  }, [questions, singleQuestionId]);

  const onCreateExam = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const availableFromIso = form.availableFrom
        ? new Date(form.availableFrom).toISOString()
        : undefined;
      const availableUntilIso = form.availableUntil
        ? new Date(form.availableUntil).toISOString()
        : undefined;
      const now = new Date();

      if (availableFromIso && availableUntilIso && new Date(availableFromIso) >= new Date(availableUntilIso)) {
        setError("End time must be after start time.");
        return;
      }
      if (availableFromIso && new Date(availableFromIso) < now) {
        setError("Start time cannot be in the past.");
        return;
      }
      if (availableUntilIso && new Date(availableUntilIso) < now) {
        setError("End time cannot be in the past.");
        return;
      }
      const durationFromWindow = computedDurationMinutes ?? Number(form.duration);
      if (!durationFromWindow || durationFromWindow <= 0) {
        setError("Duration must be greater than 0 minutes.");
        return;
      }

      const created = await createExam({
        title: form.title.trim(),
        classId: form.classId.trim(),
        duration: durationFromWindow,
        availableFrom: availableFromIso,
        availableUntil: availableUntilIso,
        maxAttempts: Number(form.maxAttempts),
        showCorrectAnswers: form.showCorrectAnswers,
        showScoreImmediately: form.showScoreImmediately,
      });
      setSuccess("Exam created.");
      setSelectedExamId(created.id);
      // Keep the selected class so user can create multiple exams quickly.
      setForm((prev) => ({ ...defaultExamForm, classId: prev.classId }));
      await loadData();
      await refreshExamQuestions(created.id);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSaving(false);
    }
  };

  const onPublish = async (examId: string) => {
    setPublishingId(examId);
    setError(null);
    setSuccess(null);
    try {
      // Avoid the student 400: "Exam has no questions yet"
      const qs = await getExamQuestions(examId);
      if (!qs || qs.length === 0) {
        setError("This exam has no questions. Attach questions before publishing.");
        return;
      }

      await publishExam(examId);
      setSuccess("Exam published.");
      await loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setPublishingId(null);
    }
  };

  const onAttachSingle = async () => {
    if (!selectedExamId || !singleQuestionId) {
      setError("Select an exam and a question before attaching.");
      return;
    }
    setAttaching(true);
    setError(null);
    setSuccess(null);
    try {
      await attachQuestionToExam(selectedExamId, singleQuestionId);
      setSuccess("Question attached.");
      setSingleQuestionId("");
      await loadData();
      await refreshExamQuestions(selectedExamId);
      setShowAttachedQuestions(false);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setAttaching(false);
    }
  };

  const onToggleBulk = (questionId: string) => {
    setBulkQuestionIds((prev) =>
      prev.includes(questionId) ? prev.filter((id) => id !== questionId) : [...prev, questionId],
    );
  };

  const onAttachBulk = async () => {
    if (!selectedExamId || bulkQuestionIds.length === 0) {
      setError("Select an exam and at least one question.");
      return;
    }
    setAttaching(true);
    setError(null);
    setSuccess(null);
    try {
      await attachQuestionsBulk(selectedExamId, bulkQuestionIds);
      setSuccess("Questions attached.");
      setBulkQuestionIds([]);
      await loadData();
      await refreshExamQuestions(selectedExamId);
      setShowAttachedQuestions(false);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setAttaching(false);
    }
  };

  const onUpdateSelectedExam = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedExamId || !editForm) {
      setError("Select an exam to edit.");
      return;
    }
    setUpdating(true);
    setError(null);
    setSuccess(null);
    try {
      const hasFrom = !!editForm.availableFrom?.trim();
      const hasUntil = !!editForm.availableUntil?.trim();
      if (hasFrom !== hasUntil) {
        setError("Set both start and end times, or leave both empty.");
        return;
      }
      if (hasFrom && hasUntil) {
        const availableFromIso = new Date(editForm.availableFrom).toISOString();
        const availableUntilIso = new Date(editForm.availableUntil).toISOString();
        if (new Date(availableFromIso) >= new Date(availableUntilIso)) {
          setError("End time must be after start time.");
          return;
        }
      }
      const durationFromWindow = editComputedDurationMinutes ?? Number(editForm.duration);
      if (!durationFromWindow || durationFromWindow <= 0) {
        setError("Duration must be greater than 0 minutes.");
        return;
      }

      const availableFromIso = editForm.availableFrom
        ? new Date(editForm.availableFrom).toISOString()
        : undefined;
      const availableUntilIso = editForm.availableUntil
        ? new Date(editForm.availableUntil).toISOString()
        : undefined;

      await updateExam(selectedExamId, {
        title: editForm.title.trim(),
        duration: durationFromWindow,
        ...(availableFromIso && availableUntilIso
          ? { availableFrom: availableFromIso, availableUntil: availableUntilIso }
          : {}),
        maxAttempts: Number(editForm.maxAttempts),
        showCorrectAnswers: editForm.showCorrectAnswers,
        showScoreImmediately: editForm.showScoreImmediately,
      });
      setSuccess("Exam updated.");
      await loadData();
      await refreshExamQuestions(selectedExamId);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setUpdating(false);
    }
  };

  const deleteDraftExam = async (exam: Exam) => {
    if (exam.status !== "DRAFT") return;
    if (!window.confirm(`Delete draft exam "${exam.title}"? This cannot be undone.`)) return;
    setDeleting(true);
    setError(null);
    setSuccess(null);
    try {
      await deleteExam(exam.id);
      setSuccess("Exam deleted.");
      if (selectedExamId === exam.id) {
        setSelectedExamId("");
        setEditForm(null);
      }
      await loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setDeleting(false);
    }
  };

  const onDeleteSelectedExam = async () => {
    if (!selectedExam) return;
    await deleteDraftExam(selectedExam);
  };

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div>
        <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>Manage exams</h2>
        <p className="hint" style={{ marginTop: 4 }}>
          Create exams, publish, attach questions, view reports and export CSV. Only <strong>draft</strong> exams can be
          deleted.
        </p>
      </div>

      {error ? <div className="error">{error}</div> : null}
      {success ? <div className="success">{success}</div> : null}

      <div className="grid grid-2" style={{ alignItems: "start", gap: 16 }}>
        <div className="grid" style={{ gap: 14 }}>
          <div className="card">
            <div className="label" style={{ marginBottom: 8 }}>Exam for edit &amp; attach</div>
            <select
              className="input"
              value={selectedExamId}
              onChange={(e) => setSelectedExamId(e.target.value)}
            >
              <option value="">— Select an exam —</option>
              {exams.map((exam) => (
                <option key={exam.id} value={exam.id}>
                  {exam.title} ({exam.status})
                </option>
              ))}
            </select>
            <p className="hint" style={{ marginTop: 8, marginBottom: 0 }}>
              Choose here before <strong>Edit</strong> or <strong>Attach questions</strong>. You can also use <strong>Select</strong> on an exam in the list.
            </p>
          </div>

          <div className="card" style={{ padding: 0, overflow: "hidden" }}>
            <button
              type="button"
              className="exam-panel-toggle"
              onClick={() => setOpenCreate((v) => !v)}
              style={{
                padding: "14px 20px",
                background: "rgba(255,255,255,0.04)",
                border: "none",
                borderBottom: openCreate ? "1px solid rgba(148,163,184,0.12)" : "none",
                cursor: "pointer",
                color: "var(--text)",
              }}
            >
              <span className="label" style={{ marginBottom: 0 }}>
                <span style={{ fontWeight: 800, marginRight: 8 }}>{openCreate ? "−" : "+"}</span>
                Create exam
              </span>
            </button>
            {openCreate ? (
              <form
                noValidate
                className="grid"
                style={{ gap: 10, padding: "16px 20px 20px", boxSizing: "border-box" }}
                onSubmit={onCreateExam}
              >
              <input
                className="input"
                placeholder="Exam title"
                value={form.title}
                onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
              />
              <select
                className="input"
                value={form.classId}
                onChange={(e) => setForm((prev) => ({ ...prev, classId: e.target.value }))}
              >
                <option value="">Select class</option>
                {classOptions.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              <input
                className="input"
                type="number"
                min={1}
                placeholder="Duration (minutes)"
                value={computedDurationMinutes ?? form.duration}
                onChange={(e) => setForm((prev) => ({ ...prev, duration: Number(e.target.value) }))}
                disabled={!!computedDurationMinutes}
                title={
                  computedDurationMinutes
                    ? "Duration is derived from start/end window"
                    : "Enter manually if start/end are not set"
                }
              />
              <div className="grid grid-2" style={{ gap: 10 }}>
                <input
                  className="input"
                  type="datetime-local"
                  min={toLocalInputValue(new Date())}
                  value={form.availableFrom}
                  onChange={(e) => setForm((prev) => ({ ...prev, availableFrom: e.target.value }))}
                  title="Start time"
                />
                <input
                  className="input"
                  type="datetime-local"
                  min={form.availableFrom || toLocalInputValue(new Date())}
                  value={form.availableUntil}
                  onChange={(e) => setForm((prev) => ({ ...prev, availableUntil: e.target.value }))}
                  title="End time"
                />
              </div>
              {computedDurationMinutes ? (
                <div className="hint">Auto duration from window: {computedDurationMinutes} min</div>
              ) : null}
              <input
                className="input"
                type="number"
                min={1}
                placeholder="Max attempts"
                value={form.maxAttempts}
                onChange={(e) => setForm((prev) => ({ ...prev, maxAttempts: Number(e.target.value) }))}
              />

              <label className="row" style={{ justifyContent: "space-between" }}>
                <span>Show correct answers</span>
                <input
                  type="checkbox"
                  checked={form.showCorrectAnswers}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, showCorrectAnswers: e.target.checked }))
                  }
                />
              </label>

              <label className="row" style={{ justifyContent: "space-between" }}>
                <span>Show score immediately after submit</span>
                <input
                  type="checkbox"
                  checked={form.showScoreImmediately}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, showScoreImmediately: e.target.checked }))
                  }
                />
              </label>
              <div className="hint" style={{ marginTop: -4 }}>
                Students see score and (if enabled) correct answers after submit and under{" "}
                <strong>My results → Correct answers</strong>, only when <strong>Show score immediately</strong> is on.
              </div>

              <button className="btn btn-solid" type="submit" disabled={saving}>
                  {saving ? "Creating…" : "Create exam"}
              </button>
            </form>
            ) : null}
          </div>

          <div className="card" style={{ padding: 0, overflow: "hidden" }}>
            <button
              type="button"
              className="exam-panel-toggle"
              onClick={() => setOpenEdit((v) => !v)}
              style={{
                padding: "14px 20px",
                background: "rgba(255,255,255,0.04)",
                border: "none",
                borderBottom: openEdit ? "1px solid rgba(148,163,184,0.12)" : "none",
                cursor: "pointer",
                color: "var(--text)",
              }}
            >
              <span className="label" style={{ marginBottom: 0 }}>
                <span style={{ fontWeight: 800, marginRight: 8 }}>{openEdit ? "−" : "+"}</span>
                Edit exam
              </span>
            </button>
            {openEdit ? (
              <div style={{ padding: "16px 20px 20px", boxSizing: "border-box", minWidth: 0 }}>
            {!selectedExamId || !editForm ? (
              <div className="hint">
                Select an exam in <strong>Exam for edit &amp; attach</strong> above, or use <strong>Select for edit</strong> in the list.
              </div>
            ) : (
              <form noValidate className="grid" style={{ gap: 10 }} onSubmit={onUpdateSelectedExam}>
                <div className="hint" style={{ marginBottom: 0 }}>
                  Status: {selectedExam?.status ?? "—"} — class cannot be changed here.
                </div>
                <input
                  className="input"
                  placeholder="Exam title"
                  value={editForm.title}
                  onChange={(e) => setEditForm((prev) => (prev ? { ...prev, title: e.target.value } : prev))}
                />
                <input
                  className="input"
                  readOnly
                  value={`Class: ${classLabel(editForm.classId, classOptions)}`}
                  title="Class (read-only)"
                />
                <input
                  className="input"
                  type="number"
                  min={1}
                  placeholder="Duration (minutes)"
                  value={editComputedDurationMinutes ?? editForm.duration}
                  onChange={(e) =>
                    setEditForm((prev) =>
                      prev ? { ...prev, duration: Number(e.target.value) } : prev,
                    )
                  }
                  disabled={!!editComputedDurationMinutes}
                  title={
                    editComputedDurationMinutes
                      ? "Duration is derived from start/end window"
                      : "Enter manually if start/end are not set"
                  }
                />
                <div className="grid grid-2" style={{ gap: 10 }}>
                  <input
                    className="input"
                    type="datetime-local"
                    value={editForm.availableFrom}
                    onChange={(e) =>
                      setEditForm((prev) => (prev ? { ...prev, availableFrom: e.target.value } : prev))
                    }
                    title="Start time (optional if no exam window)"
                  />
                  <input
                    className="input"
                    type="datetime-local"
                    value={editForm.availableUntil}
                    onChange={(e) =>
                      setEditForm((prev) => (prev ? { ...prev, availableUntil: e.target.value } : prev))
                    }
                    title="End time (optional if no exam window)"
                  />
                </div>
                {editComputedDurationMinutes ? (
                  <div className="hint">Auto duration from window: {editComputedDurationMinutes} min</div>
                ) : null}
                <input
              className="input"
                  type="number"
                  min={1}
                  placeholder="Max attempts"
                  value={editForm.maxAttempts}
                  onChange={(e) =>
                    setEditForm((prev) =>
                      prev ? { ...prev, maxAttempts: Number(e.target.value) } : prev,
                    )
                  }
                />
                <label className="row" style={{ justifyContent: "space-between" }}>
                  <span>Show correct answers</span>
                  <input
                    type="checkbox"
                    checked={editForm.showCorrectAnswers}
                    onChange={(e) =>
                      setEditForm((prev) =>
                        prev ? { ...prev, showCorrectAnswers: e.target.checked } : prev,
                      )
                    }
                  />
                </label>
                <label className="row" style={{ justifyContent: "space-between" }}>
                  <span>Show score immediately after submit</span>
                  <input
                    type="checkbox"
                    checked={editForm.showScoreImmediately}
                    onChange={(e) =>
                      setEditForm((prev) =>
                        prev ? { ...prev, showScoreImmediately: e.target.checked } : prev,
                      )
                    }
                  />
                </label>
                <div className="hint" style={{ marginTop: -4 }}>
                  Students see score and (if enabled) correct answers after submit and under{" "}
                  <strong>My results → Correct answers</strong>, only when <strong>Show score immediately</strong> is on.
                </div>
                <div className="row" style={{ gap: 10, flexWrap: "wrap" }}>
                  <button className="btn btn-solid" type="submit" disabled={updating}>
                    {updating ? "Saving…" : "Save changes"}
                  </button>
                  {selectedExam?.status === "DRAFT" ? (
                    <button
                      type="button"
                      className="btn"
                      onClick={() => onDeleteSelectedExam().catch(() => undefined)}
                      disabled={deleting}
                    >
                      {deleting ? "Deleting…" : "Delete draft"}
                    </button>
                  ) : null}
                </div>
              </form>
            )}
              </div>
            ) : null}
          </div>

          <div className="card" style={{ padding: 0, overflow: "hidden" }}>
            <button
              type="button"
              className="exam-panel-toggle"
              onClick={() => setOpenAttach((v) => !v)}
              style={{
                padding: "14px 20px",
                background: "rgba(255,255,255,0.04)",
                border: "none",
                borderBottom: openAttach ? "1px solid rgba(148,163,184,0.12)" : "none",
                cursor: "pointer",
                color: "var(--text)",
              }}
            >
              <span className="label" style={{ marginBottom: 0 }}>
                <span style={{ fontWeight: 800, marginRight: 8 }}>{openAttach ? "−" : "+"}</span>
                Attach questions
              </span>
            </button>
            {openAttach ? (
              <div className="grid" style={{ gap: 14, padding: "16px 20px 20px", boxSizing: "border-box", minWidth: 0 }}>
            {!selectedExamId ? (
              <div className="hint">Select an exam in <strong>Exam for edit &amp; attach</strong> above first.</div>
            ) : (
              <>
            <div className="hint" style={{ marginBottom: 0 }}>
              Attaching to: <strong>{selectedExam?.title ?? selectedExamId}</strong>
            </div>

            <div className="divider" />

            <div className="label" style={{ marginBottom: 8 }}>Question bank (search and category)</div>
            <div className="grid" style={{ gap: 8 }}>
              <input
                className="input"
                placeholder="Search question text"
                value={attachSearchKeyword}
                onChange={(e) => setAttachSearchKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    loadQuestionBankForAttach(attachSearchKeyword, attachCategoryFilter).catch(() => undefined);
                  }
                }}
              />
              <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                <select
                  className="input"
                  style={{ flex: "1 1 160px", minWidth: 140 }}
                  value={attachCategoryFilter}
                  onChange={(e) => setAttachCategoryFilter(e.target.value)}
                >
                  <option value="">All categories</option>
                  {questionCategories.map((c) => (
                    <option key={c} value={c}>
                      {c}
                </option>
              ))}
            </select>
                <button
                  type="button"
                  className="btn"
                  disabled={attachBankLoading}
                  onClick={() => loadQuestionBankForAttach(attachSearchKeyword, attachCategoryFilter).catch(() => undefined)}
                >
                  {attachBankLoading ? "Loading…" : "Apply filters"}
                </button>
              </div>
              <div className="hint" style={{ marginTop: 0 }}>
                {attachBankLoading ? "Loading questions…" : `${questions.length} question(s) in this view.`}
              </div>
            </div>

            <div className="divider" />

            <div className="label" style={{ marginBottom: 8 }}>Attach one question</div>
            <div className="row">
              <select
                className="input"
                value={singleQuestionId}
                onChange={(e) => setSingleQuestionId(e.target.value)}
                style={{ flex: 1 }}
                disabled={attachBankLoading || questions.length === 0}
              >
                <option value="">{attachBankLoading ? "Loading…" : "Select question"}</option>
                {questions.map((q) => (
                  <option key={q.id} value={q.id}>
                    [{q.category || "GENERAL"}] {q.content.slice(0, 60)}
                    {q.content.length > 60 ? "…" : ""}
                  </option>
                ))}
              </select>
              <button className="btn" onClick={onAttachSingle} disabled={attaching || attachBankLoading}>
                Add
              </button>
            </div>

            <div className="divider" />

            <div className="label" style={{ marginBottom: 8 }}>Attach multiple questions</div>
            <div className="grid" style={{ maxHeight: 220, overflow: "auto", paddingRight: 4 }}>
              {attachBankLoading ? (
                <div className="hint">Loading questions…</div>
              ) : questions.length === 0 ? (
                <div className="hint">No questions match the current filters.</div>
              ) : (
                questions.map((q) => (
                <label key={q.id} className="row" style={{ alignItems: "flex-start" }}>
                  <input
                    type="checkbox"
                    checked={bulkQuestionIds.includes(q.id)}
                    onChange={() => onToggleBulk(q.id)}
                      disabled={attachBankLoading}
                    />
                    <span style={{ fontSize: 13 }}>
                      <span className="pill" style={{ marginRight: 6, fontSize: 11 }}>
                        {q.category || "GENERAL"}
                      </span>
                      {q.content}
                    </span>
                </label>
                ))
              )}
            </div>
            <button className="btn" onClick={onAttachBulk} disabled={attaching || bulkQuestionIds.length === 0}>
              {attaching ? "Attaching…" : `Attach ${bulkQuestionIds.length} question(s)`}
            </button>
              </>
            )}
              </div>
            ) : null}
          </div>
        </div>

        <div className="card">
          <div className="row" style={{ justifyContent: "space-between", marginBottom: 10 }}>
            <div className="label">Exam list</div>
            <div className="row">
              <button className="btn" onClick={() => setShowExamList((v) => !v)}>
                {showExamList ? "Hide list" : "Show list"}
              </button>
            <button className="btn" onClick={() => loadData().catch(() => undefined)}>
                Refresh
            </button>
            </div>
          </div>

          {!showExamList ? (
            <div className="hint">Exam list is hidden.</div>
          ) : loading ? (
            <div className="hint">Loading…</div>
          ) : exams.length === 0 ? (
            <div className="hint">No exams yet.</div>
          ) : (
            <div className="grid" style={{ gap: 10 }}>
              {exams.map((exam) => (
                <div key={exam.id} className="card" style={{ padding: 14, background: "rgba(30,37,54,0.35)" }}>
                  <div style={{ fontWeight: 700 }}>{exam.title}</div>
                  <div className="row" style={{ marginTop: 8 }}>
                    <span className="pill">Status: {exam.status}</span>
                    <span className="pill">Duration: {exam.duration}m</span>
                    <span className="pill">Attempts: {exam.maxAttempts ?? 1}</span>
                  </div>
                  <div className="hint" style={{ marginTop: 8 }}>
                    Class: {classLabel(exam.classId, classOptions)}
                  </div>
                  <div className="row" style={{ marginTop: 10, flexWrap: "wrap" }}>
                    <button
                      type="button"
                      className="btn"
                      onClick={() => {
                        setSelectedExamId(exam.id);
                        setOpenEdit(true);
                      }}
                    >
                      Select for edit
                    </button>
                    {exam.status === "DRAFT" ? (
                      <button
                        type="button"
                        className="btn btn-danger"
                        onClick={() => deleteDraftExam(exam).catch(() => undefined)}
                        disabled={deleting}
                      >
                        {deleting ? "Deleting…" : "Delete draft"}
                      </button>
                    ) : null}
                    {exam.status !== "PUBLISHED" ? (
                      <button
                        className="btn btn-solid"
                        onClick={() => onPublish(exam.id)}
                        disabled={publishingId === exam.id}
                      >
                        {publishingId === exam.id ? "Publishing…" : "Publish"}
                      </button>
                    ) : null}
                    <Link className="btn" to={`/exam/reports/${exam.id}`}>
                      View report
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}

          {selectedExamId ? (
            <div style={{ marginTop: 14 }}>
              <div className="row" style={{ justifyContent: "space-between", marginBottom: 8 }}>
                <div className="label" style={{ marginBottom: 0 }}>
                  Questions attached to exam
                </div>
                <button
                  type="button"
                  className="btn"
                  onClick={() => setShowAttachedQuestions((v) => !v)}
                >
                  {showAttachedQuestions ? "Hide list" : "Show list"}
                </button>
              </div>

              {!showAttachedQuestions ? (
                <div className="hint">Attached questions are hidden (use Show list).</div>
              ) : examQuestionsLoading ? (
                <div className="hint">Loading questions…</div>
              ) : examQuestions.length === 0 ? (
                <div className="hint">No questions attached.</div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                  {examQuestions.map((q, index) => (
                    <div
                      key={q.questionId}
                      className="card"
                      style={{ padding: 12, background: "rgba(15,20,35,0.45)", boxShadow: "none" }}
                    >
                      <div style={{ fontWeight: 800, fontSize: 13 }}>Question {index + 1}</div>
                      <div style={{ marginTop: 6, color: "var(--text)", fontSize: 13 }}>{q.content}</div>
                      <div className="hint" style={{ marginTop: 4 }}>
                        Options: {q.options.length}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : null}

          {selectedExam ? (
            <div className="hint" style={{ marginTop: 12 }}>
              Working on exam: {selectedExam.title}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
