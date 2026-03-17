import axios, { AxiosError, type AxiosRequestConfig } from "axios";

const baseURL =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") ??
  "http://localhost:8080";

export const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; detail?: string }>) => {
    const message =
      error.response?.data?.message ??
      error.response?.data?.detail ??
      error.message ??
      "Request failed";
    return Promise.reject(new Error(message));
  },
);

interface RequestConfig extends AxiosRequestConfig {
  params?: Record<string, string | number | boolean | undefined>;
}

export const apiClient = {
  get: async <TResponse>(path: string, config?: RequestConfig) =>
    (await api.get<TResponse>(path, config)).data,
  post: async <TResponse>(
    path: string,
    body?: unknown,
    config?: RequestConfig,
  ) => (await api.post<TResponse>(path, body, config)).data,
  put: async <TResponse>(path: string, body?: unknown, config?: RequestConfig) =>
    (await api.put<TResponse>(path, body, config)).data,
  delete: async <TResponse>(path: string, config?: RequestConfig) =>
    (await api.delete<TResponse>(path, config)).data,
};
