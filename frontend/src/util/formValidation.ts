/** Shared client-side validation aligned with backend rules where known. */

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function trimOrEmpty(s: string): string {
  return s.trim();
}

export function validateLogin(username: string, password: string): string | null {
  const u = trimOrEmpty(username);
  const p = password;
  if (!u) return "Username is required.";
  if (!p) return "Password is required.";
  return null;
}

/** Auth RegisterRequest: username 3–50, email, password 6–100 */
export function validateRegister(input: {
  username: string;
  email: string;
  password: string;
}): string | null {
  const u = trimOrEmpty(input.username);
  const email = trimOrEmpty(input.email);
  const p = input.password;
  if (!u) return "Username is required.";
  if (u.length < 3 || u.length > 50) return "Username must be between 3 and 50 characters.";
  if (!email) return "Email is required.";
  if (!EMAIL_RE.test(email)) return "Please enter a valid email address.";
  if (!p) return "Password is required.";
  if (p.length < 6) return "Password must be at least 6 characters.";
  if (p.length > 100) return "Password must be at most 100 characters.";
  return null;
}

export function validateJoinCode(raw: string): string | null {
  const c = raw.trim().toUpperCase();
  if (!c) return "Join code is required.";
  if (c.length !== 8) return "Join code must be exactly 8 characters.";
  if (!/^[A-Z0-9]{8}$/.test(c)) return "Join code may only contain letters and numbers.";
  return null;
}

/** VerifyEmailRequest: exactly 6 characters (matches backend @Size(min=6,max=6)) */
export function validateVerificationCode(code: string): string | null {
  const c = code.replace(/\s+/g, "");
  if (c.length !== 6) return "Code must be exactly 6 characters.";
  return null;
}

export function validateQuestionForm(input: {
  content: string;
  category: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
}): string | null {
  if (!trimOrEmpty(input.content)) return "Question text is required.";
  if (!trimOrEmpty(input.category)) return "Category is required.";
  for (const [k, v] of [
    ["Option A", input.optionA],
    ["Option B", input.optionB],
    ["Option C", input.optionC],
    ["Option D", input.optionD],
  ] as const) {
    if (!trimOrEmpty(v)) return `${k} is required.`;
  }
  return null;
}

export function validateClassName(name: string): string | null {
  const n = trimOrEmpty(name);
  if (!n) return "Class name is required.";
  if (n.length > 200) return "Class name is too long.";
  return null;
}

export function validateExamCreate(input: {
  title: string;
  classId: string;
  maxAttempts: number;
}): string | null {
  if (!trimOrEmpty(input.title)) return "Exam title is required.";
  if (!trimOrEmpty(input.classId)) return "Please select a class.";
  if (!Number.isFinite(input.maxAttempts) || input.maxAttempts < 1) {
    return "Max attempts must be at least 1.";
  }
  return null;
}

export function validateEmailSearch(email: string): string | null {
  const e = trimOrEmpty(email);
  if (!e) return "Enter an email to search.";
  if (!EMAIL_RE.test(e)) return "Please enter a valid email address.";
  return null;
}
