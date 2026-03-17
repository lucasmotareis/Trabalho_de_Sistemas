export interface VerifyByPublicIdResponse {
  valid: boolean;
  publicId: string;
  signerName: string;
  hashAlgorithm: string;
  signatureAlgorithm: string;
  createdAt: string;
  message?: string;
  // Backward-compatible fields used by current UI.
  signatureId?: string;
  signer?: string;
  algorithm?: string;
  timestamp?: string;
}

export interface VerifyManualRequest {
  publicId: string;
  text: string;
  signatureBase64: string;
}

export type VerifyManualResponse = VerifyByPublicIdResponse;
