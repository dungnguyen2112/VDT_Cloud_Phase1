import { api } from "./gateway";

export type Question = {
  id: string;
  content: string;
  category?: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctAnswer: "A" | "B" | "C" | "D";
  createdAt?: string;
};

export type Exam = {
  id: string;
  title: string;
  duration: number;
  classId: string;
  createdBy: string;
  createdAt?: string;
  status: "DRAFT" | "PUBLISHED" | string;
  availableFrom?: string | null;
  availableUntil?: string | null;
  maxAttempts?: number | null;
  showCorrectAnswers?: boolean | null;
  showScoreImmediately?: boolean | null;
  questionIds?: string[];
};

export type ExamQuestion = {
  questionId: string;
  content: string;
  options: string[];
  optionKeys?: string[];
};

export type ExamStart = {
  exam: Exam;
  serverTime: string;
  deadlineAt: string;
};

export type RevealedAnswer = {
  questionId: string;
  correctAnswer: string;
};

export type ExamResult = {
  id: number;
  userId: string;
  examId: string;
  score: number | null;
  totalQuestions: number | null;
  violationCount: number;
  submittedAt: string;
  /** Present when the exam allows showing correct answers (with score / after submit). */
  revealedAnswers?: RevealedAnswer[] | null;
};

export type ViolationResponse = {
  userId: string;
  examId: string;
  violationCount: number;
};

export type ViolationEvent = {
  id: number;
  userId: string;
  examId: string;
  type: string;
  createdAt: string;
};

type Wrapped<T> = {
  data?: T;
  message?: string;
};

function unwrap<T>(payload: Wrapped<T> | T): T {
  const wrapped = payload as Wrapped<T>;
  if (wrapped && typeof wrapped === "object" && "data" in wrapped) {
    return wrapped.data as T;
  }
  return payload as T;
}

export async function getQuestionBank(q?: string, category?: string): Promise<Question[]> {
  const res = await api.get<Wrapped<Question[]> | Question[]>("/api/question/bank", {
    params: {
      ...(q?.trim() ? { q: q.trim() } : {}),
      ...(category?.trim() ? { category: category.trim() } : {}),
    },
  });
  return unwrap(res.data) ?? [];
}

export async function createQuestion(payload: Omit<Question, "id" | "createdAt">): Promise<Question> {
  const res = await api.post<Wrapped<Question> | Question>("/api/question", payload);
  return unwrap(res.data);
}

export async function updateQuestion(id: string, payload: Omit<Question, "id" | "createdAt">): Promise<Question> {
  const res = await api.put<Wrapped<Question> | Question>(`/api/question/${id}`, payload);
  return unwrap(res.data);
}

export async function deleteQuestion(id: string): Promise<void> {
  await api.delete(`/api/question/${id}`);
}

export async function importQuestions(file: File, examId?: string): Promise<Question[]> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await api.post<Wrapped<Question[]> | Question[]>("/api/question/import", formData, {
    params: examId ? { examId } : undefined,
    headers: { "Content-Type": "multipart/form-data" },
  });
  return unwrap(res.data) ?? [];
}

export async function generateQuestions(payload: { topic: string; count: number }, examId?: string): Promise<Question[]> {
  const res = await api.post<Wrapped<Question[]> | Question[]>("/api/question/generate", payload, {
    params: examId ? { examId } : undefined,
  });
  return unwrap(res.data) ?? [];
}

export async function listExams(): Promise<Exam[]> {
  const res = await api.get<Wrapped<Exam[]> | Exam[]>("/api/exam");
  return unwrap(res.data) ?? [];
}

export async function createExam(payload: {
  title: string;
  classId: string;
  duration: number;
  availableFrom?: string;
  availableUntil?: string;
  maxAttempts?: number;
  showCorrectAnswers?: boolean;
  showScoreImmediately?: boolean;
  status?: "DRAFT" | "PUBLISHED";
}): Promise<Exam> {
  const res = await api.post<Wrapped<Exam> | Exam>("/api/exam", payload);
  return unwrap(res.data);
}

export async function publishExam(examId: string): Promise<Exam> {
  const res = await api.post<Wrapped<Exam> | Exam>(`/api/exam/${examId}/publish`, {});
  return unwrap(res.data);
}

export type UpdateExamPayload = {
  title?: string;
  duration?: number;
  availableFrom?: string;
  availableUntil?: string;
  maxAttempts?: number;
  showCorrectAnswers?: boolean;
  showScoreImmediately?: boolean;
};

export async function updateExam(examId: string, payload: UpdateExamPayload): Promise<Exam> {
  const res = await api.patch<Wrapped<Exam> | Exam>(`/api/exam/${examId}`, payload);
  return unwrap(res.data);
}

export async function deleteExam(examId: string): Promise<void> {
  await api.delete(`/api/exam/${examId}`);
}

export async function attachQuestionToExam(examId: string, questionId: string): Promise<void> {
  await api.post(`/api/exam/${examId}/questions`, { questionId });
}

export async function attachQuestionsBulk(examId: string, questionIds: string[]): Promise<void> {
  await api.post(`/api/exam/${examId}/questions/bulk`, { questionIds });
}

export async function listMyClassExams(): Promise<Exam[]> {
  const res = await api.get<Wrapped<Exam[]> | Exam[]>("/api/exam/my-classes");
  return unwrap(res.data) ?? [];
}

export async function startExam(examId: string): Promise<ExamStart> {
  const res = await api.post<Wrapped<ExamStart> | ExamStart>(`/api/exam/${examId}/start`, {});
  return unwrap(res.data);
}

export async function getExamQuestions(examId: string): Promise<ExamQuestion[]> {
  const res = await api.get<Wrapped<ExamQuestion[]> | ExamQuestion[]>(`/api/exam/${examId}/questions`);
  return unwrap(res.data) ?? [];
}

export async function submitExam(payload: {
  examId: string;
  answers: Record<string, string>;
  idempotencyKey: string;
}): Promise<ExamResult> {
  const res = await api.post<Wrapped<ExamResult> | ExamResult>(
    "/api/results/submit",
    {
      examId: payload.examId,
      answers: payload.answers,
    },
    {
      headers: {
        "Idempotency-Key": payload.idempotencyKey,
      },
    },
  );
  return unwrap(res.data);
}

export async function getMyResults(): Promise<ExamResult[]> {
  const res = await api.get<Wrapped<ExamResult[]> | ExamResult[]>("/api/results/me");
  return unwrap(res.data) ?? [];
}

export async function reportViolation(
  examId: string,
  type?: "TAB_HIDDEN" | "FULLSCREEN_EXIT" | string,
): Promise<ViolationResponse> {
  const res = await api.post<Wrapped<ViolationResponse> | ViolationResponse>("/api/results/exam/violation", {
    examId,
    type: type ?? undefined,
  });
  return unwrap(res.data);
}

export async function getExamViolationEvents(examId: string, userId?: string): Promise<ViolationEvent[]> {
  const res = await api.get<Wrapped<ViolationEvent[]> | ViolationEvent[]>(`/api/results/exams/${examId}/violations`, {
    params: userId ? { userId } : undefined,
  });
  return unwrap(res.data) ?? [];
}

export async function getExamReport(examId: string): Promise<ExamResult[]> {
  const res = await api.get<Wrapped<ExamResult[]> | ExamResult[]>(`/api/results/exams/${examId}/report`);
  return unwrap(res.data) ?? [];
}

export async function exportExamReportCsv(examId: string): Promise<Blob> {
  const res = await api.get<Blob>(`/api/results/exams/${examId}/report.csv`, {
    responseType: "blob",
  });
  return res.data;
}
