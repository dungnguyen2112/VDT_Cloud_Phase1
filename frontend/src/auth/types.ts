export type UserRole = "ADMIN" | "INSTRUCTOR" | "STUDENT";

export type AuthProfile = {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  createdAt?: string;
};

export type AuthState = {
  token: string | null;
  profile: AuthProfile | null;
  loading: boolean;
};

