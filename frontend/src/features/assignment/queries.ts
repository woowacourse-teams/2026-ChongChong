import { queryOptions } from '@tanstack/react-query';
import { fetchAssignments } from './api';

const assignmentQueries = {
  all: () => ['studies'] as const,

  study: (studyId: number) => [...assignmentQueries.all(), studyId] as const,

  lists: (studyId: number) => [...assignmentQueries.study(studyId), 'assignments'] as const,

  list: (studyId: number) =>
    queryOptions({
      queryKey: assignmentQueries.lists(studyId),
      queryFn: () => fetchAssignments(studyId),
    }),
};

export default assignmentQueries;
