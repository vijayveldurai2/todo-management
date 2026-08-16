import React from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import { setShortcutsModalOpen } from '../../store/uiSlice';
import { SHORTCUT_LIST } from '../../hooks/useKeyboardShortcuts';

export const KeyboardShortcutsModal: React.FC = () => {
  const dispatch = useAppDispatch();
  const { isShortcutsModalOpen } = useAppSelector((state) => state.ui);

  if (!isShortcutsModalOpen) return null;

  const categories = Array.from(new Set(SHORTCUT_LIST.map((s) => s.category)));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-2xl shadow-2xl w-full max-w-lg p-6 relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={() => dispatch(setShortcutsModalOpen(false))}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-xl bg-[var(--color-primary-fixed)] text-[var(--color-primary)] flex items-center justify-center font-bold">
            <span className="material-symbols-outlined text-xl">keyboard</span>
          </div>
          <div>
            <h2 className="text-lg font-bold text-[var(--text-on-surface)]">Keyboard Shortcuts</h2>
            <p className="text-xs text-[var(--text-on-surface-variant)]">
              Quick commands for fast navigation and task creation
            </p>
          </div>
        </div>

        <div className="space-y-4 max-h-[60vh] overflow-y-auto custom-scrollbar pr-1">
          {categories.map((category) => {
            const items = SHORTCUT_LIST.filter((s) => s.category === category);
            return (
              <div key={category} className="space-y-2">
                <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)]">
                  {category}
                </h3>
                <div className="grid grid-cols-1 gap-1.5">
                  {items.map((item, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between p-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs"
                    >
                      <span className="font-medium text-[var(--text-on-surface)]">{item.description}</span>
                      <kbd className="px-2 py-1 rounded bg-[var(--bg-surface-container-highest)] border border-[var(--border-outline-variant)] font-mono font-bold text-[11px] text-[var(--color-primary)] shadow-2xs shrink-0 ml-2">
                        {item.key}
                      </kbd>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-5 pt-3 border-t border-[var(--border-outline-variant)] flex items-center justify-between text-[11px] text-[var(--text-on-surface-variant)]">
          <div className="flex items-center gap-1.5">
            <span className="material-symbols-outlined text-sm">info</span>
            <span>Shortcuts are disabled while typing in text inputs.</span>
          </div>
          <button
            onClick={() => dispatch(setShortcutsModalOpen(false))}
            className="px-3 py-1.5 rounded-lg bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] font-bold cursor-pointer hover:bg-[var(--bg-surface-container-highest)] transition-colors"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  );
};
