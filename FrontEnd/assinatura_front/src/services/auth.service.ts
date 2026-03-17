import { apiClient } from "@/lib/axios";
import type {
  AuthMeResponse,
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
} from "@/types/auth";
import { API_ENDPOINTS } from "./api";

export const authService = {
  signup(payload: SignupRequest): Promise<SignupResponse> {
    return apiClient.post<SignupResponse>(API_ENDPOINTS.auth.signup, payload);
  },

  login(payload: LoginRequest): Promise<LoginResponse> {
    return apiClient.post<LoginResponse>(
      API_ENDPOINTS.auth.login,
      payload,
    );
  },

  logout(): Promise<void> {
    return apiClient.post<void>(API_ENDPOINTS.auth.logout);
  },

  me(): Promise<AuthMeResponse> {
    return apiClient.get<AuthMeResponse>(API_ENDPOINTS.auth.me);
  },
};
