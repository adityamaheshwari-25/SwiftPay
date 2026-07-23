import { CreditCard, Send, Shield, TrendingUp, Users, Zap } from "lucide-react";
import { motion } from "framer-motion";
import { LABELS } from "@/config/labels.config";

const FEATURE_ICONS = [Send, CreditCard, Users, Shield, Zap, TrendingUp];

export const FeaturesSection = () => {
  const labels = LABELS.publicComponents.featuresSection;
  const MotionDiv = motion.div;

  return (
    <section className="relative overflow-hidden py-18 px-4 sm:py-24">
      <div className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute left-[-6rem] top-8 h-72 w-72 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute right-[-5rem] bottom-[-2rem] h-80 w-80 rounded-full bg-accent/10 blur-3xl" />
        <div className="absolute left-1/2 top-24 h-44 w-44 -translate-x-1/2 rounded-full bg-secondary/70 blur-3xl" />
      </div>

      <div className="container mx-auto max-w-7xl">
        <MotionDiv
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="mb-12 text-center sm:mb-14"
        >
          <span className="mb-5 inline-flex items-center rounded-full border border-primary/25 bg-primary/10 px-4 py-1.5 text-xs font-semibold text-primary sm:text-sm">
            {labels.badge}
          </span>
          <h2 className="mb-4 text-3xl font-black tracking-tight text-foreground sm:text-4xl lg:text-5xl">{labels.heading}</h2>
          <p className="mx-auto max-w-3xl text-base leading-relaxed text-muted-foreground sm:text-lg">{labels.subheading}</p>
        </MotionDiv>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {labels.features.map((feature, index) => {
            const Icon = FEATURE_ICONS[index];
            const tag = labels.featureTags[index];

            return (
              <MotionDiv
                key={feature.title}
                initial={{ opacity: 0, y: 24, scale: 0.985 }}
                whileInView={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.5, delay: index * 0.06, ease: [0.22, 1, 0.36, 1] }}
                viewport={{ once: true }}
                whileHover={{ y: -8, transition: { duration: 0.22 } }}
                className="group relative overflow-hidden rounded-3xl border border-border/60 bg-card p-6 shadow-[0_16px_36px_-26px_rgba(12,36,86,0.55)] transition-all duration-300 hover:border-primary/30 hover:shadow-[0_24px_50px_-26px_rgba(16,87,220,0.45)] sm:p-7"
              >
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(30,110,255,0.14),transparent_48%),radial-gradient(circle_at_bottom_left,rgba(0,181,212,0.12),transparent_50%)] opacity-0 transition-opacity duration-300 group-hover:opacity-100" />

                <div className="relative z-10 mb-5 flex items-start justify-between">
                  <div className={`flex h-13 w-13 items-center justify-center rounded-2xl ring-1 ring-black/5 ${feature.color}`}>
                    <Icon className="h-6 w-6 transition-transform duration-300 group-hover:scale-110" />
                  </div>
                  <div className="text-right">
                    <p className="text-[11px] font-bold uppercase tracking-wide text-primary/80">{tag}</p>
                    <p className="text-xs font-semibold text-muted-foreground/80">{String(index + 1).padStart(2, "0")}</p>
                  </div>
                </div>

                <h3 className="relative z-10 mb-3 text-xl font-bold tracking-tight text-foreground">{feature.title}</h3>
                <p className="relative z-10 leading-relaxed text-muted-foreground">{feature.description}</p>

                <div className="relative z-10 mt-6 flex items-center justify-between">
                  <div className="h-[2px] w-14 rounded-full bg-gradient-to-r from-primary/70 to-accent/70 transition-all duration-300 group-hover:w-24" />
                  <span className="text-xs font-semibold uppercase tracking-wide text-primary/80">{labels.brandTag}</span>
                </div>
              </MotionDiv>
            );
          })}
        </div>
      </div>
    </section>
  );
};
