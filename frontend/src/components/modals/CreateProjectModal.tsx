import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setCreateProjectModalOpen } from '../../store/uiSlice';
import { createNewProject } from '../../store/projectsSlice';
import { RoleBadge } from '../common/RoleBadge';

interface ProjectMember {
  id: string;
  name: string;
  email: string;
  role: string;
  avatar?: string;
  initials?: string;
}

const PRESET_SUGGESTIONS: ProjectMember[] = [
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
  {
    id: 'u-103',
    name: 'Jordan Smith',
    email: 'jordan.smith@acme.io',
    role: 'QA',
    initials: 'JS',
  },
  {
    id: 'u-104',
    name: 'Mei Lin',
    email: 'mei.lin@acme.io',
    role: 'PM',
    avatar:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuDERkv107r5I33Ey_ZkKRjOuIuOjDd9jFbxDkF53va48wlyiZjw5_BdBtrPRICHwSmzA89sD6o5wU5gGdhZgkWmtCAwZpWXRupdPdJAj0sC-cMXEbK_0R5B3C1qKGwnREKu93Ppu6pZyn5RRp-mUSfPTsf_4PGhpYs406TGbSLJedSmbKMDIB7gX-ht-rBOC7bGR5bIYFc8un0nFJyKuzA7XahWWx3iILXfFv3fafs9zWWDpy5NCNttq4IesSLAfQezLSMgierWU4D2',
  },
  {
    id: 'u-105',
    name: 'David Miller',
    email: 'david.miller@acme.io',
    role: 'Dev',
    avatar:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCBCweLBBlfVzgW-mjaitVw2l_r_KsV_TkyqPTm47TMyp6ZHjtiKTh_4NxOXjZyXroIE3p4Med5uIxjknN8uAxyv0DzwrXuzoj4DyWEh6HZLlpL8iztaMCTNREcTCJkDNU6clBcvpdyz15OG4G6tzO9F50DX4HYuZdx07g30A9kj_oJHjA0cp2moZQiw3W2Tkk6yAvDF_gi3eiXfbpis7avKDB8bvmAZtBEEVg0AC3Su2aSkKAIdgER7Qvh2FFxPWeSkcJDlhvsLtB_',
  },
  {
    id: 'u-106',
    name: 'Emma Watson',
    email: 'emma.watson@acme.io',
    role: 'QA',
    avatar:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuDe8UyIFm3gWbQSXDBaDGCXU9CgK2mNR4G6lpNSeLw4diCyHZpjMh5D7C4xEeC_nQiao9n-bpzD2lECjNahvxn5GTZgR2GpR2dDJZBsaNQTJYkGP8AakjYwOUgnM0NyCG3lhUwxwlBU1xHv_yKJuS7jMNzDhS8ig3mrHXyvLetP_TvjXdkGiCClhrZOvrvLt24x428xUSXGg5XA1r9QKKa31cVkdQD-gyypvJiciSiClA1KJvPlaTFoYJFRzjYsZmqdwcpeQ7vm5r45',
  },
];

