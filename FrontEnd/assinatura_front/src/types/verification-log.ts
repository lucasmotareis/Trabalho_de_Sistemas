export interface MyVerificationLogItem {
  logId: number;
  signaturePublicId: string;
  valid: boolean;
  message: string;
  verifiedAt: string;
}

export interface AdminVerificationLogItem extends MyVerificationLogItem {
  verifiedByUserId: number | null;
  verifiedByUserEmail: string | null;
}
