import { URL_POLICY } from '../../../shared/constants/urlPolicy';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { removeStudy } from '../api';
import studyQueries from '../queries';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  onError: () => void;
}

export default function useDeleteStudy({ onError }: Params) {
  const { move } = usePageNavigation();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { mutate, isPending } = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => removeStudy(studyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
      move(URL_POLICY.afterStudyDelete());
    },
    onError: (error) => {
      onError();
      toast.open(<StatusToast message={error.message} status={'error'} />);
    },
  });

  return { mutate, isPending };
}
