import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { fetchTasks } from '../../store/tasksSlice';
import { setCreateTaskModalOpen } from '../../store/uiSlice';
import { Task, Board } from '../../types';
import { RoleBadge } from '../common/RoleBadge';

interface TaskListViewProps {
  board?: Board;
}

export const TaskListView: React.FC<TaskListViewProps> = ({ board }) => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace', projectSlug = 'brand-refresh', boardSlug = 'list-view' } = useParams<{
    workspaceSlug: string;
    projectSlug: string;
    boardSlug: string;
  }>();

  const { tasks } = useAppSelector((state) => state.tasks);

  useEffect(() => {
    dispatch(fetchTasks(undefined));
  }, [dispatch]);

  const totalTasks = tasks.length || 24;
  const completedCount = tasks.filter((t) => t.status === 'Completed' || t.status === 'Done').length || 12;
  const inProgressCount = tasks.filter((t) => t.status === 'In Progress').length || 8;
  const overdueCount = tasks.filter((t) => t.status === 'Overdue' || t.status === 'Pending').length || 4;

  const handleRowClick = (task: Task) => {
    const todoIdentifier = task.display_id || task.slug || task.id;
    if (workspaceSlug && projectSlug && boardSlug) {
      navigate(`/${workspaceSlug}/${projectSlug}/${boardSlug}/todo/${todoIdentifier}`);
    } else {
      navigate(`todo/${todoIdentifier}`);
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'Completed':
      case 'Done':
        return 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20';
      case 'In Progress':
        return 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20';
      case 'Overdue':
        return 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20';
      case 'Pending':
        return 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20';
      default:
        return 'bg-slate-500/10 text-slate-600 dark:text-slate-400 border-slate-500/20';
    }
  };

  const getPriorityBadgeClass = (priority: string) => {
    switch (priority) {
      case 'Urgent':
      case 'High':
        return 'bg-rose-500/10 text-rose-600 dark:text-rose-400';
      case 'Medium':
        return 'bg-amber-500/10 text-amber-600 dark:text-amber-400';
      default:
        return 'bg-slate-500/10 text-slate-600 dark:text-slate-400';
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">
            Task Directory
          </span>
          <h1 className="text-2xl font-extrabold text-[var(--text-on-surface)] mt-1">{board?.title || 'All Tasks Overview'}</h1>
        </div>

        <button
          onClick={() => dispatch(setCreateTaskModalOpen(true))}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer shrink-0 self-start sm:self-auto"
        >
          <span className="material-symbols-outlined text-base">add</span>
          <span>New Task</span>
        </button>
      </div>

      {/* 4 Bento Stat Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-2xs">
          <span className="text-xs font-semibold text-[var(--text-on-surface-variant)]">Total Tasks</span>
          <p className="text-2xl font-extrabold text-[var(--text-on-surface)] mt-2">{totalTasks}</p>
        </div>

        <div className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-2xs">
          <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">Completed</span>
          <p className="text-2xl font-extrabold text-emerald-600 dark:text-emerald-400 mt-2">{completedCount}</p>
        </div>

        <div className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-2xs">
          <span className="text-xs font-semibold text-blue-600 dark:text-blue-400">In Progress</span>
          <p className="text-2xl font-extrabold text-blue-600 dark:text-blue-400 mt-2">{inProgressCount}</p>
        </div>

        <div className="p-5 rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shadow-2xs">
          <span className="text-xs font-semibold text-rose-600 dark:text-rose-400">Overdue / Pending</span>
          <p className="text-2xl font-extrabold text-rose-600 dark:text-rose-400 mt-2">{overdueCount}</p>
        </div>
      </div>

      {/* Task Table */}
      <div className="rounded-2xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] overflow-hidden shadow-xs">
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-left text-xs">
            <thead className="bg-[var(--bg-surface-container-low)] border-b border-[var(--border-outline-variant)] text-[var(--text-on-surface-variant)] uppercase font-bold text-[10px] tracking-wider">
              <tr>
                <th className="py-3.5 px-4">Task Title</th>
                <th className="py-3.5 px-4">Category</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4">Assignee</th>
                <th className="py-3.5 px-4">Priority</th>
                <th className="py-3.5 px-4">Due Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border-outline-variant)] text-[var(--text-on-surface)]">
              {(tasks || []).map((task: Task) => (
                <tr
                  key={task.id}
                  onClick={() => handleRowClick(task)}
                  className="hover:bg-[var(--bg-surface-container-high)] transition-colors cursor-pointer group"
                >
                  <td className="py-3.5 px-4 font-semibold group-hover:text-[var(--color-primary)] transition-colors">
                    <div className="flex items-center gap-2">
                      {task.display_id && (
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold tracking-wider bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] border border-[var(--border-outline-variant)] shrink-0">
                          {task.display_id}
                        </span>
                      )}
                      <span>{task.title}</span>
                    </div>
                  </td>
                  <td className="py-3.5 px-4 text-[var(--text-on-surface-variant)] font-medium">
                    {task.category}
                  </td>
                  <td className="py-3.5 px-4">
                    <span
                      className={`inline-block px-2.5 py-1 rounded-lg text-[10px] font-bold border uppercase tracking-wider ${getStatusBadgeClass(
                        task.status
                      )}`}
                    >
                      {task.status}
                    </span>
                  </td>
                  <td className="py-3.5 px-4">
                    <div className="flex items-center gap-2 overflow-x-auto max-w-[200px]">
                      {(task.assignees || []).map((a, i) => (
                        <div key={i} className="flex items-center gap-1 shrink-0">
                          <div
                            title={a.name}
                            className="w-6 h-6 rounded-full border border-[var(--bg-surface-container-lowest)] overflow-hidden bg-[var(--color-primary)] text-white text-[9px] font-bold flex items-center justify-center shrink-0"
                          >
                            {a.avatar ? (
                              <img src={a.avatar} alt={a.name} className="w-full h-full object-cover" />
                            ) : (
                              a.initials || a.name?.slice(0, 2).toUpperCase()
                            )}
                          </div>
                          <RoleBadge role={a.role || 'Dev'} size="xs" />
                        </div>
                      ))}
                    </div>
                  </td>
                  <td className="py-3.5 px-4">
                    <span
                      className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${getPriorityBadgeClass(
                        task.priority
                      )}`}
                    >
                      {task.priority}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-[var(--text-on-surface-variant)] font-medium">
                    {task.dueDate}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
