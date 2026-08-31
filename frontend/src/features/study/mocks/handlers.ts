import { http, HttpResponse } from 'msw';
import { studyTable } from './db';
import { API_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable } from '../../member/mocks/db';

export const handlers = [
  http.get(`${API_URL}${STUDY_URLS.list}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const memberships = await memberTable.findMany((q) => q.where({ userId: user.id }));
    const studies = await Promise.all(
      memberships.map(async (membership) => {
        const study = await studyTable.findFirst((q) => q.where({ id: membership.studyId }));
        if (!study) return null;
        const members = await memberTable.findMany((q) =>
          q.where({ studyId: study.id, userId: user.id }),
        );
        return {
          id: String(study.id),
          role: membership.role,
          name: study.name,
          description: study.description,
          memberCount: members.length,
          // 공지/과제는 아직 mock table 이 없어 고정값을 사용합니다.
          noticeCount: 2,
          assignmentCount: 2,
        };
      }),
    );
    return HttpResponse.json({ studies: studies.filter((study) => study !== null) });
  }),

  http.get(`${API_URL}${STUDY_URLS.detail}`, async ({ request, params }) => {
    const { studyId } = params;
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const member = memberTable.findFirst((q) =>
      q.where({ studyId: Number(studyId), userId: user.id }),
    );
    if (!member) return new HttpResponse(null, { status: 403 });
    const isLead = member.role === 'LEADER';
    // 과제/공지 MSW가 존재하지 않아 임시 데이터를 사용합니다.
    if (isLead) {
      return HttpResponse.json({
        notices: {
          count: 1,
          items: [
            {
              id: 1,
              title: '판교 스터디룸에서 만나도록 합시다',
              memberCount: 4,
              completeCount: 2,
            },
          ],
        },
        assignments: {
          count: 1,
          items: [
            {
              id: 1,
              title: '그리디 3문제 풀기',
              memberCount: 4,
              completeCount: 2,
            },
          ],
        },
      });
    } else {
      return HttpResponse.json({
        totalCount: 4,
        notices: [
          {
            id: 1,
            title: '판교 스터디룸에서 만나도록 합시다',
          },
        ],
        assignments: [
          {
            id: 1,
            title: '그리디 3문제 풀기',
          },
        ],
      });
    }
  }),

  http.post(`${API_URL}${STUDY_URLS.create}`, async ({ request }) => {
    const body = (await request.json()) as { name: string; description: string };
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    // msw 로직은 실제 backend API 로 대체될 예정입니다.
    // if (invalidInput) {
    //   return HttpResponse.json(invalidInput, { status: 400 });
    // }

    const studyId = Date.now();
    await studyTable.create({ id: studyId, inviteLink: 'chongchong.app/join/new', ...body });
    await memberTable.create({
      id: Date.now(),
      studyId,
      userId: user.id,
      name: user.name,
      profileImage: user.profileImage,
      role: 'LEADER',
    });
    return HttpResponse.json({ studyId }, { status: 201 });
  }),

  http.post(`${API_URL}${STUDY_URLS.join}`, async ({ request }) => {
    const { token } = (await request.json()) as { token: string };
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const study = studyTable.findFirst((q) => q.where({ inviteLink: token }));
    if (!study) return new HttpResponse(null, { status: 404 });
    const newMember = {
      id: Date.now(),
      studyId: study.id,
      userId: user.id,
      name: user.name,
      profileImage: user.profileImage,
      role: 'MEMBER' as const,
    };
    await memberTable.create(newMember);

    return HttpResponse.json({
      studyId: study.id,
    });
  }),

  http.get(`${API_URL}${STUDY_URLS.info}`, async ({ request, params }) => {
    const { studyId } = params;
    const study = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!study) return new HttpResponse(null, { status: 404 });
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const member = await memberTable.findFirst((q) =>
      q.where({ studyId: study.id, userId: user.id }),
    );
    if (!member) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      studyName: study.name,
      role: member.role,
      userName: member.name,
    });
  }),

  http.get(`${API_URL}${STUDY_URLS.inviteLink}`, async ({ params }) => {
    const { studyId } = params;
    const found = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!found) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      inviteLink: found.inviteLink,
    });
  }),

  http.delete(`${API_URL}${STUDY_URLS.remove}`, async ({ params }) => {
    const { studyId } = params;
    const study = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!study) return new HttpResponse(null, { status: 404 });
    studyTable.delete(study);
    return new HttpResponse(null, { status: 204 });
  }),
];
