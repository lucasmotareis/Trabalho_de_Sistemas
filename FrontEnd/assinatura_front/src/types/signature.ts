export interface SignTextRequest {
  text: string;
}

export interface SignApiResponse {
  id: number;
  publicId: string;
  signatureBase64: string;
  hashAlgorithm: string;
  signatureAlgorithm: string;
  createdAt: string;
}

// Backward-compatible fields kept for current UI usage.
export interface SignTextResponse extends SignApiResponse {
  signatureId: string;
  signature: string;
  algorithm?: string;
  timestamp?: string;
  signer?: string;
}
