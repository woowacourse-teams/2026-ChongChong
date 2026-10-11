import { useMutation, useQueryClient } from '@tanstack/react-query';
import { markNotificationAsRead } from '../api';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { Notification } from '../types';
import notificationQueries from '../queries';

export default function useMarkNotificationAsRead() {
  const toast = useToast();
  const queryClient = useQueryClient();

  const { mutate } = useMutation({
    mutationFn: markNotificationAsRead,
    onError: (error) => {
      toast.open(<StatusToast message={error.message} status={'Error'} />);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationQueries.lists() });
    },
  });

  const markAsRead = (notification: Notification) => {
    if (notification.isRead) return;
    mutate(notification.id);
  };

  return { markAsRead };
}
