import { queryOptions } from '@tanstack/react-query';
import { fetchStudies, fetchStudyInfo } from './api';

const studyQueries = {
  all: () => ['studies'],
  lists: () => [...studyQueries.all(), 'list'],
  list: () =>
    queryOptions({
      queryKey: [...studyQueries.lists()],
      queryFn: () => fetchStudies(),
    }),
  details: () => [...studyQueries.all(), 'detail'],
  detail: (studyId: string) => [...studyQueries.details(), studyId],
  info: (studyId: string) =>
    queryOptions({
      queryKey: [...studyQueries.detail(studyId), 'info'],
      queryFn: () => fetchStudyInfo(studyId),
    }),
};

export default studyQueries;
