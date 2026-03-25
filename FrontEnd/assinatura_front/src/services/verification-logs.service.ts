import { apiClient } from "@/lib/axios";
import type {
  AdminVerificationLogItem,
  MyVerificationLogItem,
} from "@/types/verification-log";
import { API_ENDPOINTS } from "./api";

export const verificationLogsService = {
  listMyLogs(): Promise<MyVerificationLogItem[]> {
    return apiClient.get<MyVerificationLogItem[]>(API_ENDPOINTS.verificationLogs.me);
  },

  listAllLogs(): Promise<AdminVerificationLogItem[]> {
    return apiClient.get<AdminVerificationLogItem[]>(API_ENDPOINTS.verificationLogs.all);
  },
};
