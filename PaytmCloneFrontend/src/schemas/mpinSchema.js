import { z } from "zod"

export const setMpinSchema = z
  .object({
    mpin: z
      .string()
      .regex(/^\d{4}$/, "MPIN must be exactly 4 digits"),

    confirmMpin: z
      .string()
      .regex(/^\d{4}$/, "Confirm MPIN must be exactly 4 digits"),
  })
  .refine((data) => data.mpin === data.confirmMpin, {
    message: "MPINs do not match",
    path: ["confirmMpin"],
  })
