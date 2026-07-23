import { motion } from "framer-motion";
import { LABELS } from "@/config/labels.config";
import { AnimatedCounter } from "@/components/common/AnimatedCounter";

export const StatsSection = () => {
  const labels = LABELS.publicComponents.statsSection;
  const MotionDiv = motion.div;

  return (
    <section className="py-8 px-4 bg-gray-100">
      <div className="container mx-auto max-w-7xl grid grid-cols-2 md:grid-cols-4 gap-6">
        {labels.stats.map((stat, i) => (
          <MotionDiv key={i} initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} className="text-center">
            <p className="text-3xl font-bold text-blue-600">
              {stat.prefix}
              <AnimatedCounter {...stat} />
            </p>
            <p className="text-sm text-gray-600">{stat.label}</p>
          </MotionDiv>
        ))}
      </div>
    </section>
  );
};
