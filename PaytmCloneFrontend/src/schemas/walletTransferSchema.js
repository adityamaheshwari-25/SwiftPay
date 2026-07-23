import { z } from "zod"

export const walletTransferSchema = z.object({
  receiverMobile: z
    .string()
    .regex(/^\d{10}$/, "Enter a valid 10-digit mobile number"),

  amount: z
    .number({ invalid_type_error: "Amount is required" })
    .min(1, "Minimum amount is ₹1"),

  paymentMode: z.enum([
    "UPI",
    "NETBANKING",
    "DEBIT_CARD",
    "CREDIT_CARD",
    "WALLET",
  ]),

  mpin: z.string().length(4, "MPIN must be 4 digits")
})
