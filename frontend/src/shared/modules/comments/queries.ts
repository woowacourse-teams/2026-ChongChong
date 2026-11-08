import { infiniteQueryOptions } from '@tanstack/react-query';
import { fetchComments } from './api';
import type { CommentTarget } from './types';

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
      retry: false,
    }),
};

export default commentQueries;
