import { z } from "zod"

export const addMoneySchema = z.object({
  bankAccountId: z.number(),
  amount: z.number().min(1),
  paymentMode: z.enum([
    "UPI",
    "NETBANKING",
    "DEBIT_CARD",
    "CREDIT_CARD",
    "WALLET",
  ]),
})
