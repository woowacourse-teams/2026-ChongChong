import { useNavigate } from 'react-router';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { removeStudy } from '../api';
import studyQueries from '../queries';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  onError: () => void;
}

export default function useDeleteStudy({ onError }: Params) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { mutate, isPending } = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => removeStudy(studyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
      navigate('/studies');
    },
    onError: (error) => {
      onError();
      toast.open(<StatusToast message={error.message} status={'Error'} />);
    },
  });

  return { mutate, isPending };
}
