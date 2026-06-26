import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, parseApiError } from "../api/gateway";
import { ExamQuestion, ExamResult, getExamQuestions, getMyResults } from "../api/quiz";
import { QuestionReviewOptions } from "../ui/QuestionReviewOptions";
import { shortenId } from "../utils/display";

type ExamLite = {
  id: string;
  title?: string;
  classId?: string;
};

const OPTION_LABELS = ["A", "B", "C", "D"] as const;

function mapOriginalLetterToDisplay(question: ExamQuestion | undefined, originalLetter: string | undefined) {
  const normalizedOriginal = originalLetter?.trim().toUpperCase();
  if (!normalizedOriginal) return null;

  const optionKeys = question?.optionKeys;
  if (!optionKeys || optionKeys.length === 0) {
    return OPTION_LABELS.includes(normalizedOriginal as (typeof OPTION_LABELS)[number])
      ? normalizedOriginal
      : null;
  }

  const idx = optionKeys.findIndex((k) => k?.trim().toUpperCase() === normalizedOriginal);
  if (idx < 0 || idx >= OPTION_LABELS.length) return null;
  return OPTION_LABELS[idx];
}

export default function MyResultsPage() {
  const [items, setItems] = useState<ExamResult[]>([]);
  const [examMap, setExamMap] = useState<Record<string, ExamLite>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [examQuestionsByExamId, setExamQuestionsByExamId] = useState<Record<string, ExamQuestion[]>>({});
  const [loadingQuestionsForExamId, setLoadingQuestionsForExamId] = useState<string | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const data = await getMyResults();
      setItems(data);

      const ids = Array.from(new Set((data ?? []).map((x) => x.examId).filter(Boolean)));
      if (ids.length > 0) {
        const results = await Promise.allSettled(
          ids.map(async (examId) => {
            const res = await api.get<any, any>(`/api/exam/${examId}`);
            const payload = res.data?.data ?? res.data;
            return payload as ExamLite;
          }),
        );
        const map: Record<string, ExamLite> = {};
        for (const r of results) {
          if (r.status === "fulfilled" && r.value?.id) {
            map[r.value.id] = r.value;
          }
        }
        setExamMap(map);
      } else {
        setExamMap({});
      }
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined);
  }, []);

  async function toggleAnswerReview(row: ExamResult) {
    const revealed = row.revealedAnswers;
    if (!revealed?.length) return;
    if (expandedId === row.id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(row.id);
    if (examQuestionsByExamId[row.examId]) return;
    setLoadingQuestionsForExamId(row.examId);
    try {
      const qs = await getExamQuestions(row.examId);
      setExamQuestionsByExamId((prev) => ({ ...prev, [row.examId]: Array.isArray(qs) ? qs : [] }));
    } catch {
      setExamQuestionsByExamId((prev) => ({ ...prev, [row.examId]: [] }));
    } finally {
      setLoadingQuestionsForExamId(null);
    }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <div>
          <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>My results</h2>
          <p className="hint" style={{ marginTop: 4 }}>
            Your submitted exams and scores. Use <strong>Review</strong> to see correct answers when the exam allows
            it (requires score to be visible).
          </p>
        </div>
        <div className="row">
          <button className="btn" onClick={() => loadData().catch(() => undefined)}>
            Refresh
          </button>
          <Link className="btn" to="/exam/my-classes">
            Exams
          </Link>
        </div>
      </div>

      {loading ? (
        <div className="card">Loading…</div>
      ) : error ? (
        <div className="error">{error}</div>
      ) : items.length === 0 ? (
        <div className="card" style={{ color: "#94a3b8" }}>
          No results yet.
        </div>
      ) : (
        <div className="card" style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "#94a3b8" }}>
                <th style={{ padding: "8px 6px" }}>Exam</th>
                <th style={{ padding: "8px 6px" }}>Score</th>
                <th style={{ padding: "8px 6px" }}>Questions</th>
                <th style={{ padding: "8px 6px" }}>Violations</th>
                <th style={{ padding: "8px 6px" }}>Submitted</th>
                <th style={{ padding: "8px 6px" }}>Review</th>
              </tr>
            </thead>
            <tbody>
              {items.map((row) => {
                const canReview = (row.revealedAnswers?.length ?? 0) > 0;
                const qs = examQuestionsByExamId[row.examId] ?? [];
                const revealedMap = canReview
                  ? Object.fromEntries((row.revealedAnswers ?? []).map((r) => [r.questionId, r.correctAnswer] as const))
                  : null;
                return (
                  <React.Fragment key={row.id}>
                    <tr style={{ borderTop: "1px solid var(--border)" }}>
                      <td style={{ padding: "9px 6px", minWidth: 260 }}>
                        <div style={{ display: "flex", flexDirection: "column", gap: 2, minWidth: 0 }}>
                          <span
                            style={{
                              color: "var(--text)",
                              fontWeight: 700,
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap",
                            }}
                            title={examMap[row.examId]?.title || row.examId}
                          >
                            {examMap[row.examId]?.title || shortenId(row.examId)}
                          </span>
                        </div>
                      </td>
                      <td style={{ padding: "9px 6px" }}>{row.score ?? "Pending"}</td>
                      <td style={{ padding: "9px 6px" }}>{row.totalQuestions ?? "-"}</td>
                      <td style={{ padding: "9px 6px" }}>{row.violationCount}</td>
                      <td style={{ padding: "9px 6px" }}>
                        {row.submittedAt ? new Date(row.submittedAt).toLocaleString("en-US") : "-"}
                      </td>
                      <td style={{ padding: "9px 6px", whiteSpace: "nowrap" }}>
                        {canReview ? (
                          <button type="button" className="btn" onClick={() => toggleAnswerReview(row).catch(() => undefined)}>
                            {expandedId === row.id ? "Hide" : "Correct answers"}
                          </button>
                        ) : (
                          <span className="hint" style={{ fontSize: 12 }}>
                            —
                          </span>
                        )}
                      </td>
                    </tr>
                    {expandedId === row.id && canReview && revealedMap ? (
                      <tr style={{ borderTop: "none" }}>
                        <td colSpan={6} style={{ padding: "0 12px 14px", background: "rgba(15,20,35,0.35)" }}>
                          <div className="label" style={{ margin: "12px 0 8px" }}>
                            Review — all choices (correct option highlighted)
                          </div>
                          {loadingQuestionsForExamId === row.examId ? (
                            <div className="hint">Loading questions…</div>
                          ) : qs.length === 0 ? (
                            <div className="grid" style={{ gap: 8 }}>
                              {Object.entries(revealedMap).map(([qid, ans], i) => (
                                <div key={qid} className="hint">
                                  Q{i + 1} → <strong>{ans}</strong>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div className="grid" style={{ gap: 10 }}>
                              {qs.map((q, index) => {
                                const correct = revealedMap[q.questionId];
                                if (!correct) return null;
                                const correctDisplay = mapOriginalLetterToDisplay(q, correct);
                                return (
                                  <div
                                    key={q.questionId}
                                    className="card"
                                    style={{ padding: 12, background: "rgba(30,37,54,0.5)", boxShadow: "none" }}
                                  >
                                    <div style={{ fontWeight: 700, marginBottom: 6 }}>
                                      Q{index + 1}: {q.content}
                                    </div>
                                    <QuestionReviewOptions options={q.options} correctLetter={correctDisplay ?? correct} />
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </td>
                      </tr>
                    ) : null}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
