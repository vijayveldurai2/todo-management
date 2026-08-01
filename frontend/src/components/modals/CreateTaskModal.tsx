import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setCreateTaskModalOpen } from '../../store/uiSlice';
import { createNewTask } from '../../store/tasksSlice';
import { TaskPriority, TaskStatus } from '../../types';
import { RoleBadge } from '../common/RoleBadge';

export const CreateTaskModal: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace', projectSlug = 'brand-refresh', boardSlug = 'visual-identity' } = useParams<{
    workspaceSlug?: string;
    projectSlug?: string;
    boardSlug?: string;
  }>();
  const { isCreateTaskModalOpen, activeProjectId, activeBoardId } = useAppSelector((state) => state.ui);
  const { projects, boards } = useAppSelector((state) => state.projects);
  const { user } = useAppSelector((state) => state.auth);

  const currentProject = projects.find((p) => p.id === activeProjectId) || projects[0];
  const projectBoards = boards.filter((b) => b.projectId === activeProjectId);
  const sprintBoards = projectBoards.filter((b) => b.type === 'Sprint');

  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('DESIGN');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('Medium');
  const [status, setStatus] = useState<TaskStatus>('To Do');
  const [dueDate, setDueDate] = useState('Oct 31, 2026');
  const [selectedAssigneeId, setSelectedAssigneeId] = useState<string>(user?.id || 'u-1');
  const [storyPoints, setStoryPoints] = useState<number | undefined>(3);
  const [sprintId, setSprintId] = useState<string>('');

  if (!isCreateTaskModalOpen) return null;

  const projectContributors = currentProject?.contributors || [
    {
      id: user?.id || 'u-1',
      name: user?.name || 'Vijay Kumar',
      email: user?.email || 'vijaykumar.veldurai2@gmail.com',
      role: 'PM',
      avatar: user?.avatar || '',
      initials: 'VK',
    },
    {
      id: 'u-101',
      name: 'Sarah Chen',
      email: 'sarah.chen@acme.io',
      role: 'Designer',
      avatar:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuC1PKC0Ku2evWEuc8IAMVL7d0JKP-13ijdGYcu4mHQcyrX2ieu3ezZt80XogOO-txGmsTtgIqKsKaYAcltKOZWEhza0iYeKbPPduEgqObKzej1igzR2d8T5ioYr4a0DfgDRXHj1AHtPeYxte-7bENSxstHoMSmAOENofG3s8gPMKMyfMKwpc1OiRTD9zyh4y0yWyWN_q6HEJR8dOPHzeXqtigSxrd5T56VsuCMF_TpKPsCtXG0M-ptB-VnHJb4615eRdMSCGHDMY3w9',
    },
    {
      id: 'u-102',
      name: 'Alex Rivera',
      email: 'alex.rivera@acme.io',
      role: 'Dev',
      avatar:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuBh_WmNHSdBdZ9EccTV-RIN76a0IkVKrjHkSim5lbnUnPxaiWCDIeU5DRvQPRLKmdGOM5s5mWbifbXHKelSKzEeeWEViSXBMIUAStquZlLPzsOB80axUkwezz4MJT_E4OuxNV7yPH9G8fzDgInMXMr7pC2S9cnUU9X1or_0Whm0-WBcGZIByOagADsgXSpC4AV6RVJgA8SUI3KXykzMg0Gz7v4TBcF4ygqkFhT_sFK7H5KASFVRkv6WvAGIzgLIAz_g9-93oqZF8cNR',
    },
  ];

  const chosenAssignee = projectContributors.find((c) => c.id === selectedAssigneeId) || projectContributors[0];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    const action = await dispatch(
      createNewTask({
        projectId: activeProjectId,
        boardId: activeBoardId,
        sprintId: sprintId || undefined,
        storyPoints: storyPoints || undefined,
        title: title.trim(),
        category,
        description: description.trim(),
        priority,
        status,
        dueDate,
        assignees: [
          {
            id: chosenAssignee.id,
            name: chosenAssignee.name,
            role: chosenAssignee.role || 'Dev',
            avatar: chosenAssignee.avatar || '',
            initials: chosenAssignee.initials || chosenAssignee.name.slice(0, 2).toUpperCase(),
          },
        ],
        subtasks: [],
        attachments: [],
        comments: [],
      })
    );

    if (createNewTask.fulfilled.match(action)) {
      const taskIdentifier = action.payload.display_id || action.payload.slug || action.payload.id;
      dispatch(setCreateTaskModalOpen(false));
      setTitle('');
      setDescription('');
      navigate(`/${workspaceSlug}/${projectSlug}/${boardSlug}/todo/${taskIdentifier}`);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-2xl shadow-2xl w-full max-w-lg p-6 relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={() => dispatch(setCreateTaskModalOpen(false))}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        <h2 className="text-xl font-bold text-[var(--text-on-surface)] mb-1">Create New Task</h2>
        <p className="text-xs text-[var(--text-on-surface-variant)] mb-5">
          Add a new card or task item to the active project board.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
              Task Title *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Implement responsive header nav"
              className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Category
              </label>
              <input
                type="text"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g. Design System"
                className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Due Date
              </label>
              <input
                type="text"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                placeholder="Oct 31, 2024"
                className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Assignee & Role
              </label>
              <select
                value={selectedAssigneeId}
                onChange={(e) => setSelectedAssigneeId(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs font-semibold text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors cursor-pointer"
              >
                {projectContributors.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.role || 'Member'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as TaskPriority)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors cursor-pointer"
              >
                <option value="Low">Low</option>
                <option value="Medium">Medium</option>
                <option value="High">High</option>
                <option value="Urgent">Urgent</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Story Points
              </label>
              <div className="flex items-center gap-1.5">
                {[1, 2, 3, 5, 8, 13].map((pts) => (
                  <button
                    key={pts}
                    type="button"
                    onClick={() => setStoryPoints(storyPoints === pts ? undefined : pts)}
                    className={`flex-1 py-1.5 rounded-lg text-xs font-bold border transition-all cursor-pointer ${
                      storyPoints === pts
                        ? 'bg-[var(--color-primary)] text-white border-[var(--color-primary)] shadow-xs'
                        : 'bg-[var(--bg-surface-container-low)] text-[var(--text-on-surface-variant)] border-[var(--border-outline-variant)] hover:bg-[var(--bg-surface-container-high)]'
                    }`}
                  >
                    {pts}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Assigned Sprint
              </label>
              <select
                value={sprintId}
                onChange={(e) => setSprintId(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs font-semibold text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors cursor-pointer"
              >
                <option value="">No Sprint (Backlog)</option>
                {sprintBoards.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.sprintName || s.title}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Chosen Assignee Preview Badge */}
          {chosenAssignee && (
            <div className="p-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-6 h-6 rounded-full bg-[var(--color-primary)] text-white text-[10px] font-bold flex items-center justify-center overflow-hidden">
                  {chosenAssignee.avatar ? (
                    <img src={chosenAssignee.avatar} alt={chosenAssignee.name} className="w-full h-full object-cover" />
                  ) : (
                    chosenAssignee.initials || chosenAssignee.name.slice(0, 2).toUpperCase()
                  )}
                </div>
                <span className="text-xs font-bold text-[var(--text-on-surface)]">{chosenAssignee.name}</span>
              </div>
              <RoleBadge role={chosenAssignee.role || 'Dev'} size="xs" />
            </div>
          )}

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
              Description
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide clear criteria, context, or design specifications..."
              className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors custom-scrollbar"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-[var(--border-outline-variant)]">
            <button
              type="button"
              onClick={() => dispatch(setCreateTaskModalOpen(false))}
              className="px-4 py-2.5 rounded-xl bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] text-xs font-semibold hover:bg-[var(--bg-surface-container-highest)] transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            >
              Create Task
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
