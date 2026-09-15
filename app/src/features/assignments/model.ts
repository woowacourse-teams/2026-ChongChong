import type { NoticeImage } from '../notices/model';

export type Submission = {
  readonly body: string;
  readonly link: string;
  readonly files: readonly NoticeImage[];
  readonly submittedAt: string;
};
export type AssignmentMember = {
  readonly id: string;
  readonly name: string;
  readonly submission: Submission | null;
  readonly remindedAt: string | null;
};
export type AssignmentDraft = {
  readonly title: string;
  readonly body: string;
  readonly method: string;
  readonly deadline: string;
  readonly visibility: 'public' | 'private';
  readonly leaderParticipates: boolean;
  readonly reminders: readonly string[];
  readonly images: readonly NoticeImage[];
};
export type Assignment = AssignmentDraft & {
  readonly id: string;
  readonly members: readonly AssignmentMember[];
};
export const assignmentBody =
  '백준에서 문제 푸시고 링크 올려주시면 됩니다.\n그리디 문제집에서 원하는 세 문제를 풀고 올려주세요.';
export const assignmentMethod =
  'GitHub 저장소에 문제 번호로 폴더를 만들어 올린 뒤, 저장소나 PR 링크를 제출해주세요.';
export const sampleSubmission: Submission = {
  body: assignmentBody,
  link: 'https://github.com/antoliny/algo-week3',
  files: [],
  submittedAt: '8월 3일 21:02',
};
export const memberFixtures: readonly AssignmentMember[] = [
  {
    id: 'reader-1',
    name: '안톨리니',
    submission: sampleSubmission,
    remindedAt: null,
  },
  {
    id: 'reader-2',
    name: '안톨리니',
    submission: sampleSubmission,
    remindedAt: null,
  },
  {
    id: 'self',
    name: '바니 (나)',
    submission: null,
    remindedAt: '8월 3일 21:02',
  },
  { id: 'reader-4', name: '피즈', submission: null, remindedAt: null },
];
export function createAssignmentFixtures(
  member: boolean,
): readonly Assignment[] {
  return Array.from({ length: member ? 3 : 2 }, (_, index) => ({
    id: index === 0 ? 'assignment-react' : `assignment-${index}`,
    title:
      member || index ? '디자인 완성하고 제출하기' : '이번주 그리디 3문제 풀이',
    body:
      member || index
        ? '오늘의 과제는 디자인 완성하고 디자인 시스템 정리하기입니다'
        : assignmentBody,
    method: assignmentMethod,
    deadline: `2026-11-${index ? '24' : '21'}T23:59`,
    visibility: index === 1 ? 'private' : 'public',
    leaderParticipates: index !== 1,
    reminders: ['2026-08-05T18:00'],
    images: [],
    members: memberFixtures.flatMap((person) => {
      if (
        person.id === 'self' &&
        ((member && index === 2) || (!member && index === 1))
      )
        return [];
      return [
        {
          ...person,
          submission:
            member && index === 0 && person.id === 'self'
              ? sampleSubmission
              : person.submission,
        },
      ];
    }),
  }));
}
export function validSubmissionLink(link: string): boolean {
  if (!link.trim()) return true;
  try {
    const url = new URL(link);
    return (
      ['https:', 'http:'].includes(url.protocol) &&
      url.hostname.includes('.') &&
      !url.username &&
      !url.password
    );
  } catch (error) {
    if (error instanceof TypeError) return false;
    throw error;
  }
}
export function formatAssignmentDate(value: string) {
  return `${Number(value.slice(0, 4))}년 ${Number(value.slice(5, 7))}월 ${Number(value.slice(8, 10))}일 ${value.slice(11)}`;
}
