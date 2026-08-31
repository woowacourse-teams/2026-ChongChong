import { useMutation } from '@tanstack/react-query';
import { joinStudy } from '../api';

export default function useStudyJoin() {
  const mutation = useMutation({
    mutationFn: ({ token }: { token: string }) => joinStudy({ token }),
  });

  return mutation;
}
