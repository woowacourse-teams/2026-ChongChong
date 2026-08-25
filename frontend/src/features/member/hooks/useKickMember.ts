import { useMutation, useQueryClient } from '@tanstack/react-query';
import { kickMember } from '../api';
import { memberQueries } from '../queries';

export default function useKickStudyMember() {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: ({ studyId, memberId }: { studyId: number; memberId: number }) =>
      kickMember({ studyId, memberId }),
    onSettled: (_data, _error, variables) =>
      queryClient.invalidateQueries({ queryKey: memberQueries.lists(variables.studyId) }),
  });

  return mutation;
}
