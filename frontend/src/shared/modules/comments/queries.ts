import { infiniteQueryOptions, type InfiniteData } from '@tanstack/react-query';
import { fetchComments } from './api';
import type { CommentTarget } from './types';
import type { CommentListResponse } from './responseSchemas';

export type CommentListData = Pick<CommentListResponse, 'comments' | 'totalCount'>;

function selectCommentList(data: InfiniteData<CommentListResponse>): CommentListData {
  return {
    comments: data.pages.flatMap((page) => page.comments),
    totalCount: data.pages.at(-1)?.totalCount ?? 0,
  };
}

const commentQueries = {
  all: () => ['comments'] as const,
  target: (target: CommentTarget) => [...commentQueries.all(), target] as const,
  list: (target: CommentTarget) =>
    infiniteQueryOptions({
      queryKey: [...commentQueries.target(target), 'list'],
      queryFn: ({ pageParam, signal }) => fetchComments(target, pageParam, signal),
      initialPageParam: null as number | null,
      getNextPageParam: (lastPage) =>
        lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined,
      select: selectCommentList,
      retry: false,
    }),
};

export default commentQueries;
