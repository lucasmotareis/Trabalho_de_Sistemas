import { signService } from "./sign.service";

// Backward-compatible alias while pages/components migrate to sign.service.ts
export const signatureService = {
  signText: signService.signText,
};
