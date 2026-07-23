import { Inbox } from "lucide-react";

export const EmptyState = ({ message }) => (
  <div className="p-12 flex flex-col items-center justify-center text-center space-y-3">
    <div className="h-12 w-12 rounded-full bg-slate-50 flex items-center justify-center">
      <Inbox className="w-6 h-6 text-slate-300" />
    </div>
    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-widest">{message}</p>
  </div>
);