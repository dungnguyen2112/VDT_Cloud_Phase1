import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, parseApiError } from "../api/gateway";
import { clearStoredToken, getStoredToken, setStoredToken } from "./tokenStore";
import { AuthProfile, AuthState, UserRole } from "./types";

type AuthContextValue = AuthState & {
  login: (username: string, password: string) => Promise<void>;
  register: (payload: {
    username: string;
    email: string;
    password: string;
    role: UserRole;
  }) => Promise<{ email: string }>;
  verifyEmail: (email: string, code: string) => Promise<void>;
  resendVerificationCode: (email: string) => Promise<{ email: string; expiresAt: string; message: string }>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [profile, setProfile] = useState<AuthProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Bootstrap auth state on refresh:
    // - Keep `loading=true` until we try to refresh `/api/auth/me`
    // - Avoid showing "login again" UX before profile is ready
    const bootstrap = async () => {
      const stored = getStoredToken();
      if (!stored) {
        setToken(null);
        setProfile(null);
        setLoading(false);
        return;
      }

      setToken(stored);
      try {
        await refreshProfile();
      } catch {
        // refreshProfile already clears token/profile on 401.
        // For other errors, just keep UX stable.
      } finally {
        setLoading(false);
      }
    };

    bootstrap().catch(() => setLoading(false));
  }, []);

  const refreshProfile = async () => {
    const stored = getStoredToken();
    if (!stored) {
      setProfile(null);
      setToken(null);
      return;
    }
    try {
      const res = await api.get<any, any>(`/api/auth/me`);
      // BaseResponse { data: UserProfileResponse }
      const data = res.data?.data ?? res.data;
      setProfile({
        id: data.id,
        username: data.username,
        email: data.email,
        role: data.role,
        createdAt: data.createdAt,
      });
    } catch (err) {
      const e = parseApiError(err);
      // If token is invalid, clear it to avoid infinite 401 loops.
      if (e.status === 401) {
        // Only clear if this 401 belongs to the same token we started with.
        // This prevents stale bootstrap calls from wiping a newly logged-in token.
        if (getStoredToken() === stored) {
          clearStoredToken();
          setToken(null);
          setProfile(null);
        }
      } else {
        throw err;
      }
    }
  };

  useEffect(() => {
    // If bootstrap finished but we still don't have profile (e.g. transient network error),
    // try once more in background.
    if (!token) return;
    if (profile) return;
    if (loading) return;
    refreshProfile().catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, profile, loading]);

  const login = async (username: string, password: string) => {
    setLoading(true);
    try {
      const res = await api.post<any, any>(`/api/auth/login`, { username, password });
      const data = res.data?.data ?? res.data;
      setStoredToken(data.token);
      setToken(data.token);
      setProfile({
        id: data.user.id,
        username: data.user.username,
        email: data.user.email,
        role: data.user.role,
        createdAt: data.user.createdAt,
      });
    } finally {
      setLoading(false);
    }
  };

  const register: AuthContextValue["register"] = async (payload) => {
    setLoading(true);
    try {
      const res = await api.post<any, any>(`/api/auth/register`, payload);
      const data = res.data?.data ?? res.data;
      return { email: data.email as string };
    } finally {
      setLoading(false);
    }
  };

  const verifyEmail: AuthContextValue["verifyEmail"] = async (email: string, code: string) => {
    setLoading(true);
    try {
      const res = await api.post<any, any>(`/api/auth/verify-email`, { email, code });
      const data = res.data?.data ?? res.data;
      setStoredToken(data.token);
      setToken(data.token);
      setProfile({
        id: data.user.id,
        username: data.user.username,
        email: data.user.email,
        role: data.user.role,
        createdAt: data.user.createdAt,
      });
    } finally {
      setLoading(false);
    }
  };

  const resendVerificationCode: AuthContextValue["resendVerificationCode"] = async (email: string) => {
    setLoading(true);
    try {
      const res = await api.post<any, any>(`/api/auth/resend-verification`, { email });
      const data = res.data?.data ?? res.data;
      return {
        email: data.email as string,
        expiresAt: data.expiresAt as string,
        message: data.message as string,
      };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    clearStoredToken();
    setToken(null);
    setProfile(null);
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      profile,
      loading,
      login,
      register,
      verifyEmail,
      resendVerificationCode,
      logout,
      refreshProfile,
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [token, profile, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

