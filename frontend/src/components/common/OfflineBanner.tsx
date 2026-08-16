import React, { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import { setOnlineStatus } from '../../store/themeSlice';

export const OfflineBanner: React.FC = () => {
  const dispatch = useAppDispatch();
  const { isOnline } = useAppSelector((state) => state.theme);
  const [showSyncSuccess, setShowSyncSuccess] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      dispatch(setOnlineStatus(true));
      setShowSyncSuccess(true);
      setTimeout(() => setShowSyncSuccess(false), 4000);
    };

    const handleOffline = () => {
      dispatch(setOnlineStatus(false));
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [dispatch]);

  if (isOnline && !showSyncSuccess) return null;

  return (
    <div
      className={`fixed bottom-4 right-4 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-xl text-xs font-semibold backdrop-blur-md transition-all duration-300 ${
        !isOnline
          ? 'bg-amber-600/90 text-white border border-amber-400/30'
          : 'bg-emerald-600/90 text-white border border-emerald-400/30'
      }`}
    >
      <span className="material-symbols-outlined text-lg">
        {!isOnline ? 'wifi_off' : 'sync'}
      </span>
      <div>
        <p className="font-bold">
          {!isOnline ? 'You are currently working offline' : 'Back online! Synced local updates.'}
        </p>
        <p className="text-[10px] opacity-90">
          {!isOnline
            ? 'All project, task & board updates are saved locally to Redux storage.'
            : 'Changes synced with local state engine.'}
        </p>
      </div>
    </div>
  );
};
