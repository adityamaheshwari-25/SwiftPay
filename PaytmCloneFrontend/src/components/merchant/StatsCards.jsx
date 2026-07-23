import { LABELS } from "@/config/labels.config";
import { StatCard } from "./StatCard";

export const StatsCard = ({ stats, isLoading }) => {
  const labels = LABELS.merchantComponents.statsCards;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg-grid-cols-3 gap-6 items-stretch">
      <StatCard
        label={labels.todayCollection}
        value={stats?.today}
        growth={stats?.dailyGrowthRate}
        compareLabel={labels.comparedYesterday}
        isLoading={isLoading}
      />
      <StatCard
        label={labels.monthlyCollection}
        value={stats?.monthly}
        growth={stats?.monthlyGrowthRate}
        compareLabel={labels.comparedLastMonth}
        isLoading={isLoading}
      />
    </div>
  );
};
