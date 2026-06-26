import React from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type SidebarItem = {
  to: string;
  label: string;
};

function SidebarLink({ item }: { item: SidebarItem }) {
  const { pathname } = useLocation();
  const active = pathname === item.to || pathname.startsWith(`${item.to}/`);

  return (
    <Link
      to={item.to}
      className="sidebar-link"
      style={{
        background: active ? "rgba(59,130,246,0.16)" : "transparent",
        borderColor: active ? "rgba(59,130,246,0.45)" : "transparent",
        color: active ? "#dbeafe" : "#94a3b8",
      }}
    >
      {item.label}
    </Link>
  );
}

export default function RoleSidebar() {
  const { profile } = useAuth();
  const { pathname } = useLocation();

  if (!profile) return null;
  if (pathname === "/login" || pathname === "/register" || pathname === "/verify-email") {
    return null;
  }

  const common: SidebarItem[] = [{ to: "/notifications", label: "Notifications" }];

  const roleItems: SidebarItem[] =
    profile.role === "STUDENT"
      ? [
          { to: "/classes/student", label: "My classes" },
          { to: "/join", label: "Join class" },
          { to: "/exam/my-classes", label: "Exams" },
          { to: "/results/me", label: "Results" },
        ]
      : [
          { to: "/classes/teacher", label: "Classes" },
          { to: "/question-bank", label: "Question bank" },
          { to: "/exam/manage", label: "Manage exams" },
        ];

  const items = [...roleItems, ...common];

  return (
    <aside className="app-sidebar card" style={{ padding: 14 }}>
      <div className="label" style={{ marginBottom: 8 }}>
        Navigation
      </div>
      <div className="grid" style={{ gap: 6 }}>
        {items.map((item) => (
          <SidebarLink key={item.to} item={item} />
        ))}
      </div>
    </aside>
  );
}
