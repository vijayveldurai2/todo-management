import React from 'react';
import { useAppDispatch } from '../../store';
import { setCreateTaskModalOpen } from '../../store/uiSlice';
import { Board } from '../../types';

interface CalendarViewProps {
  board?: Board;
}

export const CalendarView: React.FC<CalendarViewProps> = ({ board }) => {
  const dispatch = useAppDispatch();

  // Calendar month dates data
  const calendarDays = [
    { day: 1, label: 'Sun', event: 'Kickoff Meeting', color: 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300' },
    { day: 2, label: 'Mon', event: null },
    { day: 3, label: 'Tue', event: 'Market Analysis & Urgent Fix #24', color: 'bg-rose-500/20 text-rose-700 dark:text-rose-300' },
    { day: 4, label: 'Wed', event: null },
    { day: 5, label: 'Thu', event: 'Persona Research', color: 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-300' },
    { day: 6, label: 'Fri', event: null },
    { day: 7, label: 'Sat', event: null },

    { day: 8, label: '', event: null },
    { day: 9, label: '', event: 'Visual Stylesheet', color: 'bg-blue-500/20 text-blue-700 dark:text-blue-300' },
    { day: 10, label: '', event: 'Brand Refresh Review', color: 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300' },
    { day: 11, label: '', event: null },
    { day: 12, label: '', event: 'Asset Export', color: 'bg-amber-500/20 text-amber-700 dark:text-amber-300' },
    { day: 13, label: '', event: null },
    { day: 14, label: '', event: null },

    { day: 15, label: '', event: null },
    { day: 16, label: '', event: null },
    { day: 17, label: '', event: 'Legal Final Check', color: 'bg-slate-500/20 text-slate-700 dark:text-slate-300' },
    { day: 18, label: '', event: null },
    { day: 19, label: '', event: 'Final Presentation', color: 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-300' },
    { day: 20, label: '', event: null },
    { day: 21, label: '', event: null },

    { day: 22, label: '', event: null },
    { day: 23, label: '', event: null },
    { day: 24, label: '', event: 'Sprint Retrospective', color: 'bg-violet-500/20 text-violet-700 dark:text-violet-300' },
    { day: 25, label: '', event: null },
    { day: 26, label: '', event: null },
    { day: 27, label: '', event: null },
    { day: 28, label: '', event: null },

    { day: 29, label: '', event: null },
    { day: 30, label: '', event: 'Q4 Launch Goal', color: 'bg-rose-500/20 text-rose-700 dark:text-rose-300' },
  ];

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">
            Delivery Schedule
          </span>
          <h1 className="text-2xl font-extrabold text-[var(--text-on-surface)] mt-1">September 2024</h1>
          <p className="text-xs text-[var(--text-on-surface-variant)] mt-1">
            Sprint milestone events and scheduled release dates.
          </p>
        </div>

        <button
          onClick={() => dispatch(setCreateTaskModalOpen(true))}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer shrink-0 self-start sm:self-auto"
        >
          <span className="material-symbols-outlined text-base">event</span>
          <span>Schedule Milestone</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Month Grid */}
        <div className="lg:col-span-3 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] p-4 shadow-xs">
          <div className="grid grid-cols-7 gap-2 mb-2 text-center text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)]">
            <div>Sun</div>
            <div>Mon</div>
            <div>Tue</div>
            <div>Wed</div>
            <div>Thu</div>
            <div>Fri</div>
            <div>Sat</div>
          </div>

          <div className="grid grid-cols-7 gap-2">
            {calendarDays.map((item, index) => (
              <div
                key={index}
                className="min-h-[90px] p-2 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] flex flex-col justify-between hover:border-[var(--color-primary)] transition-colors cursor-pointer"
              >
                <div className="flex justify-between items-center text-xs font-bold text-[var(--text-on-surface)]">
                  <span>{item.day}</span>
                </div>

                {item.event ? (
                  <div className={`p-1.5 rounded-lg text-[10px] font-bold line-clamp-2 leading-tight ${item.color}`}>
                    {item.event}
                  </div>
                ) : (
                  <div className="h-4" />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Side Bento Cards */}
        <div className="space-y-4">
          <div className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-2xs space-y-3">
            <h3 className="font-bold text-xs uppercase tracking-wider text-[var(--text-on-surface)] flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm text-[var(--color-primary)]">alarm</span>
              <span>Upcoming Deadlines</span>
            </h3>

            <div className="space-y-2 text-xs">
              <div className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
                <span className="text-[10px] font-bold uppercase text-rose-600 dark:text-rose-400">Sep 10</span>
                <p className="font-bold text-[var(--text-on-surface)] mt-0.5">Brand Refresh Review</p>
                <p className="text-[10px] text-[var(--text-on-surface-variant)]">Executive signoff call</p>
              </div>

              <div className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
                <span className="text-[10px] font-bold uppercase text-amber-600 dark:text-amber-400">Sep 12</span>
                <p className="font-bold text-[var(--text-on-surface)] mt-0.5">Asset Export Sprint</p>
                <p className="text-[10px] text-[var(--text-on-surface-variant)]">SVG & PNG packages</p>
              </div>
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-2xs space-y-2">
            <h3 className="font-bold text-xs uppercase tracking-wider text-[var(--text-on-surface)] flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm text-emerald-600 dark:text-emerald-400">
                monitoring
              </span>
              <span>Project Health</span>
            </h3>
            <p className="text-xs text-[var(--text-on-surface-variant)] leading-relaxed">
              All 4 sprint streams are performing within expected velocity thresholds. No critical blockers detected.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
