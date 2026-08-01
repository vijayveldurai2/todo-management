import React from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setCreateProjectModalOpen, setSettingsModalOpen } from '../../store/uiSlice';

export const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace', projectSlug, boardSlug } = useParams<{
    workspaceSlug?: string;
    projectSlug?: string;
    boardSlug?: string;
  }>();

  const { projects } = useAppSelector((state) => state.projects);

  const pathname = location.pathname;

  const isWorkspaceActive = pathname === '/workspace' || (pathname === `/${workspaceSlug}` && !projectSlug);
  const isTasksActive = pathname.startsWith('/tasks') || boardSlug === 'list-view';
  const isKanbanActive = pathname.startsWith('/kanban') || boardSlug === 'visual-identity';
  const isSprintActive = pathname.startsWith('/sprint') || boardSlug === 'sprint-board';
  const isCalendarActive = pathname.startsWith('/calendar');

  return (
    <aside className="w-64 fixed top-16 left-0 bottom-0 bg-[var(--bg-surface-container-low)] border-r border-[var(--border-outline-variant)] p-4 flex flex-col justify-between hidden md:flex z-40 transition-colors">
      <div className="space-y-6 overflow-y-auto custom-scrollbar pr-1">
        {/* Workspace selector */}
        <div
          onClick={() => navigate(`/${workspaceSlug}`)}
          className="flex items-center justify-between p-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-xs cursor-pointer hover:border-[var(--color-primary)] transition-all"
        >
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="w-8 h-8 rounded-lg bg-[var(--color-primary)] text-white flex items-center justify-center font-bold text-sm shrink-0">
              TW
            </div>
            <div className="overflow-hidden">
              <h3 className="font-bold text-xs truncate text-[var(--text-on-surface)]">Team Workspace</h3>
              <p className="text-[10px] text-[var(--text-on-surface-variant)] truncate">Main Workspace</p>
            </div>
          </div>
          <span className="material-symbols-outlined text-[var(--text-on-surface-variant)] text-sm">unfold_more</span>
        </div>

        {/* Main Navigation */}
        <div className="space-y-1">
          <button
            onClick={() => navigate(`/${workspaceSlug}`)}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold cursor-pointer transition-colors ${
              isWorkspaceActive
                ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] dark:bg-[var(--color-primary)] dark:text-white'
                : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-lg">dashboard</span>
            <span>Workspace Overview</span>
          </button>

          <button
            onClick={() => navigate('/tasks')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold cursor-pointer transition-colors ${
              isTasksActive
                ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] dark:bg-[var(--color-primary)] dark:text-white'
                : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-lg">check_box</span>
            <span>Task List View</span>
          </button>

          <button
            onClick={() => navigate('/kanban')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold cursor-pointer transition-colors ${
              isKanbanActive
                ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] dark:bg-[var(--color-primary)] dark:text-white'
                : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-lg">view_kanban</span>
            <span>Kanban Board</span>
          </button>

          <button
            onClick={() => navigate('/sprint')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold cursor-pointer transition-colors ${
              isSprintActive
                ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] dark:bg-[var(--color-primary)] dark:text-white'
                : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-lg">directions_run</span>
            <span>Sprint Board</span>
          </button>

          <button
            onClick={() => navigate('/calendar')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold cursor-pointer transition-colors ${
              isCalendarActive
                ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] dark:bg-[var(--color-primary)] dark:text-white'
                : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-lg">calendar_month</span>
            <span>Calendar Schedule</span>
          </button>

          <button
            onClick={() => navigate('/login')}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)] transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-lg">login</span>
            <span>Login Screen</span>
          </button>
        </div>

        {/* Projects Section Header */}
        <div>
          <div className="flex items-center justify-between px-3 mb-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)]">
              Projects ({projects.length})
            </span>
            <button
              onClick={() => dispatch(setCreateProjectModalOpen(true))}
              title="Add New Project"
              className="p-1 rounded hover:bg-[var(--bg-surface-container-high)] text-[var(--color-primary)] cursor-pointer"
            >
              <span className="material-symbols-outlined text-sm font-bold">add</span>
            </button>
          </div>

          <div className="space-y-1">
            {projects.map((proj) => {
              const pSlug = proj.slug || proj.id;
              const isSelected = projectSlug === pSlug;
              return (
                <button
                  key={proj.id}
                  onClick={() => navigate(`/${workspaceSlug}/${pSlug}`)}
                  className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium cursor-pointer transition-colors group ${
                    isSelected
                      ? 'bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-[var(--color-primary)] font-bold shadow-2xs'
                      : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)]'
                  }`}
                >
                  <div className="flex items-center gap-2.5 overflow-hidden">
                    <span
                      className="material-symbols-outlined text-base"
                      style={{ color: proj.color || 'var(--color-primary)' }}
                    >
                      {proj.icon || 'folder'}
                    </span>
                    <span className="truncate">{proj.name}</span>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Sidebar Footer */}
      <div className="pt-4 border-t border-[var(--border-outline-variant)] space-y-1">
        <button
          onClick={() => dispatch(setCreateProjectModalOpen(true))}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer mb-2"
        >
          <span className="material-symbols-outlined text-base">add_circle</span>
          <span>New Project</span>
        </button>

        <button
          onClick={() => dispatch(setSettingsModalOpen(true))}
          className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)] hover:text-[var(--text-on-surface)] transition-colors cursor-pointer"
        >
          <span className="material-symbols-outlined text-base">settings</span>
          <span>Settings</span>
        </button>
      </div>
    </aside>
  );
};
