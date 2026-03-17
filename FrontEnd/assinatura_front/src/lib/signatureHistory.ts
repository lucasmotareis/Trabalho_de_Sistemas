import type { SignTextResponse } from "@/types/signature";
import { getStoredSession } from "@/lib/auth";

const SIGNATURE_HISTORY_PREFIX = "digital-signature-history";

function buildUserHistoryKey(userKey: string) {
  return `${SIGNATURE_HISTORY_PREFIX}:${userKey}`;
}

export function getSessionUserHistoryKey() {
  const session = getStoredSession();
  const idPart = session?.user?.id ? String(session.user.id) : "";
  const emailPart = session?.user?.email?.trim().toLowerCase() ?? "";
  const userKey = idPart || emailPart;
  return userKey || null;
}

export function getSignatureHistory(userKey: string): SignTextResponse[] {
  if (typeof window === "undefined") {
    return [];
  }

  const raw = window.localStorage.getItem(buildUserHistoryKey(userKey));
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as SignTextResponse[];
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.filter(
      (item) =>
        item &&
        typeof item.id === "number" &&
        typeof item.publicId === "string" &&
        typeof item.signatureBase64 === "string",
    );
  } catch {
    return [];
  }
}

export function appendSignatureHistory(userKey: string, result: SignTextResponse) {
  if (typeof window === "undefined") {
    return;
  }

  const current = getSignatureHistory(userKey);
  const deduped = current.filter(
    (item) => item.id !== result.id && item.publicId !== result.publicId,
  );
  const next = [result, ...deduped];
  window.localStorage.setItem(buildUserHistoryKey(userKey), JSON.stringify(next));
}

export function clearSignatureHistory(userKey: string) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(buildUserHistoryKey(userKey));
}
