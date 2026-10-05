import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { updateProfileName } from '../api';
import userQueries from '../queries';

interface Params {
  onSuccess: () => void;
}

export default function useUpdateProfileName({ onSuccess }: Params) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { mutate, isPending, error, reset } = useMutation({
    mutationFn: updateProfileName,
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(userQueries.profile().queryKey, updatedProfile);
      onSuccess();
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === 'INVALID_USER_NAME') return;
      toast.open(<StatusToast status="Error" message={error.message} />);
    },
  });

  const errorText = useMemo(
    () =>
      error instanceof ApiError && error.code === 'INVALID_USER_NAME' ? error.message : undefined,
    [error],
  );

  return { mutate, isPending, errorText, reset };
}
