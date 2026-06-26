import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import VerifyEmailPage from "./pages/VerifyEmailPage";
import EnterEmailVerifyPage from "./pages/EnterEmailVerifyPage";
import ClassesPage from "./pages/ClassesPage";
import JoinClassPage from "./pages/JoinClassPage";
import NotificationsPage from "./pages/NotificationsPage";
import QuestionBankPage from "./pages/QuestionBankPage";
import ExamsManagePage from "./pages/ExamsManagePage";
import ExamReportPage from "./pages/ExamReportPage";
import StudentExamsPage from "./pages/StudentExamsPage";
import TakeExamPage from "./pages/TakeExamPage";
import MyResultsPage from "./pages/MyResultsPage";
import TopBar from "./ui/TopBar";
import PageShell from "./ui/PageShell";
import RoleGate from "./ui/RoleGate";

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth();
  if (loading) return <div className="card">Checking session…</div>;
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function RoleRedirect() {
  const { profile, loading } = useAuth();
  if (loading) return <div className="card">Loading profile…</div>;
  if (!profile) return <Navigate to="/login" replace />;
  return (
    <Navigate
      to={profile.role === "STUDENT" ? "/classes/student" : "/classes/teacher"}
      replace
    />
  );
}

export default function App() {
  return (
    <AuthProvider>
      <PageShell
        topBar={<TopBar />}
        content={
          <Routes>
            <Route path="/" element={<RoleRedirect />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/enter-email-verify" element={<EnterEmailVerifyPage />} />

            <Route
              path="/classes/:mode"
              element={
                <RequireAuth>
                  <ClassesPage />
                </RequireAuth>
              }
            />
            <Route
              path="/join"
              element={
                <RequireAuth>
                  <JoinClassPage />
                </RequireAuth>
              }
            />
            <Route
              path="/notifications"
              element={
                <RequireAuth>
                  <NotificationsPage />
                </RequireAuth>
              }
            />

            <Route
              path="/question-bank"
              element={
                <RequireAuth>
                  <RoleGate allow={["INSTRUCTOR", "ADMIN"]}>
                    <QuestionBankPage />
                  </RoleGate>
                </RequireAuth>
              }
            />
            <Route
              path="/exam/manage"
              element={
                <RequireAuth>
                  <RoleGate allow={["INSTRUCTOR", "ADMIN"]}>
                    <ExamsManagePage />
                  </RoleGate>
                </RequireAuth>
              }
            />
            <Route
              path="/exam/reports/:examId"
              element={
                <RequireAuth>
                  <RoleGate allow={["INSTRUCTOR", "ADMIN"]}>
                    <ExamReportPage />
                  </RoleGate>
                </RequireAuth>
              }
            />
            <Route
              path="/exam/my-classes"
              element={
                <RequireAuth>
                  <RoleGate allow={["STUDENT"]}>
                    <StudentExamsPage />
                  </RoleGate>
                </RequireAuth>
              }
            />
            <Route
              path="/exam/:examId/take"
              element={
                <RequireAuth>
                  <RoleGate allow={["STUDENT"]}>
                    <TakeExamPage />
                  </RoleGate>
                </RequireAuth>
              }
            />
            <Route
              path="/results/me"
              element={
                <RequireAuth>
                  <RoleGate allow={["STUDENT"]}>
                    <MyResultsPage />
                  </RoleGate>
                </RequireAuth>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        }
      />
    </AuthProvider>
  );
}

