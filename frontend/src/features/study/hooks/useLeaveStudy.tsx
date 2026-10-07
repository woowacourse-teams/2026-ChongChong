import { useNavigate } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import studyQueries from '../../study/queries';
import { leaveStudy } from '../api';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  onError: () => void;
}

export default function useLeaveStudy({ onError }: Params) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => leaveStudy({ studyId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
      navigate('/studies');
    },
    onError: (error) => {
      onError();
      toast.open(<StatusToast message={error.message} status="Error" />);
    },
  });

  return mutation;
}
