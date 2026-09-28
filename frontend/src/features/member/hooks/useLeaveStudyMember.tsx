import { useNavigate } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { leaveStudyMember } from '../api';
import studyQueries from '../../study/queries';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  onError: () => void;
}

export default function useLeaveStudyMember({ onError }: Params) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => leaveStudyMember({ studyId }),
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
