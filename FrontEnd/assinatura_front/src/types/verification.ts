import type {
  VerifyByPublicIdResponse,
  VerifyManualRequest,
} from "./verify";

export type VerifySignatureResponse = VerifyByPublicIdResponse;

export interface VerifySignaturePayloadRequest {
  text: string;
  signature: string;
}

export type VerifyPayloadRequest = VerifyManualRequest;
