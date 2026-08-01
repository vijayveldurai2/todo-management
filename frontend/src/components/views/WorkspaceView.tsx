import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setCreateProjectModalOpen } from '../../store/uiSlice';

export const WorkspaceView: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace' } = useParams<{ workspaceSlug: string }>();
  const { projects } = useAppSelector((state) => state.projects);

  const handleSelectProject = (projectSlug: string) => {
    navigate(`/${workspaceSlug}/${projectSlug}`);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header Banner / Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">
            Workspace Overview
          </span>
          <h1 className="text-2xl font-extrabold text-[var(--text-on-surface)] mt-1">Active Projects</h1>
          <p className="text-xs text-[var(--text-on-surface-variant)] mt-1">
            Manage your team's initiatives, board tasks, and delivery milestones.
          </p>
        </div>

        <button
          onClick={() => dispatch(setCreateProjectModalOpen(true))}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer shrink-0 self-start sm:self-auto"
        >
          <span className="material-symbols-outlined text-base">add</span>
          <span>New Project</span>
        </button>
      </div>

      {/* Projects Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-5">
        {projects.map((project) => {
          const projectSlug = project.slug || project.id;
          return (
            <div
              key={project.id}
              onClick={() => handleSelectProject(projectSlug)}
              className="p-6 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] hover:border-[var(--color-primary)] shadow-xs hover:shadow-md transition-all cursor-pointer group flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span
                    className="px-2.5 py-1 rounded-lg text-[10px] font-bold uppercase tracking-wider text-white"
                    style={{ backgroundColor: project.color || 'var(--color-primary)' }}
                  >
                    {project.category}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-[var(--text-on-surface)] group-hover:text-[var(--color-primary)] transition-colors mb-2">
                  {project.name}
                </h3>

                <p className="text-xs text-[var(--text-on-surface-variant)] line-clamp-2 mb-4 leading-relaxed">
                  {project.description}
                </p>
              </div>

              <div>
                {/* Progress */}
                <div className="mb-4">
                  <div className="flex justify-between items-center text-xs mb-1.5 font-semibold">
                    <span className="text-[var(--text-on-surface-variant)]">Progress</span>
                    <span className="text-[var(--text-on-surface)]">
                      {project.completedTasks} of {project.totalTasks} tasks ({project.progressPercentage}%)
                    </span>
                  </div>
                  <div className="w-full h-2 bg-[var(--bg-surface-container-high)] rounded-full overflow-hidden">
                    <div
                      className="h-full transition-all duration-500 rounded-full"
                      style={{
                        width: `${project.progressPercentage}%`,
                        backgroundColor: project.color || 'var(--color-primary)',
                      }}
                    />
                  </div>
                </div>

                {/* Contributors Stack */}
                <div className="flex items-center justify-between border-t border-[var(--border-outline-variant)] pt-3">
                  <div className="flex -space-x-2">
                    {project.contributors.map((contrib, i) => (
                      <div
                        key={i}
                        title={contrib.name}
                        className="w-7 h-7 rounded-full border-2 border-[var(--bg-surface-container-lowest)] overflow-hidden bg-[var(--color-primary)] text-white text-[10px] font-bold flex items-center justify-center shrink-0"
                      >
                        {contrib.avatar ? (
                          <img src={contrib.avatar} alt={contrib.name} className="w-full h-full object-cover" />
                        ) : (
                          contrib.initials || contrib.name?.slice(0, 2).toUpperCase() || 'U'
                        )}
                      </div>
                    ))}
                  </div>

                  <span className="text-xs font-semibold text-[var(--color-primary)] group-hover:translate-x-1 transition-transform flex items-center gap-1">
                    <span>View Boards</span>
                    <span className="material-symbols-outlined text-sm">arrow_forward</span>
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
