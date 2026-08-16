import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../../services/apiService';
import { Workspace } from '../../types';

export const WorkspacePicker: React.FC = () => {
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiService
      .getWorkspaces()
      .then((res) => {
        setWorkspaces(res);
        setIsLoading(false);
        // Auto navigate to the first workspace slug (e.g. /main-workspace)
        if (res.length > 0) {
          navigate(`/${res[0].slug}`, { replace: true });
        }
      })
      .catch((err) => {
        console.warn('Error fetching workspaces:', err);
        setIsLoading(false);
        navigate('/main-workspace', { replace: true });
      });
  }, [navigate]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[var(--bg-surface)] flex flex-col items-center justify-center p-6 text-[var(--text-on-surface)]">
        <div className="flex items-center gap-3 text-xs font-semibold text-[var(--text-on-surface-variant)]">
          <div className="w-5 h-5 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin"></div>
          <span>Loading workspaces...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--bg-surface)] flex items-center justify-center p-6 text-[var(--text-on-surface)]">
      <div className="max-w-lg w-full bg-[var(--bg-surface-container-lowest)] p-8 rounded-2xl border border-[var(--border-outline-variant)] shadow-xl space-y-6">
        <div>
          <span className="text-xs font-bold uppercase tracking-widest text-[var(--color-primary)]">
            Task Management App
          </span>
          <h1 className="text-2xl font-bold mt-1">Select a Workspace</h1>
          <p className="text-xs text-[var(--text-on-surface-variant)] mt-1">
            Choose a workspace to view its projects, boards, and slug-routed tasks.
          </p>
        </div>

        <div className="space-y-3">
          {workspaces.map((ws) => (
            <button
              key={ws.id}
              onClick={() => navigate(`/${ws.slug}`)}
              className="w-full flex items-center justify-between p-4 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] hover:border-[var(--color-primary)] hover:bg-[var(--bg-surface-container-high)] transition-all cursor-pointer text-left"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[var(--color-primary)] text-white flex items-center justify-center font-bold text-sm">
                  {ws.name.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <h3 className="font-bold text-sm text-[var(--text-on-surface)]">{ws.name}</h3>
                  <p className="text-xs text-[var(--text-on-surface-variant)]">{ws.description || 'Active Workspace'}</p>
                </div>
              </div>
              <span className="material-symbols-outlined text-xl text-[var(--color-primary)]">arrow_forward</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
