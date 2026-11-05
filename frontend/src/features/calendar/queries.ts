import { queryOptions } from '@tanstack/react-query';
import { fetchMonthlyCalendar } from './api';

const calendarQueries = {
  months: (studyId: number) => ['studies', studyId, 'calendar'] as const,

  month: (studyId: number, year: number, month: number) =>
    queryOptions({
      queryKey: [...calendarQueries.months(studyId), year, month],
      queryFn: () => fetchMonthlyCalendar(studyId, year, month),
      retry: false,
    }),
};

export default calendarQueries;
