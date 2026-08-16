import React from 'react';

export type MemberRole = 'Dev' | 'QA' | 'Designer' | 'PM' | string;

interface RoleBadgeProps {
  role?: string;
  size?: 'xs' | 'sm' | 'md';
  className?: string;
}

export const RoleBadge: React.FC<RoleBadgeProps> = ({ role = 'Dev', size = 'xs', className = '' }) => {
  if (!role) return null;

  const normalized = role.toLowerCase().trim();

  let colorClasses = 'bg-slate-500/15 text-slate-700 dark:text-slate-300 border-slate-500/30';

  if (normalized.includes('dev') || normalized.includes('eng')) {
    colorClasses = 'bg-sky-500/15 text-sky-700 dark:text-sky-300 border-sky-500/30';
  } else if (normalized.includes('qa') || normalized.includes('test')) {
    colorClasses = 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border-emerald-500/30';
  } else if (normalized.includes('design') || normalized.includes('ui') || normalized.includes('art')) {
    colorClasses = 'bg-purple-500/15 text-purple-700 dark:text-purple-300 border-purple-500/30';
  } else if (normalized.includes('pm') || normalized.includes('lead') || normalized.includes('product') || normalized.includes('manager')) {
    colorClasses = 'bg-amber-500/15 text-amber-700 dark:text-amber-300 border-amber-500/30';
  }

  const sizeClasses =
    size === 'xs'
      ? 'px-1.5 py-0.5 text-[9px]'
      : size === 'sm'
      ? 'px-2 py-0.5 text-[10px]'
      : 'px-2.5 py-1 text-xs';

  return (
    <span
      className={`inline-flex items-center font-bold tracking-wider uppercase border rounded-md shrink-0 whitespace-nowrap ${sizeClasses} ${colorClasses} ${className}`}
    >
      {role}
    </span>
  );
};
