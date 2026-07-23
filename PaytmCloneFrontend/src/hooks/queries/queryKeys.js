export const queryKeys = {
  auth: {
    all: ["auth"],
    merchantCategories: () => [...queryKeys.auth.all, "merchantCategories"],
  },
  user: {
    all: ['user'],
    profile: () => [...queryKeys.user.all, 'profile'],
    dashboard: () => [...queryKeys.user.all, 'dashboard'],
    security: () => [...queryKeys.user.all, 'security']
  },
  wallet: {
    all: ['wallet'],
    balance: () => [...queryKeys.wallet.all, 'balance'],
  },
  transactions: {
    all: ['transactions'],
    list: (page) => [...queryKeys.transactions.all, 'list', { page }],
    infinite: () => [...queryKeys.transactions.all, 'infinite'],
  },
  banks: {
    all: ['banks'],
    list: () => [...queryKeys.banks.all, 'list'],
  },
  merchant: {
    all: ['merchant'],
    dashboard: () => [...queryKeys.merchant.all, 'dashboard'],
    transactions: (page, size) => [...queryKeys.merchant.all, 'transactions', { page, size }],
    settlements: (page, size) => [...queryKeys.merchant.all, 'settlements', { page, size }]
  },
  admin: {
    all: ['admin'],
    kycList: () => [...queryKeys.admin.all, 'kyc'],
    stats: () => [...queryKeys.admin.all, 'stats'],
    highValueMerchantSummary: (minAmount, q, limit, offset) => [
      ...queryKeys.admin.all,
      "merchant",
      "highValue",
      "summary",
      { minAmount, q: q || undefined,  limit, offset }, // its a practise to pass on the params that are passing into your api, so whenever that changes it marks the data as stale and refetch it.
    ],

    highValueMerchantTransactions: (merchantId, minAmount, limit, offset) => [
      ...queryKeys.admin.all,
      "merchant",
      "highValue",
      "transactions",
      { merchantId, minAmount, limit, offset },
    ],
  },
  splits: {
    all: () => ["splits"],
    lists: () => ["splits", "lists"],
    created: () => ["splits", "created"],     // needs list endpoint or local fallback
    involved: () => ["splits", "involved"],   // needs list endpoint or local fallback
    details: (splitId) => ["splits", "details", splitId],
  },
};
