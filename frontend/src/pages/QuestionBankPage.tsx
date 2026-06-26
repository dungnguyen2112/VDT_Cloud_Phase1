import React, { useEffect, useMemo, useState } from "react";
import { api, parseApiError } from "../api/gateway";
import { validateQuestionForm } from "../util/formValidation";
import {
  createQuestion,
  deleteQuestion,
  generateQuestions,
  getQuestionBank,
  importQuestions,
  Question,
  updateQuestion,
} from "../api/quiz";

type QuestionFormState = {
  content: string;
  category: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctAnswer: "A" | "B" | "C" | "D";
};

const emptyForm: QuestionFormState = {
  content: "",
  category: "GENERAL",
  optionA: "",
  optionB: "",
  optionC: "",
  optionD: "",
  correctAnswer: "A",
};

export default function QuestionBankPage() {
  const [items, setItems] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<QuestionFormState>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [importing, setImporting] = useState(false);

  const [topic, setTopic] = useState("");
  const [count, setCount] = useState(3);
  const [generating, setGenerating] = useState(false);

  const filtered = useMemo(() => {
    return items;
  }, [items]);

  async function loadQuestions() {
    setLoading(true);
    setError(null);
    try {
      const [data, categoryRes] = await Promise.all([
        getQuestionBank(keyword, category),
        api.get<any>(`/api/question/bank/categories`),
      ]);
      setItems(data);
      const categoryData = categoryRes.data?.data ?? categoryRes.data ?? [];
      setCategories(Array.isArray(categoryData) ? categoryData : []);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadQuestions().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const onEdit = (q: Question) => {
    setEditingId(q.id);
    setForm({
      content: q.content,
      category: q.category || "GENERAL",
      optionA: q.optionA,
      optionB: q.optionB,
      optionC: q.optionC,
      optionD: q.optionD,
      correctAnswer: q.correctAnswer,
    });
    setError(null);
    setSuccess(null);
  };

  const onSubmitQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccess(null);
    const qErr = validateQuestionForm(form);
    if (qErr) {
      setError(qErr);
      setSaving(false);
      return;
    }
    try {
      if (editingId) {
        await updateQuestion(editingId, form);
        setSuccess("Question updated.");
      } else {
        await createQuestion(form);
        setSuccess("Question created.");
      }
      resetForm();
      await loadQuestions();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (id: string) => {
    setError(null);
    setSuccess(null);
    try {
      await deleteQuestion(id);
      setSuccess("Question deleted.");
      await loadQuestions();
      if (editingId === id) resetForm();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const onImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError("Please choose a CSV or XLSX file to import.");
      return;
    }
    setImporting(true);
    setError(null);
    setSuccess(null);
    try {
      const imported = await importQuestions(file);
      setSuccess(`Import thanh cong ${imported.length} cau hoi.`);
      setFile(null);
      await loadQuestions();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setImporting(false);
    }
  };

  const onGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    setGenerating(true);
    setError(null);
    setSuccess(null);
    if (!topic.trim()) {
      setError("Topic is required for auto-generation.");
      setGenerating(false);
      return;
    }
    if (count < 1 || count > 20) {
      setError("Count must be between 1 and 20.");
      setGenerating(false);
      return;
    }
    try {
      const generated = await generateQuestions({ topic: topic.trim(), count });
      setSuccess(`Generated ${generated.length} question(s).`);
      setTopic("");
      setCount(3);
      await loadQuestions();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div>
        <h2 style={{ margin: 0, fontWeight: 800, letterSpacing: "-0.02em" }}>Question bank</h2>
        <p className="hint" style={{ marginTop: 4 }}>
          Create, edit, delete, import, and generate questions
        </p>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start", gap: 16 }}>
        <div className="grid" style={{ gap: 14 }}>
          <div className="card">
            <div className="label" style={{ marginBottom: 10 }}>
              {editingId ? "Edit question" : "New question"}
            </div>
            <form noValidate onSubmit={onSubmitQuestion} className="grid" style={{ gap: 10 }}>
              <textarea
                className="input"
                placeholder="Question text"
                value={form.content}
                onChange={(e) => setForm((prev) => ({ ...prev, content: e.target.value }))}
                rows={3}
              />
              <input
                className="input"
                placeholder="Category (e.g. REACT, ALGORITHM, JAVA)"
                value={form.category}
                onChange={(e) => setForm((prev) => ({ ...prev, category: e.target.value.toUpperCase() }))}
              />
              <input
                className="input"
                placeholder="Option A"
                value={form.optionA}
                onChange={(e) => setForm((prev) => ({ ...prev, optionA: e.target.value }))}
              />
              <input
                className="input"
                placeholder="Option B"
                value={form.optionB}
                onChange={(e) => setForm((prev) => ({ ...prev, optionB: e.target.value }))}
              />
              <input
                className="input"
                placeholder="Option C"
                value={form.optionC}
                onChange={(e) => setForm((prev) => ({ ...prev, optionC: e.target.value }))}
              />
              <input
                className="input"
                placeholder="Option D"
                value={form.optionD}
                onChange={(e) => setForm((prev) => ({ ...prev, optionD: e.target.value }))}
              />
              <select
                className="input"
                value={form.correctAnswer}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    correctAnswer: e.target.value as QuestionFormState["correctAnswer"],
                  }))
                }
              >
                <option value="A">Correct answer: A</option>
                <option value="B">Correct answer: B</option>
                <option value="C">Correct answer: C</option>
                <option value="D">Correct answer: D</option>
              </select>

              <div className="row">
                <button className="btn btn-solid" type="submit" disabled={saving}>
                  {saving ? "Saving…" : editingId ? "Update" : "Create"}
                </button>
                {editingId ? (
                  <button className="btn" type="button" onClick={resetForm}>
                        Cancel
                  </button>
                ) : null}
              </div>
            </form>
          </div>

          <div className="card">
            <div className="label" style={{ marginBottom: 10 }}>
              Import from file
            </div>
            <form className="grid" style={{ gap: 10 }} onSubmit={onImport}>
              <input
                className="input"
                type="file"
                accept=".csv,.xlsx"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
              <button className="btn" type="submit" disabled={!file || importing}>
                {importing ? "Importing…" : "Choose file"}
              </button>
            </form>
          </div>

          <div className="card">
            <div className="label" style={{ marginBottom: 10 }}>
              Auto-generate questions
            </div>
            <form className="grid" style={{ gap: 10 }} onSubmit={onGenerate}>
              <input
                className="input"
                placeholder="Topic"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
              />
              <input
                className="input"
                type="number"
                min={1}
                max={20}
                value={count}
                onChange={(e) => setCount(Number(e.target.value))}
              />
              <button className="btn" type="submit" disabled={generating || !topic.trim()}>
                {generating ? "Generating…" : "Generate"}
              </button>
            </form>
          </div>
        </div>

        <div className="card">
          <div className="row" style={{ justifyContent: "space-between", marginBottom: 12 }}>
            <div className="label">Questions</div>
            <div className="row">
              <input
                className="input"
                placeholder="Search content"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                style={{ width: 220 }}
              />
              <select
                className="input"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                style={{ width: 180 }}
              >
                <option value="">All categories</option>
                {categories.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
              <button className="btn" onClick={() => loadQuestions().catch(() => undefined)}>
                Refresh
              </button>
            </div>
          </div>

          {error ? <div className="error" style={{ marginBottom: 10 }}>{error}</div> : null}
          {success ? <div className="success" style={{ marginBottom: 10 }}>{success}</div> : null}

          {loading ? (
            <div className="hint">Loading…</div>
          ) : filtered.length === 0 ? (
            <div className="hint">No questions found.</div>
          ) : (
            <div className="grid" style={{ gap: 10 }}>
              {filtered.map((q) => (
                <div key={q.id} className="card" style={{ padding: 14, background: "rgba(30,37,54,0.35)" }}>
                  <div style={{ fontWeight: 700, marginBottom: 6 }}>{q.content}</div>
                  <div className="row" style={{ marginBottom: 6 }}>
                    <span className="pill">Category: {q.category || "GENERAL"}</span>
                  </div>
                  <div className="hint">A. {q.optionA}</div>
                  <div className="hint">B. {q.optionB}</div>
                  <div className="hint">C. {q.optionC}</div>
                  <div className="hint">D. {q.optionD}</div>
                  <div className="row" style={{ justifyContent: "space-between", marginTop: 10 }}>
                    <span className="pill">Answer: {q.correctAnswer}</span>
                    <div className="row">
                      <button className="btn" onClick={() => onEdit(q)}>
                        Edit
                      </button>
                      <button className="btn btn-danger" onClick={() => onDelete(q.id)}>
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
