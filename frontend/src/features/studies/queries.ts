import { queryOptions } from '@tanstack/react-query';
import { fetchStudies, fetchStudyInfo, fetchStudyInviteLink } from './api';

const studyQueries = {
  all: () => ['studies'],
  lists: () => [...studyQueries.all(), 'list'],
  list: () =>
    queryOptions({
      queryKey: [...studyQueries.lists()],
      queryFn: () => fetchStudies(),
    }),
  details: () => [...studyQueries.all(), 'detail'],
  detail: (studyId: number) => [...studyQueries.details(), studyId],
  info: (studyId: number) =>
    queryOptions({
      queryKey: [...studyQueries.detail(studyId), 'info'],
      queryFn: () => fetchStudyInfo(studyId),
    }),
  inviteLink: (studyId: number) =>
    queryOptions({
      queryKey: [...studyQueries.detail(studyId), 'inviteLink'],
      queryFn: () => fetchStudyInviteLink(studyId),
    }),
};

export default studyQueries;
