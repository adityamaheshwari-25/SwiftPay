import { z } from "zod"

export const createBankAccountSchema = z.object({
  bankName: z
    .string()
    .trim()
    .min(1, "Bank name is required"),

  accountNumber: z
    .string()
    .min(9, "Account number must be between 9 and 18 digits")
    .max(18, "Account number must be between 9 and 18 digits")
    .regex(/^\d+$/, "Account number must contain only digits"),

  ifsc: z
    .string()
    .trim()
    .regex(
      /^[A-Za-z]{4}0[A-Za-z0-9]{6}$/,
      "Invalid IFSC code format"
    )
    .transform((value) => value.toUpperCase()),
})
