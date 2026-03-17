import { verifyService } from "./verify.service";
import type {
  VerifySignaturePayloadRequest,
  VerifySignatureResponse,
} from "@/types/verification";

// Backward-compatible adapter while pages/components migrate to verify.service.ts.
export const verificationService = {
  verifyById(publicId: string): Promise<VerifySignatureResponse> {
    return verifyService.verifyByPublicId(publicId);
  },

  verifyPayload(payload: VerifySignaturePayloadRequest): Promise<VerifySignatureResponse> {
    return Promise.reject(
      new Error(
        "verifyPayload now requires publicId and signatureBase64. Use verifyService.verifyManual.",
      ),
    );
  },
};
