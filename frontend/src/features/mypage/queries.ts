import { queryOptions } from '@tanstack/react-query';
import { getProfile } from './api';

const myPageQueries = {
  profile: () =>
    queryOptions({
      queryKey: ['profile'],
      queryFn: getProfile,
    }),
};

export default myPageQueries;
