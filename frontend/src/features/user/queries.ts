import { queryOptions } from '@tanstack/react-query';
import { getProfile } from './api';

const userQueries = {
  profile: () =>
    queryOptions({
      queryKey: ['profile'],
      queryFn: getProfile,
    }),
};

export default userQueries;
