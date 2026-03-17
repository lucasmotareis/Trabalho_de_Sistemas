import { apiClient } from "@/lib/axios";
import type { SignApiResponse, SignTextRequest, SignTextResponse } from "@/types/signature";
import { API_ENDPOINTS } from "./api";

function mapSignResponse(response: SignApiResponse): SignTextResponse {
  return {
    ...response,
    signatureId: response.publicId,
    signature: response.signatureBase64,
    algorithm: response.signatureAlgorithm,
    timestamp: response.createdAt,
  };
}

export const signService = {
  async signText(payload: SignTextRequest): Promise<SignTextResponse> {
    const response = await apiClient.post<SignApiResponse>(
      API_ENDPOINTS.sign.signText,
      payload,
    );
    return mapSignResponse(response);
  },
};
