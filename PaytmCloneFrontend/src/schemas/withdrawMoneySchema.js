import { z } from "zod"

export const withdrawMoneySchema = z.object({
  bankAccountId: z.number(),
  amount: z.number().min(1),
  mpin: z.string().regex(/^\d{4}$/),
})
