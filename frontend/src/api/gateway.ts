import axios, { AxiosError } from "axios";
import { clearStoredToken, getStoredToken } from "../auth/tokenStore";

// Same-origin /api (Vite + Docker nginx proxy to gateway). Override: VITE_API_BASE_URL.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export type BaseResponse<T> = {
  timestamp?: string;
  status: number;
  message?: string;
  data: T;
};

export type ApiError = {
  status?: number;
  message: string;
};

function extractServerMessage(data: unknown): string | null {
  if (data == null) return null;
  if (typeof data === "string") return data;
  if (typeof data !== "object") return null;
  const o = data as Record<string, unknown>;

  const fieldErrors = o.errors ?? o.fieldErrors;
  if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
    const parts = fieldErrors
      .map((item: unknown) => {
        if (typeof item === "string") return item;
        if (item && typeof item === "object") {
          const fe = item as Record<string, unknown>;
          const msg = fe.defaultMessage ?? fe.message;
          const field = fe.field ?? fe.property;
          if (typeof msg === "string" && msg) {
            return typeof field === "string" && field ? `${field}: ${msg}` : msg;
          }
        }
        return null;
      })
      .filter((s): s is string => Boolean(s));
    if (parts.length > 0) {
      return parts.join(" ");
    }
  }

  if (typeof o.message === "string" && o.message) {
    return o.message;
  }
  if (typeof o.error === "string" && o.error) {
    return o.error;
  }
  const nested = o.data;
  if (nested && typeof nested === "object") {
    const msg = (nested as Record<string, unknown>).message;
    if (typeof msg === "string" && msg) return msg;
  }
  return null;
}

function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const e = err as AxiosError<any>;
    const status = e.response?.status;
    const fallbackMessage =
      status === 401
        ? "Unauthorized"
        : status === 403
          ? "Forbidden"
          : status === 409
            ? "Conflict request"
            : "Request failed";
    const data = e.response?.data;
    const extracted = extractServerMessage(data);
    return {
      status,
      message: extracted ?? e.message ?? fallbackMessage,
    };
  }
  return { message: err instanceof Error ? err.message : "Request failed" };
}

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const hasToken = !!getStoredToken();

      if (status === 401 && hasToken) {
        const currentToken = getStoredToken();
        const authHeader = ((error.config as any)?.headers?.Authorization ??
          (error.config as any)?.headers?.authorization) as string | undefined;
        const failedRequestToken =
          typeof authHeader === "string" && authHeader.startsWith("Bearer ")
            ? authHeader.slice(7)
            : null;

        // Ignore stale 401 from an older request/token (common during login bootstrap race).
        if (failedRequestToken && currentToken && failedRequestToken !== currentToken) {
          return Promise.reject(error);
        }

        // If user token is invalid, force login (except when the request is just `/api/auth/me` during boot).
        const requestUrl = ((error.config as any)?.url ?? "") as string;
        const isAuthMe = requestUrl.includes("/api/auth/me");

        if (!isAuthMe) {
          clearStoredToken();
          const path = window.location.pathname;
          if (path !== "/login" && path !== "/register" && path !== "/verify-email") {
            window.location.replace("/login");
          }
        } else {
          // Let AuthContext decide UX; don't hard-redirect here.
          clearStoredToken();
        }
      }

      if (status === 409 && error.response?.data && !error.response.data.message) {
        error.response.data.message =
          "Conflict: this resource may already exist or has changed. Please refresh and try again.";
      }
    }

    return Promise.reject(error);
  },
);

export async function apiGet<T>(path: string): Promise<T> {
  const res = await api.get<BaseResponse<T>>(path);
  return res.data.data;
}

export async function apiPost<T, B>(path: string, body: B): Promise<T> {
  const res = await api.post<BaseResponse<T>>(path, body);
  return res.data.data;
}

export async function apiPatch<T, B>(path: string, body: B): Promise<T> {
  const res = await api.patch<BaseResponse<T>>(path, body);
  return res.data.data;
}

export function parseApiError(err: unknown): ApiError {
  return toApiError(err);
}

