import { useMutation, useQueryClient } from '@tanstack/react-query';
import { leaveStudyMember } from '../api';
import studyQueries from '../../studies/queries';

export default function useLeaveStudyMember() {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => leaveStudyMember({ studyId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
    },
  });

  return mutation;
}
