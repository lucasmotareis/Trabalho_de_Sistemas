import { apiClient } from "@/lib/axios";
import type { MyKeysResponse, PublicKeyItem } from "@/types/keys";
import { API_ENDPOINTS } from "./api";

export const keysService = {
  listPublicKeys(): Promise<PublicKeyItem[]> {
    return apiClient.get<PublicKeyItem[]>(API_ENDPOINTS.keys.public);
  },

  getMyKeys(): Promise<MyKeysResponse> {
    return apiClient.get<MyKeysResponse>(API_ENDPOINTS.keys.me);
  },
};
