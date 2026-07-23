import React from "react";
import { motion } from "framer-motion";
import { ArrowRight, Percent, ShieldCheck, Wallet } from "lucide-react";
import { LABELS } from "@/config/labels.config";
import { Button } from "../ui/button";

export const CTASection = () => {
  const labels = LABELS.publicComponents.ctaSection;
  const MotionDiv = motion.div;

  return (
    <section className="py-16 px-4 sm:py-20">
      <MotionDiv
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        viewport={{ once: true }}
        className="relative container mx-auto max-w-6xl overflow-hidden rounded-[2rem] border border-white/25 bg-gradient-to-br from-blue-700 via-blue-600 to-cyan-600 p-8 text-white shadow-[0_30px_70px_-34px_rgba(13,52,137,0.7)] sm:p-12 lg:p-14"
      >
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute -left-16 -top-14 h-52 w-52 rounded-full bg-white/14 blur-3xl" />
          <div className="absolute -right-12 bottom-0 h-56 w-56 rounded-full bg-cyan-200/20 blur-3xl" />
        </div>

        <div className="relative z-10 grid grid-cols-1 gap-10 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <span className="mb-4 inline-flex items-center rounded-full border border-white/35 bg-white/10 px-4 py-1.5 text-xs font-bold uppercase tracking-wider text-white/95">
              {labels.badge}
            </span>
            <h2 className="text-3xl font-black tracking-tight sm:text-4xl lg:text-5xl">{labels.heading}</h2>
            <p className="mt-4 max-w-2xl text-base leading-relaxed text-white/90 sm:text-lg">{labels.subheading}</p>

            <div className="mt-7 flex flex-col gap-4 sm:flex-row sm:items-center">
              <Button
                size="lg"
                className="group h-12 px-8 text-base !bg-white !text-blue-900 hover:!bg-slate-100 shadow-xl shadow-blue-950/25"
              >
                {labels.primaryCta}
                <ArrowRight className="ml-2 h-5 w-5 transition-transform group-hover:translate-x-1" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="h-12 px-8 text-base border-2 !border-white !text-white hover:!bg-white hover:!text-blue-700 !bg-transparent"
              >
                {labels.learnMore}
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 lg:grid-cols-1">
            <div className="rounded-2xl border border-white/30 bg-white/12 p-4 backdrop-blur-sm">
              <div className="mb-2 inline-flex h-9 w-9 items-center justify-center rounded-xl bg-white/20">
                <Percent className="h-5 w-5" />
              </div>
              <p className="text-lg font-extrabold">{labels.statsCard.instantSettlementTitle}</p>
              <p className="mt-1 text-sm text-white/85">{labels.statsCard.instantSettlementDesc}</p>
            </div>

            <div className="rounded-2xl border border-white/30 bg-white/12 p-4 backdrop-blur-sm">
              <div className="mb-2 inline-flex h-9 w-9 items-center justify-center rounded-xl bg-white/20">
                <Wallet className="h-5 w-5" />
              </div>
              <p className="text-lg font-extrabold">{labels.statsCard.insightsTitle}</p>
              <p className="mt-1 text-sm text-white/85">{labels.statsCard.insightsDesc}</p>
            </div>

            <div className="rounded-2xl border border-white/30 bg-white/12 p-4 backdrop-blur-sm">
              <div className="mb-2 inline-flex h-9 w-9 items-center justify-center rounded-xl bg-white/20">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <p className="text-lg font-extrabold">{labels.statsCard.securityTitle}</p>
              <p className="mt-1 text-sm text-white/85">{labels.statsCard.securityDesc}</p>
            </div>
          </div>
        </div>
      </MotionDiv>
    </section>
  );
};