export const CreateProjectModal: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace' } = useParams<{ workspaceSlug: string }>();
  const { isCreateProjectModalOpen } = useAppSelector((state) => state.ui);
  const { user } = useAppSelector((state) => state.auth);

  const [activeTab, setActiveTab] = useState<'general' | 'members'>('general');

  // Form State
  const [name, setName] = useState('');
  const [prefix, setPrefix] = useState('');
  const [prefixError, setPrefixError] = useState('');
  const [isPrefixEdited, setIsPrefixEdited] = useState(false);
  const [category, setCategory] = useState('DESIGN');
  const [description, setDescription] = useState('');
  const [selectedColor, setSelectedColor] = useState('#3525cd');
  const [selectedIcon, setSelectedIcon] = useState('palette');

  const { projects: existingProjects } = useAppSelector((state) => state.projects);

  const generateSuggestedPrefix = (projectName: string): string => {
    if (!projectName.trim()) return '';
    const clean = projectName.trim().replace(/[^a-zA-Z0-9\s]/g, '');
    const words = clean.split(/\s+/).filter(Boolean);
    let code = '';
    if (words.length >= 2) {
      code = words.map((w) => w[0]).join('').toUpperCase();
    } else if (words.length === 1) {
      const w = words[0].toUpperCase();
      code = w.length >= 2 ? w.slice(0, 2) : w + 'X';
    }
    code = code.replace(/[^A-Z0-9]/g, '');
    if (code.length < 2) code = (code + 'PR').slice(0, 2);
    return code.slice(0, 6);
  };

  const validatePrefix = (value: string): boolean => {
    const clean = value.toUpperCase().trim();
    if (!clean) {
      setPrefixError('Project code is required.');
      return false;
    }
    if (!/^[A-Z0-9]{2,6}$/.test(clean)) {
      setPrefixError('Must be 2–6 uppercase letters or numbers.');
      return false;
    }
    const isTaken = existingProjects.some(
      (p) => p.prefix?.toUpperCase() === clean
    );
    if (isTaken) {
      setPrefixError(`${clean} is already used by another project`);
      return false;
    }
    setPrefixError('');
    return true;
  };

  const handleNameChange = (val: string) => {
    setName(val);
    if (!isPrefixEdited) {
      const suggested = generateSuggestedPrefix(val);
      setPrefix(suggested);
      if (suggested) validatePrefix(suggested);
      else setPrefixError('');
    }
  };

  // Members State
  const [members, setMembers] = useState<ProjectMember[]>([
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
  ]);

  // Add Member inputs
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRole, setSelectedRole] = useState<string>('Dev');
  const [showSuggestions, setShowSuggestions] = useState(false);

  const colors = ['#3525cd', '#059669', '#e11d48', '#d97706', '#0284c7', '#7c3aed', '#575e70', '#ba1a1a'];
  const icons = ['palette', 'smartphone', 'corporate_fare', 'campaign', 'folder', 'language', 'hub', 'rocket_launch'];
  const rolesList = ['Dev', 'QA', 'Designer', 'PM'];

  if (!isCreateProjectModalOpen) return null;

  const filteredSuggestions = PRESET_SUGGESTIONS.filter(
    (s) =>
      !members.some((m) => m.email.toLowerCase() === s.email.toLowerCase() || m.id === s.id) &&
      (s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.email.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleAddMember = (candidate?: ProjectMember) => {
    if (candidate) {
      setMembers((prev) => [...prev, { ...candidate, role: selectedRole || candidate.role }]);
      setSearchQuery('');
      setShowSuggestions(false);
      return;
    }

    if (!searchQuery.trim()) return;

    // Custom typed member
    const query = searchQuery.trim();
    const isEmail = query.includes('@');
    const newMember: ProjectMember = {
      id: `m-${Date.now()}`,
      name: isEmail ? query.split('@')[0] : query,
      email: isEmail ? query : `${query.toLowerCase().replace(/\s+/g, '.')}@acme.io`,
      role: selectedRole,
      initials: query.slice(0, 2).toUpperCase(),
    };

    setMembers((prev) => [...prev, newMember]);
    setSearchQuery('');
    setShowSuggestions(false);
  };

  const handleRemoveMember = (id: string) => {
    setMembers((prev) => prev.filter((m) => m.id !== id));
  };

  const handleMemberRoleChange = (id: string, newRole: string) => {
    setMembers((prev) =>
      prev.map((m) => (m.id === id ? { ...m, role: newRole } : m))
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    if (!validatePrefix(prefix)) {
      setActiveTab('general');
      return;
    }

    const action = await dispatch(
      createNewProject({
        name: name.trim(),
        prefix: prefix.toUpperCase().trim(),
        category,
        description: description.trim() || 'Custom project workspace with assigned team roles.',
        color: selectedColor,
        icon: selectedIcon,
        contributors: members.map((m) => ({
          id: m.id,
          name: m.name,
          email: m.email,
          role: m.role,
          avatar: m.avatar,
          initials: m.initials,
        })),
      })
    );

    if (createNewProject.fulfilled.match(action)) {
      const projectSlug = action.payload.slug || action.payload.id;
      dispatch(setCreateProjectModalOpen(false));
      setName('');
      setPrefix('');
      setPrefixError('');
      setIsPrefixEdited(false);
      setDescription('');
      setActiveTab('general');
      navigate(`/${workspaceSlug}/${projectSlug}`);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-2xl shadow-2xl w-full max-w-xl p-6 relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={() => dispatch(setCreateProjectModalOpen(false))}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        <h2 className="text-xl font-bold text-[var(--text-on-surface)] mb-1">Create New Project</h2>
        <p className="text-xs text-[var(--text-on-surface-variant)] mb-4">
          Set up project workspace parameters, add team members, and define role permissions.
        </p>

        {/* Top Tab Switcher */}
        <div className="flex border-b border-[var(--border-outline-variant)] mb-5">
          <button
            type="button"
            onClick={() => setActiveTab('general')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold border-b-2 transition-all cursor-pointer ${
              activeTab === 'general'
                ? 'border-[var(--color-primary)] text-[var(--color-primary)]'
                : 'border-transparent text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-base">folder_special</span>
            <span>General Info</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('members')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold border-b-2 transition-all cursor-pointer ${
              activeTab === 'members'
                ? 'border-[var(--color-primary)] text-[var(--color-primary)]'
                : 'border-transparent text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            <span className="material-symbols-outlined text-base">group</span>
            <span>Members & Roles</span>
            <span className="px-2 py-0.5 rounded-full bg-[var(--color-primary-fixed)] text-[var(--color-primary)] text-[10px] font-extrabold">
              {members.length}
            </span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* TAB 1: GENERAL INFO */}
          {activeTab === 'general' && (
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-3">
                <div className="col-span-2">
                  <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                    Project Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => handleNameChange(e.target.value)}
                    placeholder="e.g. Website Redesign"
                    className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors"
                  />
                </div>

                <div className="col-span-1">
                  <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                    Project Code *
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={prefix}
                    onChange={(e) => {
                      setIsPrefixEdited(true);
                      const upper = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
                      setPrefix(upper);
                      validatePrefix(upper);
                    }}
                    onBlur={() => validatePrefix(prefix)}
                    placeholder="e.g. WR"
                    className={`w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border text-sm font-mono font-bold uppercase tracking-wider text-[var(--text-on-surface)] outline-none transition-colors ${
                      prefixError ? 'border-red-500 focus:border-red-500' : 'border-[var(--border-outline-variant)] focus:border-[var(--color-primary)]'
                    }`}
                  />
                </div>
              </div>

              {prefixError ? (
                <p className="text-xs text-red-500 font-medium flex items-center gap-1 -mt-2">
                  <span className="material-symbols-outlined text-sm">error</span>
                  {prefixError}
                </p>
              ) : (
                <p className="text-[11px] text-[var(--text-on-surface-variant)] -mt-2">
                  2–6 uppercase characters (e.g. WR for Website Redesign). Used for all task IDs (WR-1, WR-2). Immutable after creation.
                </p>
              )}

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                  Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors cursor-pointer"
                >
                  <option value="DESIGN">DESIGN</option>
                  <option value="ENGINEERING">ENGINEERING</option>
                  <option value="OPERATIONS">OPERATIONS</option>
                  <option value="MARKETING">MARKETING</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Brief overview of project goals, team scope, and target outcomes..."
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-sm text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] transition-colors custom-scrollbar"
                />
              </div>

              {/* Color & Icon Selection */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2">
                  Accent Color & Icon
                </label>

                <div className="flex items-center gap-2 mb-3 overflow-x-auto pb-1 custom-scrollbar">
                  {colors.map((c) => (
                    <button
                      type="button"
                      key={c}
                      onClick={() => setSelectedColor(c)}
                      className={`w-7 h-7 rounded-full cursor-pointer transition-transform ${
                        selectedColor === c ? 'ring-2 ring-offset-2 ring-[var(--color-primary)] scale-110' : 'opacity-80 hover:opacity-100'
                      }`}
                      style={{ backgroundColor: c }}
                    />
                  ))}
                </div>

                <div className="grid grid-cols-8 gap-2">
                  {icons.map((ic) => (
                    <button
                      type="button"
                      key={ic}
                      onClick={() => setSelectedIcon(ic)}
                      className={`p-2 rounded-xl flex items-center justify-center cursor-pointer border transition-all ${
                        selectedIcon === ic
                          ? 'bg-[var(--color-primary-fixed)] text-[var(--color-primary)] border-[var(--color-primary)] font-bold'
                          : 'border-[var(--border-outline-variant)] text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)]'
                      }`}
                    >
                      <span className="material-symbols-outlined text-lg">{ic}</span>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: MEMBERS & ROLES */}
          {activeTab === 'members' && (
            <div className="space-y-4">
              {/* Add Member Inputs */}
              <div className="p-3.5 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] space-y-3 relative">
                <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-primary)] block">
                  Add Member to Project
                </span>

                <div className="flex flex-col sm:flex-row items-center gap-2">
                  <div className="relative flex-1 w-full">
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        setShowSuggestions(true);
                      }}
                      onFocus={() => setShowSuggestions(true)}
                      placeholder="Enter name or email address..."
                      className="w-full px-3 py-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)]"
                    />

                    {/* Autocomplete Suggestions Dropdown */}
                    {showSuggestions && searchQuery.trim() && filteredSuggestions.length > 0 && (
                      <div className="absolute left-0 right-0 top-full mt-1 bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-xl shadow-lg z-20 max-h-40 overflow-y-auto custom-scrollbar p-1">
                        {filteredSuggestions.map((s) => (
                          <div
                            key={s.id}
                            onClick={() => handleAddMember(s)}
                            className="p-2 rounded-lg hover:bg-[var(--bg-surface-container-high)] cursor-pointer flex items-center justify-between gap-2 text-xs"
                          >
                            <div className="flex items-center gap-2">
                              <div className="w-6 h-6 rounded-full bg-[var(--color-primary)] text-white text-[9px] font-bold flex items-center justify-center overflow-hidden shrink-0">
                                {s.avatar ? (
                                  <img src={s.avatar} alt={s.name} className="w-full h-full object-cover" />
                                ) : (
                                  s.initials || s.name.slice(0, 2).toUpperCase()
                                )}
                              </div>
                              <div>
                                <p className="font-semibold text-[var(--text-on-surface)]">{s.name}</p>
                                <p className="text-[10px] text-[var(--text-on-surface-variant)]">{s.email}</p>
                              </div>
                            </div>
                            <RoleBadge role={s.role} size="xs" />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Role Selector Dropdown */}
                  <div className="w-full sm:w-32 shrink-0">
                    <select
                      value={selectedRole}
                      onChange={(e) => setSelectedRole(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-xs font-semibold text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] cursor-pointer"
                    >
                      {rolesList.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Add Button */}
                  <button
                    type="button"
                    onClick={() => handleAddMember()}
                    className="w-full sm:w-auto px-4 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold hover:opacity-90 transition-opacity cursor-pointer shrink-0 flex items-center justify-center gap-1"
                  >
                    <span className="material-symbols-outlined text-sm">person_add</span>
                    <span>Add</span>
                  </button>
                </div>
              </div>

              {/* Members List */}
              <div className="space-y-2 max-h-56 overflow-y-auto custom-scrollbar pr-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] block px-1">
                  Assigned Project Team ({members.length})
                </span>

                {members.map((m) => (
                  <div
                    key={m.id}
                    className="p-2.5 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] flex items-center justify-between gap-3 text-xs"
                  >
                    <div className="flex items-center gap-2.5 overflow-hidden">
                      <div className="w-8 h-8 rounded-full bg-[var(--color-primary)] text-white text-xs font-bold flex items-center justify-center overflow-hidden shrink-0">
                        {m.avatar ? (
                          <img src={m.avatar} alt={m.name} className="w-full h-full object-cover" />
                        ) : (
                          m.initials || m.name.slice(0, 2).toUpperCase()
                        )}
                      </div>
                      <div className="overflow-hidden">
                        <p className="font-bold text-[var(--text-on-surface)] truncate">{m.name}</p>
                        <p className="text-[10px] text-[var(--text-on-surface-variant)] truncate">{m.email}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {/* Interactive Role Switcher Dropdown */}
                      <select
                        value={m.role}
                        onChange={(e) => handleMemberRoleChange(m.id, e.target.value)}
                        className="px-2 py-1 rounded-lg bg-[var(--bg-surface-container-high)] border border-[var(--border-outline-variant)] text-[10px] font-bold text-[var(--text-on-surface)] outline-none cursor-pointer"
                      >
                        {rolesList.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>

                      <RoleBadge role={m.role} size="xs" />

                      {/* Remove Member Button */}
                      {members.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveMember(m.id)}
                          className="p-1 rounded-lg hover:bg-rose-500/10 text-[var(--text-on-surface-variant)] hover:text-rose-600 transition-colors cursor-pointer"
                          title="Remove Member"
                        >
                          <span className="material-symbols-outlined text-base">close</span>
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Form Actions Footer */}
          <div className="flex justify-between items-center pt-4 border-t border-[var(--border-outline-variant)]">
            <span className="text-[10px] text-[var(--text-on-surface-variant)] font-medium">
              {activeTab === 'general' ? 'Next step: Team members & roles' : `${members.length} team members configured`}
            </span>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => dispatch(setCreateProjectModalOpen(false))}
                className="px-4 py-2 rounded-xl bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] text-xs font-semibold hover:bg-[var(--bg-surface-container-highest)] transition-colors cursor-pointer"
              >
                Cancel
              </button>

              {activeTab === 'general' ? (
                <button
                  type="button"
                  onClick={() => setActiveTab('members')}
                  className="px-4 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold shadow-xs hover:opacity-90 cursor-pointer flex items-center gap-1"
                >
                  <span>Members</span>
                  <span className="material-symbols-outlined text-sm">arrow_forward</span>
                </button>
              ) : (
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer flex items-center gap-1"
                >
                  <span className="material-symbols-outlined text-sm">check</span>
                  <span>Create Project</span>
                </button>
              )}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
