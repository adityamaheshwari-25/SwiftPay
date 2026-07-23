import React from 'react';
import { motion } from 'framer-motion';
import { HeroSection } from '@/components/public/HeroSection';
import { StatsSection } from '@/components/public/StatsSection';
import { FeaturesSection } from '@/components/public/FeaturesSection';
import { ProductStoriesSection } from '@/components/public/ProductStoriesSection';
import { PublicFooter } from '@/components/public/PublicFooter';
import { CTASection } from '@/components/public/CTASection';
import { Header } from '@/components/common/Header';
import { useAuth } from '@/context/AuthContext';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';

export default function WalletLandingPage() {
  const { user } = useAuth();
  const MotionDiv = motion.div;

  return (
    <div className="relative min-h-screen overflow-x-hidden bg-background">
      <div className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute -top-32 right-[-6rem] h-80 w-80 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute top-[28rem] left-[-7rem] h-80 w-80 rounded-full bg-accent/20 blur-3xl" />
        <div className="absolute bottom-[18rem] right-[5%] h-64 w-64 rounded-full bg-secondary/70 blur-3xl" />
      </div>

      <Header user={user} />

      <main className="relative">

        <HeroSection />

        <MotionDiv
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.2 }}
          transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
          className="relative z-10"
        >
          <StatsSection />
        </MotionDiv>

        <MotionDiv
          initial={{ opacity: 0, y: 28 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.15 }}
          transition={{ duration: 0.58, ease: [0.22, 1, 0.36, 1], delay: 0.08 }}
          className="relative"
        >
          <FeaturesSection />
        </MotionDiv>

        <MotionDiv
          initial={{ opacity: 0, y: 28 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.16 }}
          transition={{ duration: 0.58, ease: [0.22, 1, 0.36, 1], delay: 0.1 }}
          className="relative"
        >
          <ProductStoriesSection />
        </MotionDiv>

        <MotionDiv
          initial={{ opacity: 0, y: 28 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.25 }}
          transition={{ duration: 0.58, ease: [0.22, 1, 0.36, 1], delay: 0.1 }}
          className="px-2 sm:px-4"
        >
          <CTASection />
        </MotionDiv>
      </main>

      <PublicFooter />
    </div>
  );
}
