/** Short token for logs / secondary labels — avoids full UUID in UI */
export function shortenId(value: string | undefined | null, left = 8, right = 4): string {
  if (!value) return "—";
  const v = value.trim();
  if (v.length <= left + right + 2) return v;
  return `${v.slice(0, left)}…${v.slice(-right)}`;
}

/** Prefer class name; never show raw UUID when we have a roster */
export function classLabel(classId: string, options: Array<{ id: string; name: string }>): string {
  const c = options.find((x) => x.id === classId);
  if (c?.name?.trim()) return c.name.trim();
  return "Class";
}
