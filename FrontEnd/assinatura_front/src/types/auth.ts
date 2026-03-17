import type { User } from "./user";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  nome: string;
  email: string;
  password: string;
}

export interface AuthUserResponse {
  id: number;
  nome: string;
  email: string;
}

export type SignupResponse = AuthUserResponse;
export type LoginResponse = AuthUserResponse;
export type AuthMeResponse = AuthUserResponse;

// Backward-compatible shape currently used by existing UI state helpers.
export interface AuthSession {
  user: User;
}
