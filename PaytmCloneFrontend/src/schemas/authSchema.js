import React from 'react';
import { z } from 'zod';
// Zod Schemas
export const loginSchema = z.object({
  email: z.string().email({message: 'Invalid email address'}),
  password: z.string().min(1, 'Password is required'),
});

export const registerUserSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email({ message: 'Invalid email' }),
  mobile: z.string().regex(/^[6-9]\d{9}$/, 'Invalid mobile number'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters long')
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
      'Password must include uppercase, lowercase, number, and special character'
    ),
});

export const registerMerchantSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email({ message: 'Invalid email' }),
  mobile: z.string().regex(/^[6-9]\d{9}$/, 'Invalid mobile number'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters long')
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
      'Password must include uppercase, lowercase, number, and special character'
    ),
  businessName: z.string().min(1, 'Business name is required'),
  category: z.string().min(1, 'Category is required'),
});