import React, { useEffect, useState } from 'react';
import { Outlet, useParams } from 'react-router-dom';
import { NotFoundPage } from '../views/NotFoundPage';
import { apiService } from '../../services/apiService';
import { Project } from '../../types';

export const ProjectLayout: React.FC = () => {
  const { workspaceSlug, projectSlug } = useParams<{ workspaceSlug: string; projectSlug: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    if (!workspaceSlug || !projectSlug) return;

    setIsLoading(true);
    setIsNotFound(false);

    apiService
      .getProjectBySlug(workspaceSlug, projectSlug)
      .then((proj) => {
        if (isMounted) {
          setProject(proj);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.warn('Project not found:', err);
          setIsNotFound(true);
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [workspaceSlug, projectSlug]);

  if (isLoading) {
    return (
      <div className="py-12 flex justify-center items-center text-xs text-[var(--text-on-surface-variant)]">
        <div className="w-5 h-5 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin mr-2"></div>
        <span>Loading project...</span>
      </div>
    );
  }

  if (isNotFound || !project) {
    return <NotFoundPage type="project" />;
  }

  return <Outlet context={{ project }} />;
};
