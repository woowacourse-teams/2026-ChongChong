import { queryOptions } from '@tanstack/react-query';
import { fetchStudies } from './api';

const studyQueries = {
  all: () => ['studies'],
  lists: () => [...studyQueries.all(), 'list'],
  list: () =>
    queryOptions({
      queryKey: [...studyQueries.lists()],
      queryFn: () => fetchStudies(),
    }),
};

export default studyQueries;
