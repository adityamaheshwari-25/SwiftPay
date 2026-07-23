export const APP_ROLES = {
  USER: "USER",
  MERCHANT: "MERCHANT",
  ADMIN: "ADMIN",
  SUPER_ADMIN: "SUPER_ADMIN",
};

export const ROLE_HOME_PATHS = {
  [APP_ROLES.USER]: "/user/dashboard",
  [APP_ROLES.MERCHANT]: "/merchant/dashboard",
  [APP_ROLES.ADMIN]: "/admin/kyc",
  [APP_ROLES.SUPER_ADMIN]: "/admin/kyc",
};
