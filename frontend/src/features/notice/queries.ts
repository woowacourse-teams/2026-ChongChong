import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import {
  fetchNoticeList,
  fetchNoticeDetail,
  fetchNoticeMyRead,
  fetchNoticeReadStatus,
} from './api';

const noticeQueries = {
  all: () => ['studies'] as const,

  study: (studyId: number) => [...noticeQueries.all(), studyId] as const,

  lists: (studyId: number) => [...noticeQueries.study(studyId), 'notices'] as const,

  list: (studyId: number) =>
    infiniteQueryOptions({
      queryKey: noticeQueries.lists(studyId),
      queryFn: ({ pageParam }: { pageParam: number | null }) =>
        fetchNoticeList(studyId, pageParam ?? undefined),
      initialPageParam: null,
      getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
    }),

  detail: (studyId: number, noticeId: number) =>
    queryOptions({
      queryKey: [...noticeQueries.lists(studyId), noticeId, 'detail'],
      queryFn: () => fetchNoticeDetail(studyId, noticeId),
    }),

  readStatus: (studyId: number, noticeId: number) =>
    queryOptions({
      queryKey: [...noticeQueries.lists(studyId), noticeId, 'read-status'],
      queryFn: () => fetchNoticeReadStatus(studyId, noticeId),
    }),

  myRead: (studyId: number, noticeId: number) =>
    queryOptions({
      queryKey: [...noticeQueries.lists(studyId), noticeId, 'myRead'],
      queryFn: () => fetchNoticeMyRead(studyId, noticeId),
    }),
};
