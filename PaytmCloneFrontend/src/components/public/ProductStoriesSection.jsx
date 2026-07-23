import { motion } from "framer-motion";
import { ArrowUpRight, CalendarDays, CircleDollarSign, Percent, Sparkles, Users } from "lucide-react";
import { LABELS } from "@/config/labels.config";
const STORY_ICONS = [CircleDollarSign, Users, CalendarDays, Percent];

export const ProductStoriesSection = () => {
  const labels = LABELS.publicComponents.productStoriesSection;
  const storyCards = labels.cards.map((card, index) => ({
    ...card,
    icon: STORY_ICONS[index],
  }));
  const MotionDiv = motion.div;

  return (
    <section className="relative overflow-hidden py-18 px-4 sm:py-24">
      <div className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute left-[5%] top-16 h-72 w-72 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute right-[3%] bottom-0 h-80 w-80 rounded-full bg-accent/10 blur-3xl" />
      </div>

      <div className="container mx-auto max-w-7xl">
        <MotionDiv
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.2 }}
          transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
          className="mb-12 text-center sm:mb-14"
        >
          <span className="mb-5 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-1.5 text-xs font-semibold text-primary sm:text-sm">
            <Sparkles className="h-3.5 w-3.5" />
            {labels.badge}
          </span>
          <h2 className="text-3xl font-black tracking-tight text-foreground sm:text-4xl lg:text-5xl">
            {labels.heading}
          </h2>
          <p className="mx-auto mt-4 max-w-3xl text-base text-muted-foreground sm:text-lg">
            {labels.subheading}
          </p>
        </MotionDiv>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {storyCards.map((card, index) => {
            const Icon = card.icon;
            return (
              <MotionDiv
                key={card.title}
                initial={{ opacity: 0, y: 28, scale: 0.985 }}
                whileInView={{ opacity: 1, y: 0, scale: 1 }}
                viewport={{ once: true, amount: 0.18 }}
                transition={{ duration: 0.52, delay: index * 0.06, ease: [0.22, 1, 0.36, 1] }}
                className="group relative overflow-hidden rounded-3xl border border-border/70 bg-card p-5 shadow-[0_14px_34px_-24px_rgba(24,39,75,0.55)] sm:p-6"
              >
                <div className={`absolute inset-0 bg-gradient-to-br ${card.tint} opacity-0 transition-opacity duration-300 group-hover:opacity-100`} />

                <div className="relative z-10">
                  <div className="relative mb-5 overflow-hidden rounded-2xl border border-border/60 bg-slate-50/70">
                    <img
                      src={card.media}
                      alt={card.title}
                      loading="lazy"
                      decoding="async"
                      className="h-full w-full object-cover"
                    />
                  </div>

                  <div className="mb-3 flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                      <Icon className="h-5 w-5" />
                    </div>
                    <h3 className="text-xl font-bold tracking-tight text-foreground">{card.title}</h3>
                  </div>

                  <p className="text-muted-foreground leading-relaxed">{card.description}</p>

                  <ul className="mt-4 space-y-2">
                    {card.points.map((point) => (
                      <li key={point} className="flex items-start gap-2 text-sm text-foreground/90">
                        <ArrowUpRight className="mt-0.5 h-4 w-4 text-primary" />
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>

                  {card.badge && (
                    <div className="mt-5 inline-flex rounded-full border border-emerald-200 bg-emerald-50 px-3.5 py-1.5 text-xs font-bold uppercase tracking-wide text-emerald-700">
                      {card.badge}
                    </div>
                  )}
                </div>
              </MotionDiv>
            );
          })}
        </div>
      </div>
    </section>
  );
};
