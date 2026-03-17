import { apiClient } from "@/lib/axios";
import type {
  VerifyByPublicIdResponse,
  VerifyManualRequest,
  VerifyManualResponse,
} from "@/types/verify";
import { API_ENDPOINTS } from "./api";

function mapVerifyResponse(
  response: VerifyByPublicIdResponse,
): VerifyByPublicIdResponse {
  return {
    ...response,
    signatureId: response.publicId,
    signer: response.signerName,
    algorithm: response.signatureAlgorithm,
    timestamp: response.createdAt,
  };
}

export const verifyService = {
  async verifyByPublicId(publicId: string): Promise<VerifyByPublicIdResponse> {
    const response = await apiClient.get<VerifyByPublicIdResponse>(
      API_ENDPOINTS.verify.byPublicId(publicId),
    );
    return mapVerifyResponse(response);
  },

  async verifyManual(payload: VerifyManualRequest): Promise<VerifyManualResponse> {
    const response = await apiClient.post<VerifyManualResponse>(
      API_ENDPOINTS.verify.manual,
      payload,
    );
    return mapVerifyResponse(response);
  },
};
