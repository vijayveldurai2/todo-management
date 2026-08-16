import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setCreateBoardModalOpen } from '../../store/uiSlice';
import { createNewBoard } from '../../store/projectsSlice';

export const CreateBoardModal: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace', projectSlug = 'brand-refresh' } = useParams<{
    workspaceSlug?: string;
    projectSlug?: string;
  }>();

  const { isCreateBoardModalOpen, activeProjectId } = useAppSelector((state) => state.ui);
  const { projects } = useAppSelector((state) => state.projects);

  const currentProject = projects.find((p) => p.slug === projectSlug || p.id === activeProjectId) || projects[0];

  const [title, setTitle] = useState('');
  const [boardType, setBoardType] = useState<'Kanban' | 'Sprint'>('Kanban');
  const [template, setTemplate] = useState('Default Kanban');
  const [sprintName, setSprintName] = useState('Sprint 1');
  const [startDate, setStartDate] = useState('Jul 21, 2026');
  const [endDate, setEndDate] = useState('Aug 04, 2026');
  const [goal, setGoal] = useState('');

  if (!isCreateBoardModalOpen) return null;

  const boardTypeOptions: { type: 'Kanban' | 'Sprint'; label: string; icon: string; desc: string }[] = [
    { type: 'Kanban', label: 'Kanban Board', icon: 'view_kanban', desc: 'Visual workflow columns (To Do, In Progress, Done)' },
    { type: 'Sprint', label: 'Sprint Board', icon: 'directions_run', desc: 'Agile iteration with backlog, story points & burndown chart' },
  ];

  const templatesForType = {
    Kanban: ['Default Kanban', 'Agile Software Dev', 'Design Review Board'],
    Sprint: ['Scrum 2-Week Sprint', 'Kanban Continuous Sprint', 'Custom Sprint'],
  };

  const handleTypeSelect = (type: 'Kanban' | 'Sprint') => {
    setBoardType(type);
    if (type === 'Sprint' && (!title || title.includes('Board'))) {
      setTitle('Sprint 12 Board');
      setSprintName('Sprint 12');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !currentProject) return;

    let icon = boardType === 'Sprint' ? 'directions_run' : 'view_kanban';

    const action = await dispatch(
      createNewBoard({
        projectId: currentProject.id,
        title: title.trim(),
        icon,
        type: boardType,
        sprintName: boardType === 'Sprint' ? sprintName : undefined,
        startDate: boardType === 'Sprint' ? startDate : undefined,
        endDate: boardType === 'Sprint' ? endDate : undefined,
        goal: boardType === 'Sprint' ? goal : undefined,
        status: boardType === 'Sprint' ? 'Active' : undefined,
      })
    );

    if (createNewBoard.fulfilled.match(action)) {
      const bSlug = action.payload.slug || action.payload.id;
      const pSlug = currentProject.slug || currentProject.id;
      dispatch(setCreateBoardModalOpen(false));
      setTitle('');
      setGoal('');
      navigate(`/${workspaceSlug}/${pSlug}/${bSlug}`);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-2xl shadow-2xl w-full max-w-lg p-6 relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={() => dispatch(setCreateBoardModalOpen(false))}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        <h2 className="text-xl font-bold text-[var(--text-on-surface)] mb-1">Create Board</h2>
        <p className="text-xs text-[var(--text-on-surface-variant)] mb-4">
          Add a new interactive workspace board to <strong>{currentProject?.name}</strong>.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Board Title */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
              Board Name *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Mobile Client Sprint 12"
              className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors"
            />
          </div>

          {/* Board Type Selection */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2">
              Board Type
            </label>
            <div className="grid grid-cols-2 gap-2">
              {boardTypeOptions.map((opt) => (
                <div
                  key={opt.type}
                  onClick={() => handleTypeSelect(opt.type)}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    boardType === opt.type
                      ? 'border-[var(--color-primary)] bg-[var(--color-primary-fixed)] text-[var(--color-primary)] shadow-xs'
                      : 'border-[var(--border-outline-variant)] bg-[var(--bg-surface-container-low)] text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)]'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1">
                    <span className="material-symbols-outlined text-lg">{opt.icon}</span>
                    <span className="text-xs font-bold text-[var(--text-on-surface)]">{opt.label}</span>
                  </div>
                  <p className="text-[10px] leading-tight text-[var(--text-on-surface-variant)] opacity-90">{opt.desc}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Board Template Selector */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
              Board Template
            </label>
            <select
              value={template}
              onChange={(e) => setTemplate(e.target.value)}
              className="w-full px-3.5 py-2 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs font-semibold text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] cursor-pointer"
            >
              {templatesForType[boardType]?.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>

          {/* SPRINT SPECIFIC FIELDS */}
          {boardType === 'Sprint' && (
            <div className="p-3.5 rounded-xl bg-[var(--color-primary-fixed)]/20 border border-[var(--color-primary)]/30 space-y-3 animate-in fade-in duration-200">
              <div className="flex items-center gap-2 text-xs font-bold text-[var(--color-primary)]">
                <span className="material-symbols-outlined text-base">directions_run</span>
                <span>Sprint Configuration</span>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                    Sprint Name
                  </label>
                  <input
                    type="text"
                    value={sprintName}
                    onChange={(e) => setSprintName(e.target.value)}
                    placeholder="Sprint 12"
                    className="w-full px-3 py-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)]"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                    Date Range
                  </label>
                  <div className="flex items-center gap-1">
                    <input
                      type="text"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      placeholder="Jul 21, 2026"
                      className="w-1/2 px-2 py-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-[11px] text-[var(--text-on-surface)] outline-none"
                    />
                    <span className="text-[10px] font-bold text-[var(--text-on-surface-variant)]">–</span>
                    <input
                      type="text"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      placeholder="Aug 04, 2026"
                      className="w-1/2 px-2 py-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-[11px] text-[var(--text-on-surface)] outline-none"
                    />
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                  Goal / Objective (Optional)
                </label>
                <textarea
                  rows={2}
                  value={goal}
                  onChange={(e) => setGoal(e.target.value)}
                  placeholder="e.g. Complete client authentication, payment gateway, and mobile polish..."
                  className="w-full px-3 py-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] custom-scrollbar"
                />
              </div>
            </div>
          )}

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-3 border-t border-[var(--border-outline-variant)]">
            <button
              type="button"
              onClick={() => dispatch(setCreateBoardModalOpen(false))}
              className="px-4 py-2 rounded-xl bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] text-xs font-semibold hover:bg-[var(--bg-surface-container-highest)] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-sm">add</span>
              <span>Create Board</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
