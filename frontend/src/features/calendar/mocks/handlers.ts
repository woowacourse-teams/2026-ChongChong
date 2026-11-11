import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { studyTable } from '../../study/mocks/db';
import { memberTable } from '../../member/mocks/db';
import { noticeTable, noticeRecipientTable } from '../../notice/mocks/db';
import {
  assignmentTable,
  submissionTable,
  type SubmissionSchemaType,
} from '../../assignment/mocks/db';
import { CALENDAR_URLS } from '../urls';

interface CalendarEvent {
  eventType: 'NOTICE_CREATED' | 'ASSIGNMENT_CREATED' | 'ASSIGNMENT_DUE';
  resourceType: 'NOTICE' | 'ASSIGNMENT';
  resourceId: number;
  title: string;
  eventAt: string;
  readStatus?: 'NOT_ASSIGNED' | 'READ' | 'UNREAD';
  submissionStatus?: 'NOT_SUBMITTED' | 'MISSING' | 'SUBMITTED' | 'LATE_SUBMITTED';
}

const eventOrder = { NOTICE_CREATED: 0, ASSIGNMENT_CREATED: 1, ASSIGNMENT_DUE: 2 };

function errorResponse(code: string, message: string, status: number) {
  return HttpResponse.json({ code, message }, { status });
}

function toKoreanTimestamp(value: string) {
  const withOffset = /(?:Z|[+-]\d{2}:\d{2})$/.test(value) ? value : value + '+09:00';
  return new Date(withOffset).getTime();
}

function getSubmissionStatus(submission: SubmissionSchemaType, deadline: number, now: number) {
  if (submission.submitted && submission.createdAt) {
    return toKoreanTimestamp(submission.createdAt) <= deadline ? 'SUBMITTED' : 'LATE_SUBMITTED';
  }
  return now <= deadline ? 'NOT_SUBMITTED' : 'MISSING';
}

function koreanEventAt(value: string) {
  if (!/(?:Z|[+-]\d{2}:\d{2})$/.test(value)) return value;
  return new Date(toKoreanTimestamp(value) + 9 * 60 * 60 * 1000)
    .toISOString()
    .replace(/\.000Z$/, '')
    .replace(/Z$/, '');
}

export const handlers = [
  http.get(`${API_URL}${CALENDAR_URLS.monthly.mock}`, ({ request, params }) => {
    const query = new URL(request.url).searchParams;
    const yearValue = query.get('year');
    const monthValue = query.get('month');
    if (
      !yearValue ||
      !monthValue ||
      !/^[+-]?\d+$/.test(yearValue) ||
      !/^[+-]?\d+$/.test(monthValue)
    ) {
      return errorResponse('INVALID_REQUEST', '연도와 월은 필수 정수입니다.', 400);
    }
    const year = Number(yearValue);
    const month = Number(monthValue);
    const studyId = Number(params.studyId);
    if (
      !Number.isInteger(studyId) ||
      studyId < 1 ||
      year < 1 ||
      year > 9998 ||
      month < 1 ||
      month > 12
    ) {
      return errorResponse('INVALID_REQUEST_PARAMETER', '조회 범위를 확인해주세요.', 400);
    }

    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    if (!studyTable.findFirst((q) => q.where({ id: studyId }))) {
      return errorResponse('STUDY_NOT_FOUND', '스터디를 찾을 수 없습니다.', 404);
    }
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (!member) return errorResponse('STUDY_ACCESS_DENIED', '스터디에 접근할 수 없습니다.', 403);

    const monthPrefix = `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-`;
    const now = Date.now();
    const events: CalendarEvent[] = [];

    for (const notice of noticeTable.findMany((q) => q.where({ studyId }))) {
      const createdAt = koreanEventAt(notice.createdAt);
      if (!createdAt.startsWith(monthPrefix)) continue;
      const recipient = noticeRecipientTable.findFirst((q) =>
        q.where({ noticeId: notice.id, memberId: member.id }),
      );
      events.push({
        eventType: 'NOTICE_CREATED',
        resourceType: 'NOTICE',
        resourceId: notice.id,
        title: notice.title,
        eventAt: createdAt,
        readStatus: !recipient ? 'NOT_ASSIGNED' : recipient.readAt ? 'READ' : 'UNREAD',
      });
    }

    for (const assignment of assignmentTable.findMany((q) => q.where({ studyId }))) {
      const submission = submissionTable.findFirst((q) =>
        q.where({ assignmentId: assignment.id, userId: user.id }),
      );
      if (!submission?.assigned) continue;

      const deadline = toKoreanTimestamp(assignment.closeAt);
      const submissionStatus = getSubmissionStatus(submission, deadline, now);

      for (const [eventType, sourceAt] of [
        ['ASSIGNMENT_CREATED', assignment.createdAt],
        ['ASSIGNMENT_DUE', assignment.closeAt],
      ] as const) {
        const eventAt = koreanEventAt(sourceAt);
        if (!eventAt.startsWith(monthPrefix)) continue;
        events.push({
          eventType,
          resourceType: 'ASSIGNMENT',
          resourceId: assignment.id,
          title: assignment.title,
          eventAt,
          submissionStatus,
        });
      }
    }

    events.sort(
      (a, b) =>
        toKoreanTimestamp(a.eventAt) - toKoreanTimestamp(b.eventAt) ||
        eventOrder[a.eventType] - eventOrder[b.eventType] ||
        a.resourceId - b.resourceId,
    );
    return HttpResponse.json({ year, month, events });
  }),
];
