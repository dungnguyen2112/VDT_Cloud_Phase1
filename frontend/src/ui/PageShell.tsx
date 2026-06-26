import React from "react";
import { useLocation } from "react-router-dom";
import RoleSidebar from "./RoleSidebar";

export default function PageShell({
  topBar,
  content,
}: {
  topBar: React.ReactNode;
  content: React.ReactNode;
}) {
  const { pathname } = useLocation();
  const hideSidebar =
    pathname === "/login" ||
    pathname === "/register" ||
    pathname === "/verify-email" ||
    pathname === "/enter-email-verify";

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      {topBar}
      <main style={{ flex: 1 }}>
        <div className={hideSidebar ? "container" : "container app-shell"}>
          {!hideSidebar ? <RoleSidebar /> : null}
          <div className="app-content">{content}</div>
        </div>
      </main>
    </div>
  );
}
