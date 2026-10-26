import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import notificationQueries from '../../notification/queries';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { enablePush, disablePush } from '../../notification/push';
import { getOrCreateInstallationId } from '../../notification/localPush';

export default function useNotificationEnabledState() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [installationId] = useState(getOrCreateInstallationId);
  const statusQuery = notificationQueries.pushStatus(installationId);
  const { data, isPending, isError, isFetching, refetch } = useQuery(statusQuery);
  const isEnabled =
    data?.isActive === true && 'Notification' in window && Notification.permission === 'granted';
  const [changing, startChanging, endChanging] = useBooleanState();

  async function disableNotifications() {
    try {
      await disablePush();
    } finally {
      await queryClient.invalidateQueries({ queryKey: statusQuery.queryKey });
    }
  }

  async function handleToggleNotificationEnabled() {
    if (changing || isPending || isError || isFetching) return;

    startChanging();

    try {
      const nextEnabledState = !isEnabled;

      if (nextEnabledState) {
        if (!(await enablePush())) return;
        await queryClient.invalidateQueries({ queryKey: statusQuery.queryKey });
      } else {
        await disableNotifications();
      }
    } catch (error) {
      toast.open(
        <StatusToast
          message={
            error instanceof Error
              ? error.message
              : '알림 설정을 변경하지 못했어요. 다시 시도해 주세요.'
          }
          status="error"
        />,
      );
    } finally {
      endChanging();
    }
  }

  return {
    isEnabled,
    changing,
    isLoading: isPending || isFetching,
    isError,
    refetch,
    handleToggleNotificationEnabled,
    disableNotifications,
  };
}
