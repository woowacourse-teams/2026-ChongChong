import { useMutation } from '@tanstack/react-query';
import { withdrawAccount } from '../api';
import { clearAccessToken } from '../../login/accessToken';
import { clearLocalPushSubscription } from '../../notification/localPush';

export default function useWithdrawAccount() {
  return useMutation({
    mutationFn: withdrawAccount,
    onSuccess: async () => {
      clearAccessToken();
      await clearLocalPushSubscription().catch(console.error);
    },
  });
}
