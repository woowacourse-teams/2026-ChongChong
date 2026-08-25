import { queryOptions } from '@tanstack/react-query';
import { fetchMembers } from './api';

export const memberQueries = {
  all: () => ['studies'],
  study: (studyId: number) => [...memberQueries.all(), studyId],
  lists: (studyId: number) => [...memberQueries.study(studyId), 'members'],
  list: (studyId: number) =>
    queryOptions({
      queryKey: [...memberQueries.lists(studyId)],
      queryFn: () => fetchMembers(studyId),
    }),
};
