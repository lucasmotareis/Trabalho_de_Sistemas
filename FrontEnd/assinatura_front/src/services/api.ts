export const API_ENDPOINTS = {
  auth: {
    signup: "/auth/signup",
    login: "/auth/login",
    logout: "/auth/logout",
    me: "/auth/me",
  },
  sign: {
    signText: "/sign",
  },
  verify: {
    byPublicId: (publicId: string) => `/verify/${publicId}`,
    manual: "/verify",
  },
  keys: {
    public: "/keys/public",
    me: "/keys/me",
  },
  verificationLogs: {
    me: "/me/verification-logs",
    all: "/verification-logs",
  },
};
