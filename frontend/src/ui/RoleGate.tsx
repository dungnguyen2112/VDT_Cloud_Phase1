import React from "react";
import { useAuth } from "../auth/AuthContext";
import { UserRole } from "../auth/types";

export default function RoleGate({
  allow,
  message,
  children,
}: {
  allow: UserRole[];
  message?: string;
  children: React.ReactNode;
}) {
  const { profile, loading } = useAuth();

  if (loading) {
    return <div className="card">Loading permissions…</div>;
  }

  if (!profile) {
    return <div className="error">Can dang nhap de truy cap trang nay.</div>;
  }

  if (!allow.includes(profile.role)) {
    return <div className="error">{message ?? "You do not have access to this feature."}</div>;
  }

  return <>{children}</>;
}
