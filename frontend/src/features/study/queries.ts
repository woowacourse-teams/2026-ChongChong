import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import { Role, StudyJoinTarget } from './types';
import {
  fetchStudies,
  fetchPublicStudies,
  fetchStudyInfo,
  fetchStudyInviteLink,
  fetchStudyDetail,
  fetchStudyJoinInfo,
} from './api';

const studyQueries = {
  all: () => ['studies'],
  lists: () => [...studyQueries.all(), 'list'],
  list: () =>
    queryOptions({
      queryKey: [...studyQueries.lists()],
      queryFn: () => fetchStudies(),
    }),
  publicList: (keyword = '') =>
    infiniteQueryOptions({
      queryKey: [...studyQueries.lists(), 'public', keyword],
      queryFn: ({ pageParam }: { pageParam: number | null }) =>
        fetchPublicStudies(pageParam ?? undefined, keyword),
      initialPageParam: null,
      getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
    }),
  details: () => [...studyQueries.all(), 'detail'],
  joinInfo: (target: string | StudyJoinTarget) =>
    queryOptions({
      queryKey: [...studyQueries.all(), 'join-info', target],
      queryFn: ({ signal }) => fetchStudyJoinInfo(target, signal),
      retry: false,
    }),
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
