import api from '../../client';
import { ApiError, handleError } from '../../shared/api/error';
import { isMonthlyCalendarResponse } from './responseSchemas';
import { CALENDAR_URLS } from './urls';

export async function fetchMonthlyCalendar(studyId: number, year: number, month: number) {
  try {
    const response = await api.get(CALENDAR_URLS.monthly.path(studyId), {
      searchParams: { year, month },
    });
    const data: unknown = await response.json();

    if (!isMonthlyCalendarResponse(data)) {
      throw new Error('월간 일정 응답 형식이 올바르지 않습니다.');
    }
    if (data.year !== year || data.month !== month) {
      throw new Error('요청한 월과 일정 응답의 월이 일치하지 않습니다.');
    }

    return data;
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('월간 일정을 불러오는 데 실패했습니다.', { cause: error }),
    });
  }
}
