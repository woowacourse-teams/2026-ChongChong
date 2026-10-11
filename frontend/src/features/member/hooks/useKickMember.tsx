import { useMutation, useQueryClient } from '@tanstack/react-query';
import { kickMember } from '../api';
import { memberQueries } from '../queries';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  onError: () => void;
}

export default function useKickStudyMember({ onError }: Params) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const { mutate } = useMutation({
    mutationFn: ({ studyId, memberId }: { studyId: number; memberId: number }) =>
      kickMember({ studyId, memberId }),
    onSettled: (_data, _error, variables) =>
      queryClient.invalidateQueries({ queryKey: memberQueries.lists(variables.studyId) }),
    onError: (error) => {
      onError();
      toast.open(<StatusToast message={error.message} status="Error" />);
    },
  });

  return { mutate };
}
