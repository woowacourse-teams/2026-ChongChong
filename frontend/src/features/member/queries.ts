import { queryOptions } from '@tanstack/react-query';
import { fetchMembers } from './api';

export const memberQueries = {
  all: () => ['members'],
  study: (studyId: number) => [...memberQueries.all(), studyId],
  lists: (studyId: number) => [...memberQueries.study(studyId), 'list'],
  list: (studyId: number) =>
    queryOptions({
      queryKey: [...memberQueries.lists(studyId)],
      queryFn: () => fetchMembers(studyId),
    }),
};
