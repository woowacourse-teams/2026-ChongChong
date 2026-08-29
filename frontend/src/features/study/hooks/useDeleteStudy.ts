import { useQueryClient, useMutation } from '@tanstack/react-query';
import { removeStudy } from '../api';
import studyQueries from '../queries';

export default function useDeleteStudy() {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => removeStudy(studyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
    },
  });

  return mutation;
}
