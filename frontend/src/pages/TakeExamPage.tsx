import React, { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { parseApiError } from "../api/gateway";
import {
  ExamQuestion,
  ExamResult,
  ExamStart,
  getExamQuestions,
  reportViolation,
  startExam,
  submitExam,
} from "../api/quiz";
import { QuestionReviewOptions } from "../ui/QuestionReviewOptions";

const SAME_EVENT_DEDUP_MS = 1200;
const OPTION_LABELS = ["A", "B", "C", "D"] as const;

function mapDisplayLetterToOriginal(question: ExamQuestion | undefined, displayLetter: string | undefined) {
  const normalizedDisplay = displayLetter?.trim().toUpperCase();
  if (!normalizedDisplay) return null;
  const displayIdx = OPTION_LABELS.indexOf(normalizedDisplay as (typeof OPTION_LABELS)[number]);
  if (displayIdx < 0) return null;

  const optionKeys = question?.optionKeys;
  if (!optionKeys || optionKeys.length <= displayIdx) {
    return normalizedDisplay;
  }

  const original = optionKeys[displayIdx]?.trim().toUpperCase();
  if (!original) return normalizedDisplay;
  return OPTION_LABELS.includes(original as (typeof OPTION_LABELS)[number]) ? original : normalizedDisplay;
}

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

function formatSeconds(totalSeconds: number) {
  const safe = Math.max(totalSeconds, 0);
  const m = Math.floor(safe / 60);
  const s = safe % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
}

async function requestFullscreenMode() {
  const el = document.documentElement as HTMLElement & {
    webkitRequestFullscreen?: () => Promise<void> | void;
    msRequestFullscreen?: () => Promise<void> | void;
  };

  if (document.fullscreenElement) return;
  if (el.requestFullscreen) {
    await el.requestFullscreen();
    return;
  }
  if (el.webkitRequestFullscreen) {
    await el.webkitRequestFullscreen();
    return;
  }
  if (el.msRequestFullscreen) {
    await el.msRequestFullscreen();
  }
}

export default function TakeExamPage() {
  const { examId } = useParams();

  const [startData, setStartData] = useState<ExamStart | null>(null);
  const [questions, setQuestions] = useState<ExamQuestion[]>([]);
  const [answers, setAnswers] = useState<Record<string, string>>({});

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitResult, setSubmitResult] = useState<ExamResult | null>(null);

  const [violationCount, setViolationCount] = useState<number>(0);
  const [secondsLeft, setSecondsLeft] = useState<number>(0);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(!!document.fullscreenElement);
  const [fullscreenWarning, setFullscreenWarning] = useState<string | null>(null);

  const lastViolationReportRef = useRef(0);
  const reportingRef = useRef(false);
  const lastViolationTypeRef = useRef<"tab_hidden" | "fullscreen_exit" | null>(null);
  const autoSubmittedRef = useRef(false);

  const pendingScore = useMemo(() => {
    if (!submitResult) return false;
    return submitResult.score === null || submitResult.totalQuestions === null;
  }, [submitResult]);

  const revealedByQuestionId = useMemo(() => {
    const list = submitResult?.revealedAnswers;
    if (!list?.length) return null;
    return Object.fromEntries(list.map((r) => [r.questionId, r.correctAnswer] as const));
  }, [submitResult]);

  const answeredCount = useMemo(
    () => Object.keys(answers).filter((k) => answers[k]).length,
    [answers],
  );

  const questionsById = useMemo(
    () => Object.fromEntries(questions.map((q) => [q.questionId, q] as const)),
    [questions],
  );

  useEffect(() => {
    async function boot() {
      if (!examId) return;
      setLoading(true);
      setError(null);
      try {
        const [startRes, questionRes] = await Promise.all([startExam(examId), getExamQuestions(examId)]);
        setStartData(startRes);
        setQuestions(questionRes);

        const serverTime = new Date(startRes.serverTime).getTime();
        const deadlineAt = new Date(startRes.deadlineAt).getTime();
        const offsetMs = serverTime - Date.now();

        const updateClock = () => {
          const nowServerMs = Date.now() + offsetMs;
          const leftSec = Math.max(0, Math.floor((deadlineAt - nowServerMs) / 1000));
          setSecondsLeft(leftSec);
        };

        updateClock();
        const timer = window.setInterval(updateClock, 1000);
        return () => window.clearInterval(timer);
      } catch (err) {
        setError(parseApiError(err).message);
      } finally {
        setLoading(false);
      }
    }

    let cleanup: (() => void) | void;
    boot()
      .then((fn) => {
        cleanup = fn;
      })
      .catch(() => undefined);

    return () => {
      if (cleanup) cleanup();
    };
  }, [examId]);

  useEffect(() => {
    if (!examId || !startData || submitResult) return;

    const tryReport = async (type: "tab_hidden" | "fullscreen_exit") => {
      const now = Date.now();
      const isSameType = lastViolationTypeRef.current === type;
      const tooSoon = now - lastViolationReportRef.current < SAME_EVENT_DEDUP_MS;
      if (reportingRef.current || (isSameType && tooSoon)) {
        return;
      }
      reportingRef.current = true;
      lastViolationReportRef.current = now;
      lastViolationTypeRef.current = type;
      try {
        const apiType = type === "tab_hidden" ? "TAB_HIDDEN" : "FULLSCREEN_EXIT";
        const violation = await reportViolation(examId, apiType);
        setViolationCount(violation.violationCount);
      } catch {
        // Best effort by requirement: never block submit flow if reporting fails.
      } finally {
        reportingRef.current = false;
      }
    };

    const onVisibilityChange = () => {
      if (document.hidden) {
        tryReport("tab_hidden").catch(() => undefined);
      }
    };

    const onFullscreenChange = () => {
      const active = !!document.fullscreenElement;
      setIsFullscreen(active);
      if (!active) {
        setFullscreenWarning("You left fullscreen. Return to fullscreen to continue the exam.");
        tryReport("fullscreen_exit").catch(() => undefined);
      } else {
        setFullscreenWarning(null);
      }
    };

    const onCopy = (e: ClipboardEvent) => {
      e.preventDefault();
    };

    const onContextMenu = (e: MouseEvent) => {
      e.preventDefault();
    };

    const onSelectStart = (e: Event) => {
      e.preventDefault();
    };

    const onKeyDown = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      const meta = e.ctrlKey || e.metaKey;
      // Block common copy shortcuts and force awareness when trying to exit fullscreen.
      if ((meta && key === "c") || (meta && key === "x") || key === "f12") {
        e.preventDefault();
      }
      if (key === "escape" && document.fullscreenElement) {
        setFullscreenWarning("Do not exit fullscreen while taking the exam.");
      }
      if (key === "f11") {
        // Some browsers toggle fullscreen with F11 without reliably firing fullscreenchange.
        // Re-check shortly after key press and report if fullscreen is gone.
        window.setTimeout(() => {
          if (!document.fullscreenElement) {
            setIsFullscreen(false);
            setFullscreenWarning("You left fullscreen (F11). Return to fullscreen to continue.");
            tryReport("fullscreen_exit").catch(() => undefined);
          }
        }, 250);
      }
    };

    document.addEventListener("visibilitychange", onVisibilityChange);
    document.addEventListener("fullscreenchange", onFullscreenChange);
    document.addEventListener("copy", onCopy);
    document.addEventListener("contextmenu", onContextMenu);
    document.addEventListener("selectstart", onSelectStart);
    window.addEventListener("keydown", onKeyDown);

    // Force fullscreen when the student starts taking exam.
    requestFullscreenMode()
      .then(() => setIsFullscreen(!!document.fullscreenElement))
      .catch(() =>
        setFullscreenWarning("The browser blocked automatic fullscreen. Use Return to fullscreen."),
      );

    return () => {
      document.removeEventListener("visibilitychange", onVisibilityChange);
      document.removeEventListener("fullscreenchange", onFullscreenChange);
      document.removeEventListener("copy", onCopy);
      document.removeEventListener("contextmenu", onContextMenu);
      document.removeEventListener("selectstart", onSelectStart);
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [examId, startData, submitResult]);

  const onSelect = (questionId: string, answer: string) => {
    setAnswers((prev) => ({ ...prev, [questionId]: answer }));
  };

  const onSubmit = async () => {
    if (!examId) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const normalizedAnswers = Object.fromEntries(
        Object.entries(answers).map(([questionId, displayLetter]) => {
          const originalLetter = mapDisplayLetterToOriginal(questionsById[questionId], displayLetter);
          return [questionId, originalLetter ?? displayLetter];
        }),
      );

      const idempotencyKey = `${examId}-${crypto.randomUUID()}`;
      const result = await submitExam({ examId, answers: normalizedAnswers, idempotencyKey });
      setSubmitResult(result);
      setViolationCount(result.violationCount ?? violationCount);
    } catch (err) {
      setSubmitError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    if (!startData || submitResult || submitting) return;
    if (secondsLeft > 0) return;
    if (autoSubmittedRef.current) return;
    autoSubmittedRef.current = true;
    onSubmit().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [secondsLeft, startData, submitResult, submitting]);

  if (!examId) {
    return <div className="error">Invalid exam id.</div>;
  }

  if (loading) {
    return <div className="card">Starting exam…</div>;
  }

  if (error) {
    return <div className="error">{error}</div>;
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      {!isFullscreen ? (
        <div className="card" style={{ borderColor: "var(--danger-border)" }}>
          <div style={{ fontWeight: 800, color: "var(--danger-text)", marginBottom: 8 }}>
            Yeu cau toan man hinh
          </div>
          <div className="hint" style={{ marginBottom: 10 }}>
            {fullscreenWarning ?? "Ban phai bat toan man hinh de tiep tuc bai thi."}
          </div>
          <button
            className="btn btn-solid"
            type="button"
            onClick={() => {
              requestFullscreenMode()
                .then(() => setIsFullscreen(!!document.fullscreenElement))
                .catch(() => setFullscreenWarning("Could not enter fullscreen. Try again."));
            }}
          >
            Return to fullscreen
          </button>
        </div>
      ) : null}

      <div className="card" style={{ position: "sticky", top: 68, zIndex: 8 }}>
        <div className="row" style={{ justifyContent: "space-between" }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: 18 }}>{startData?.exam.title}</div>
            <div className="hint">Exam ID: {examId}</div>
          </div>
          <div className="row">
            <span className="pill">Con lai: {formatSeconds(secondsLeft)}</span>
            <span className="pill">Da tra loi: {answeredCount}/{questions.length}</span>
            <span className="pill">Violations: {violationCount}</span>
          </div>
        </div>
      </div>

      {questions.map((question, index) => (
        <div key={question.questionId} className="card">
          <div style={{ fontWeight: 700, marginBottom: 10 }}>
            Q{index + 1}: {question.content}
          </div>
          <div className="grid" style={{ gap: 8 }}>
            {question.options.map((option, optionIndex) => {
              const optionLabel = OPTION_LABELS[optionIndex] ?? `${optionIndex + 1}`;
              const checked = answers[question.questionId] === optionLabel;
              return (
                <label key={`${question.questionId}-${optionLabel}`} className="row" style={{ alignItems: "center" }}>
                  <input
                    type="radio"
                    name={question.questionId}
                    checked={checked}
                    onChange={() => onSelect(question.questionId, optionLabel)}
                  />
                  <span>
                    {optionLabel}. {option}
                  </span>
                </label>
              );
            })}
          </div>
        </div>
      ))}

      {submitError ? <div className="error">{submitError}</div> : null}

      {submitResult ? (
        <div className="grid" style={{ gap: 14 }}>
          <div className="card" style={{ borderColor: "var(--success-border)" }}>
            <div style={{ fontWeight: 700, marginBottom: 8 }}>Submitted successfully.</div>
            {pendingScore ? (
              <div className="hint">
                Your attempt was recorded. The score will appear when the instructor releases it
                (show score immediately is off).
                {startData?.exam.showCorrectAnswers ? (
                  <>
                    {" "}
                    Correct answers stay hidden until your score is shown; then check{" "}
                    <Link to="/results/me">My results</Link>.
                  </>
                ) : null}
              </div>
            ) : (
              <div>
                <div className="row">
                  <span className="pill">Score: {submitResult.score}</span>
                  <span className="pill">Total: {submitResult.totalQuestions}</span>
                  <span className="pill">Violations: {submitResult.violationCount}</span>
                </div>
              </div>
            )}
            <div className="row" style={{ marginTop: 10 }}>
              <Link className="btn" to="/results/me">
                My results
              </Link>
              <Link className="btn" to="/exam/my-classes">
                Back to exams
              </Link>
            </div>
          </div>

          {revealedByQuestionId && questions.length > 0 ? (
            <div className="card">
              <div className="label" style={{ marginBottom: 8 }}>Correct answers</div>
              <p className="hint" style={{ marginTop: 0 }}>
                Your instructor enabled review after submit. Compare with your choices above.
              </p>
              <div className="grid" style={{ gap: 12 }}>
                {questions.map((q, index) => {
                  const correct = revealedByQuestionId[q.questionId];
                  if (!correct) return null;
                  const mine = answers[q.questionId];
                  const mineOriginal = mapDisplayLetterToOriginal(q, mine);
                  const correctDisplay = mapOriginalLetterToDisplay(q, correct);
                  const ok =
                    mineOriginal != null && mineOriginal.trim() !== "" && mineOriginal.toUpperCase() === correct.toUpperCase();
                  return (
                    <div
                      key={q.questionId}
                      className="card"
                      style={{
                        padding: 12,
                        background: "rgba(30,37,54,0.45)",
                        boxShadow: "none",
                        borderColor: ok ? "var(--success-border)" : "var(--border)",
                      }}
                    >
                      <div style={{ fontWeight: 700, marginBottom: 6 }}>
                        Q{index + 1}: {q.content}
                      </div>
                      <div className="row" style={{ flexWrap: "wrap", gap: 8, marginBottom: 4 }}>
                        <span className="pill">{ok ? "You got this right" : "You got this wrong"}</span>
                        {mine ? <span className="pill">Your letter: {mine}</span> : <span className="pill">Your letter: (none)</span>}
                      </div>
                      <QuestionReviewOptions options={q.options} correctLetter={correctDisplay ?? correct} userLetter={mine} />
                    </div>
                  );
                })}
              </div>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="row" style={{ justifyContent: "flex-end" }}>
          <button className="btn btn-solid" onClick={onSubmit} disabled={submitting || secondsLeft <= 0}>
            {submitting ? "Submitting…" : "Submit"}
          </button>
        </div>
      )}
    </div>
  );
}
