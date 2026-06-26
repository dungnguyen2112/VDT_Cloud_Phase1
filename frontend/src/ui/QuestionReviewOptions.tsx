import React from "react";

const OPTION_LABELS = ["A", "B", "C", "D"] as const;

type Props = {
  options: string[];
  correctLetter: string;
  /** When set (e.g. right after exam), highlights the student's choice if wrong. */
  userLetter?: string | null;
};

export function QuestionReviewOptions({ options, correctLetter, userLetter }: Props) {
  const correct = correctLetter.trim().toUpperCase();
  const user = userLetter?.trim().toUpperCase() ?? "";

  return (
    <div className="grid" style={{ gap: 6, marginTop: 8 }}>
      {OPTION_LABELS.map((label, idx) => {
        const text = options[idx] ?? "";
        const isCorrect = label === correct;
        const isUserWrong = user.length > 0 && label === user && !isCorrect;
        return (
          <div
            key={label}
            style={{
              padding: "8px 10px",
              borderRadius: 8,
              border: `1px solid ${
                isCorrect ? "var(--success-border, #22c55e)" : isUserWrong ? "var(--danger-border, #ef4444)" : "var(--border)"
              }`,
              background: isCorrect
                ? "rgba(34, 197, 94, 0.12)"
                : isUserWrong
                  ? "rgba(239, 68, 68, 0.1)"
                  : "rgba(30, 37, 54, 0.35)",
            }}
          >
            <span style={{ fontWeight: 800, marginRight: 8 }}>{label}.</span>
            <span>{text}</span>
            {isCorrect ? (
              <span className="pill" style={{ marginLeft: 10, fontSize: 11 }}>
                {user === label ? "Correct — your choice" : "Correct answer"}
              </span>
            ) : null}
            {isUserWrong ? (
              <span className="pill" style={{ marginLeft: 10, fontSize: 11 }}>
                Your answer
              </span>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}
