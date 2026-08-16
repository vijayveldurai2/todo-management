import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';

interface NotFoundPageProps {
  type?: 'workspace' | 'project' | 'board' | 'todo';
}

export const NotFoundPage: React.FC<NotFoundPageProps> = ({ type }) => {
  const navigate = useNavigate();
  const params = useParams();

  const entityName = type
    ? type.charAt(0).toUpperCase() + type.slice(1)
    : 'Page';

  return (
    <div className="min-h-screen flex items-center justify-center bg-[var(--bg-surface)] p-6 text-[var(--text-on-surface)]">
      <div className="max-w-md w-full bg-[var(--bg-surface-container-lowest)] p-8 rounded-2xl border border-[var(--border-outline-variant)] shadow-xl text-center space-y-5 animate-in zoom-in-95 duration-200">
        <div className="w-16 h-16 rounded-full bg-red-500/10 text-red-500 flex items-center justify-center mx-auto">
          <span className="material-symbols-outlined text-3xl">sentiment_dissatisfied</span>
        </div>

        <div>
          <span className="text-xs font-bold uppercase tracking-widest text-[var(--text-on-surface-variant)]">
            404 Not Found
          </span>
          <h1 className="text-2xl font-bold mt-1 text-[var(--text-on-surface)]">
            {entityName} Not Found
          </h1>
          <p className="text-xs text-[var(--text-on-surface-variant)] mt-2 leading-relaxed">
            We couldn't find a {type || 'resource'} matching the URL parameters in this workspace.
          </p>
        </div>

        {Object.keys(params).length > 0 && (
          <div className="p-3 bg-[var(--bg-surface-container-low)] rounded-lg text-left text-[11px] font-mono space-y-1 overflow-x-auto text-[var(--text-on-surface-variant)]">
            {params.workspaceSlug && <div><strong className="text-[var(--color-primary)]">workspace:</strong> {params.workspaceSlug}</div>}
            {params.projectSlug && <div><strong className="text-[var(--color-primary)]">project:</strong> {params.projectSlug}</div>}
            {params.boardSlug && <div><strong className="text-[var(--color-primary)]">board:</strong> {params.boardSlug}</div>}
            {params.todoSlug && <div><strong className="text-[var(--color-primary)]">todo:</strong> {params.todoSlug}</div>}
          </div>
        )}

        <div className="pt-2 flex flex-col gap-2">
          <button
            onClick={() => navigate('/main-workspace')}
            className="w-full py-2.5 px-4 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs shadow-md hover:opacity-90 active:scale-95 transition-all cursor-pointer flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-sm">home</span>
            <span>Return to Workspace</span>
          </button>
        </div>
      </div>
    </div>
  );
};
