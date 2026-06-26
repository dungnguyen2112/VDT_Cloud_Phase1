import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { parseApiError } from "../api/gateway";
import { Exam, ExamResult, getMyResults, listMyClassExams } from "../api/quiz";

function attemptInfo(results: ExamResult[], examId: string) {
  const attempts = results.filter((r) => r.examId === examId).length;
  return attempts;
}

export default function StudentExamsPage() {
  const [items, setItems] = useState<Exam[]>([]);
  const [results, setResults] = useState<ExamResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [examData, resultData] = await Promise.all([listMyClassExams(), getMyResults()]);
      setItems(examData);
      setResults(resultData);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined);
  }, []);

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <div>
          <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>My exams</h2>
          <p className="hint" style={{ marginTop: 4 }}>
            Published exams in your enrolled classes
          </p>
        </div>
        <button className="btn" onClick={() => loadData().catch(() => undefined)}>
          Refresh
        </button>
      </div>

      {loading ? (
        <div className="card">Loading…</div>
      ) : error ? (
        <div className="error">{error}</div>
      ) : items.length === 0 ? (
        <div className="card" style={{ color: "#94a3b8" }}>
          No published exams in your classes yet.
        </div>
      ) : (
        <div className="grid grid-2">
          {items.map((exam) => (
            <div key={exam.id} className="card">
              {(() => {
                const attempts = attemptInfo(results, exam.id);
                const maxAttempts = exam.maxAttempts ?? 1;
                const reachedLimit = attempts >= maxAttempts;
                const lockReason = reachedLimit ? `Attempt limit reached (${maxAttempts})` : null;
                return (
                  <>
              <div style={{ fontWeight: 700, fontSize: 16 }}>{exam.title}</div>
              <div className="row" style={{ marginTop: 8 }}>
                <span className="pill">{exam.status}</span>
                <span className="pill">{exam.duration} min</span>
                <span className="pill">Attempts: {exam.maxAttempts ?? 1}</span>
                <span className="pill">Taken: {attempts}</span>
              </div>
              {lockReason ? (
                <div className="hint" style={{ marginTop: 8, color: "#fca5a5" }}>
                  {lockReason}
                </div>
              ) : null}
              <div className="row" style={{ marginTop: 12 }}>
                {reachedLimit ? (
                  <button className="btn" disabled title={lockReason ?? undefined}>
                    No attempts left
                  </button>
                ) : (
                  <Link className="btn btn-solid" to={`/exam/${exam.id}/take`}>
                    Take exam
                  </Link>
                )}
              </div>
                  </>
                );
              })()}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
