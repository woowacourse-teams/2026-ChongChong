import { useMutation } from '@tanstack/react-query';
import { withdrawAccount } from '../api';

export default function useWithdrawAccount() {
  return useMutation({ mutationFn: withdrawAccount });
}
