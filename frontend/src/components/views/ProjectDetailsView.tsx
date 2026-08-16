import React, { useEffect, useState } from 'react';
import { useNavigate, useParams, useOutletContext } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setCreateBoardModalOpen } from '../../store/uiSlice';
import { fetchBoards } from '../../store/projectsSlice';
import { RoleBadge } from '../common/RoleBadge';
import { BoardType, Project, Board } from '../../types';
import { apiService } from '../../services/apiService';

interface ProjectOutletContext {
  project?: Project;
}

export const ProjectDetailsView: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace', projectSlug } = useParams<{
    workspaceSlug: string;
    projectSlug: string;
  }>();

  const outletContext = useOutletContext<ProjectOutletContext>();
  const [currentProject, setCurrentProject] = useState<Project | null>(outletContext?.project || null);
  const [boards, setBoards] = useState<Board[]>([]);
  const [activeTab, setActiveTab] = useState<'boards' | 'overview'>('boards');

  useEffect(() => {
    let isMounted = true;
    if (projectSlug && workspaceSlug) {
      apiService
        .getProjectBySlug(workspaceSlug, projectSlug)
        .then((p) => {
          if (isMounted) setCurrentProject(p);
        })
        .catch((e) => console.warn(e));

      apiService
        .getBoards(workspaceSlug, projectSlug)
        .then((bList) => {
          if (isMounted) setBoards(bList);
        })
        .catch((e) => console.warn(e));
    }
    return () => {
      isMounted = false;
    };
  }, [workspaceSlug, projectSlug]);

  const handleOpenBoard = (board: Board) => {
    const bSlug = board.slug || board.id;
    navigate(`/${workspaceSlug}/${projectSlug}/${bSlug}`);
  };

  const getBoardTypeIcon = (type?: BoardType) => {
    switch (type) {
      case 'Sprint':
        return 'directions_run';
      case 'List':
        return 'format_list_bulleted';
      case 'Calendar':
        return 'calendar_month';
      case 'Kanban':
      default:
        return 'view_kanban';
    }
  };

  const getBoardTypeColor = (type?: BoardType) => {
    switch (type) {
      case 'Sprint':
        return 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30';
      case 'List':
        return 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border-indigo-500/30';
      case 'Calendar':
        return 'bg-violet-500/10 text-violet-600 dark:text-violet-400 border-violet-500/30';
      case 'Kanban':
      default:
        return 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/30';
    }
  };

  if (!currentProject) return null;

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-xs text-[var(--text-on-surface-variant)]">
        <button
          onClick={() => navigate(`/${workspaceSlug}`)}
          className="hover:text-[var(--color-primary)] cursor-pointer"
        >
          Workspace
        </button>
        <span className="material-symbols-outlined text-xs">chevron_right</span>
        <span className="font-semibold text-[var(--text-on-surface)]">{currentProject.name}</span>
      </div>

      {/* Project Header Banner */}
      <div className="p-6 rounded-2xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span
                className="px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider text-white"
                style={{ backgroundColor: currentProject.color || 'var(--color-primary)' }}
              >
                {currentProject.category}
              </span>
            </div>
            <h1 className="text-2xl font-extrabold text-[var(--text-on-surface)]">{currentProject.name}</h1>
            <p className="text-xs text-[var(--text-on-surface-variant)] mt-1 max-w-2xl leading-relaxed">
              {currentProject.description}
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 shrink-0">
            <button
              onClick={() => dispatch(setCreateBoardModalOpen(true))}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-base">add</span>
              <span>New Board</span>
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 border-t border-[var(--border-outline-variant)] pt-4 mt-4 text-xs font-semibold">
          <button
            onClick={() => setActiveTab('boards')}
            className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center gap-2 ${
              activeTab === 'boards'
                ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] dark:bg-[var(--color-primary)] dark:text-white'
                : 'text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)]'
            }`}
          >
            <span className="material-symbols-outlined text-sm">dashboard</span>
            <span>Project Boards ({boards.length})</span>
          </button>
        </div>
      </div>

      {/* Boards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {boards.map((board) => (
          <div
            key={board.id}
            onClick={() => handleOpenBoard(board)}
            className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] hover:border-[var(--color-primary)] shadow-xs hover:shadow-md transition-all cursor-pointer group flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-3">
                <span
                  className={`px-2.5 py-1 rounded-lg text-[10px] font-bold uppercase tracking-wider border flex items-center gap-1 ${getBoardTypeColor(
                    board.type
                  )}`}
                >
                  <span className="material-symbols-outlined text-xs">{getBoardTypeIcon(board.type)}</span>
                  <span>{board.type || 'Board'}</span>
                </span>
              </div>

              <h3 className="text-base font-bold text-[var(--text-on-surface)] group-hover:text-[var(--color-primary)] transition-colors mb-1">
                {board.title}
              </h3>

              {board.type === 'Sprint' && board.goal && (
                <p className="text-xs text-[var(--text-on-surface-variant)] line-clamp-2 mt-2 leading-relaxed">
                  {board.goal}
                </p>
              )}
            </div>

            <div className="flex items-center justify-between border-t border-[var(--border-outline-variant)] pt-3 mt-4 text-xs">
              <span className="text-[var(--text-on-surface-variant)] font-semibold">
                {board.cardsCount || 0} tasks
              </span>
              <span className="text-[var(--color-primary)] font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                <span>Open</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
