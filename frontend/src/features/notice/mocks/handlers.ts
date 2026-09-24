import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { invalidInputResponse } from '../../../mocks/errors';
import { paginateByCursor } from '../../../mocks/pagination';
import { memberTable } from '../../member/mocks/db';
import type { NoticeFormValues, UpdateNoticeValue } from '../types';
import { noticeRecipientTable, noticeTable, type NoticeSchemaType } from './db';
import { validateNotice } from './validators';

function localDateTimeNow() {
  return new Date().toISOString();
}

function notFound(code: string, message: string) {
  return HttpResponse.json({ code, message }, { status: 404 });
}

function findStudyMember(studyId: number, userId: number) {
  return memberTable.findFirst((query) => query.where({ studyId, userId }));
}

function findNotice(studyId: number, noticeId: number) {
  return noticeTable.findFirst((query) => query.where({ id: noticeId, studyId }));
}

function findRecipient(noticeId: number, memberId: number) {
  return noticeRecipientTable.findFirst((query) => query.where({ noticeId, memberId }));
}

export const handlers = [
  http.get(`${API_URL}/studies/:studyId/notices`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const studyId = Number(params.studyId);
    const member = findStudyMember(studyId, user.id);
    if (!member) return new HttpResponse(null, { status: 403 });

    const studyNotices: NoticeSchemaType[] = noticeTable.findMany((query) =>
      query.where({ studyId }),
    );
    const searchParams = new URL(request.url).searchParams;
    const requestedSize = Number(searchParams.get('size'));
    const pageSize = requestedSize > 0 ? requestedSize : undefined;
    const { page, nextCursor, hasNext } = paginateByCursor(studyNotices, searchParams, pageSize);

    const notices = page.map((notice) => {
      const common = {
        id: notice.id,
        title: notice.title,
        content: notice.content,
        createdAt: notice.createdAt,
      };

      if (member.role === 'LEADER') {
        const recipients = noticeRecipientTable.findMany((query) =>
          query.where({ noticeId: notice.id }),
        );
        const readRecipientCount = recipients.filter(({ readAt }) => readAt !== null).length;

        return {
          ...common,
          recipientCount: recipients.length,
          readRecipientCount,
          isComplete: recipients.length === readRecipientCount,
        };
      }

      const recipient = findRecipient(notice.id, member.id);
      const readStatus = !recipient ? 'NOT_ASSIGNED' : recipient.readAt ? 'READ' : 'UNREAD';

      return { ...common, readStatus };
    });

    return HttpResponse.json({
      nextCursor: hasNext ? nextCursor : null,
      hasNext,
      notices,
    });
  }),

  http.post(`${API_URL}/studies/:studyId/notices`, async ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const studyId = Number(params.studyId);
    const member = findStudyMember(studyId, user.id);
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const body = (await request.json()) as NoticeFormValues;
    const fieldErrors = validateNotice(body);
    if (fieldErrors.length > 0) return invalidInputResponse(fieldErrors);

    const notices = noticeTable.findMany();
    const noticeId = Math.max(0, ...notices.map(({ id }) => id)) + 1;
    await noticeTable.create({
      id: noticeId,
      studyId,
      title: body.title,
      content: body.content,
      createdAt: localDateTimeNow(),
    });

    const recipients = memberTable.findMany((query) => query.where({ studyId, role: 'MEMBER' }));
    const existingRecipients = noticeRecipientTable.findMany();
    let recipientId = Math.max(0, ...existingRecipients.map(({ id }) => id)) + 1;

    for (const recipient of recipients) {
      await noticeRecipientTable.create({
        id: recipientId,
        noticeId,
        memberId: recipient.id,
        readAt: null,
        lastRemindAt: null,
      });
      recipientId += 1;
    }

    return HttpResponse.json({ noticeId }, { status: 201 });
  }),

  http.get(`${API_URL}/studies/:studyId/notices/:noticeId/status`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const [studyId, noticeId] = [params.studyId, params.noticeId].map(Number);
    const member = findStudyMember(studyId, user.id);
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const notice = findNotice(studyId, noticeId);
    if (!notice) return notFound('NOTICE_NOT_FOUND', '존재하지 않는 공지입니다.');

    const recipients = noticeRecipientTable.findMany((query) => query.where({ noticeId }));
    const readMembers = recipients
      .filter(({ readAt }) => readAt !== null)
      .flatMap(({ memberId }) => {
        const recipient = memberTable.findFirst((query) => query.where({ id: memberId, studyId }));
        return recipient
          ? [{ id: recipient.id, name: recipient.name, profileImage: recipient.profileImage }]
          : [];
      });
    const unreadMembers = recipients
      .filter(({ readAt }) => readAt === null)
      .flatMap(({ memberId, lastRemindAt }) => {
        const recipient = memberTable.findFirst((query) => query.where({ id: memberId, studyId }));
        return recipient
          ? [
              {
                id: recipient.id,
                name: recipient.name,
                profileImage: recipient.profileImage,
                lastRemindAt,
              },
            ]
          : [];
      });

    return HttpResponse.json({
      id: notice.id,
      memberCount: readMembers.length + unreadMembers.length,
      readCount: readMembers.length,
      unreadCount: unreadMembers.length,
      remindAt: null,
      readMembers,
      unreadMembers,
    });
  }),

  http.get(`${API_URL}/studies/:studyId/notices/:noticeId/status/me`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const [studyId, noticeId] = [params.studyId, params.noticeId].map(Number);
    const member = findStudyMember(studyId, user.id);
    if (!member) return new HttpResponse(null, { status: 403 });

    const notice = findNotice(studyId, noticeId);
    if (!notice) return notFound('NOTICE_NOT_FOUND', '존재하지 않는 공지입니다.');

    const recipient = findRecipient(noticeId, member.id);
    if (!recipient) return HttpResponse.json({ readStatus: 'NOT_ASSIGNED' as const });
    if (!recipient.readAt) return HttpResponse.json({ readStatus: 'UNREAD' as const });

    return HttpResponse.json({ readStatus: 'READ' as const, readAt: recipient.readAt });
  }),

  http.patch(`${API_URL}/studies/:studyId/notices/:noticeId/read`, async ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const [studyId, noticeId] = [params.studyId, params.noticeId].map(Number);
    const member = findStudyMember(studyId, user.id);
    if (!member) return new HttpResponse(null, { status: 403 });

    const notice = findNotice(studyId, noticeId);
    if (!notice) return notFound('NOTICE_NOT_FOUND', '존재하지 않는 공지입니다.');

    const recipient = findRecipient(noticeId, member.id);
    if (!recipient) {
      return notFound('NOTICE_RECIPIENT_NOT_FOUND', '공지 수신자 정보를 찾을 수 없습니다.');
    }

    const readAt = recipient.readAt ?? localDateTimeNow();
    noticeRecipientTable.delete((query) => query.where({ id: recipient.id }));
    await noticeRecipientTable.create({
      id: recipient.id,
      noticeId: recipient.noticeId,
      memberId: recipient.memberId,
      readAt,
      lastRemindAt: recipient.lastRemindAt,
    });

    return HttpResponse.json({ readAt });
  }),

  http.get(`${API_URL}/studies/:studyId/notices/:noticeId`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const [studyId, noticeId] = [params.studyId, params.noticeId].map(Number);
    const member = findStudyMember(studyId, user.id);
    if (!member) return new HttpResponse(null, { status: 403 });

    const notice = findNotice(studyId, noticeId);
    if (!notice) return notFound('NOTICE_NOT_FOUND', '존재하지 않는 공지입니다.');

    return HttpResponse.json({
      id: notice.id,
      title: notice.title,
      content: notice.content,
      createdAt: notice.createdAt,
    });
  }),

  http.patch(`${API_URL}/studies/:studyId/notices/:noticeId`, async ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const [studyId, noticeId] = [params.studyId, params.noticeId].map(Number);
    const member = findStudyMember(studyId, user.id);
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const notice = findNotice(studyId, noticeId);
    if (!notice) return notFound('NOTICE_NOT_FOUND', '존재하지 않는 공지입니다.');

    const body = (await request.json()) as UpdateNoticeValue;
    const fieldErrors = validateNotice(body, true);
    if (fieldErrors.length > 0) return invalidInputResponse(fieldErrors);

    noticeTable.delete((query) => query.where({ id: notice.id, studyId }));
    await noticeTable.create({
      id: notice.id,
      studyId: notice.studyId,
      title: body.title ?? notice.title,
      content: body.content ?? notice.content,
      createdAt: notice.createdAt,
    });

    return new HttpResponse(null, { status: 204 });
  }),

  http.delete(`${API_URL}/studies/:studyId/notices/:noticeId`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const [studyId, noticeId] = [params.studyId, params.noticeId].map(Number);
    const member = findStudyMember(studyId, user.id);
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const notice = findNotice(studyId, noticeId);
    if (!notice) return notFound('NOTICE_NOT_FOUND', '존재하지 않는 공지입니다.');

    noticeRecipientTable.delete((query) => query.where({ noticeId }));
    noticeTable.delete((query) => query.where({ id: noticeId, studyId }));

    return new HttpResponse(null, { status: 204 });
  }),
];
