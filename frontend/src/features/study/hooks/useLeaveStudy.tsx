import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import studyQueries from '../../study/queries';
import { leaveStudy } from '../api';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  onError: () => void;
}

export default function useLeaveStudy({ onError }: Params) {
  const { move } = usePageNavigation();
  const queryClient = useQueryClient();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => leaveStudy({ studyId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
      move(URL_POLICY.afterStudyLeave());
    },
    onError: (error) => {
      onError();
      toast.open(<StatusToast message={error.message} status="error" />);
    },
  });

  return mutation;
}
