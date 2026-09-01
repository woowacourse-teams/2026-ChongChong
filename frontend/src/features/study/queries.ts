import { queryOptions } from '@tanstack/react-query';
import { Role } from './types';
import { fetchStudies, fetchStudyInfo, fetchStudyInviteLink, fetchStudyDetail } from './api';

const studyQueries = {
  all: () => ['studies'],
  lists: () => [...studyQueries.all(), 'list'],
  list: () =>
    queryOptions({
      queryKey: [...studyQueries.lists()],
      queryFn: () => fetchStudies(),
    }),
  details: () => [...studyQueries.all(), 'detail'],
  detail: <R extends Role>(studyId: number, role: R) =>
    queryOptions({
      queryKey: [...studyQueries.details(), studyId, role],
      queryFn: () => fetchStudyDetail(studyId, role),
    }),
  info: (studyId: number) =>
    queryOptions({
      queryKey: [...studyQueries.details(), studyId, 'info'],
      queryFn: () => fetchStudyInfo(studyId),
    }),
  inviteLink: (studyId: number) =>
    queryOptions({
      queryKey: [...studyQueries.details(), studyId, 'inviteLink'],
      queryFn: () => fetchStudyInviteLink(studyId),
    }),
};

export default studyQueries;
