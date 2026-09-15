export type StudyActivity = {
  readonly id: string;
  readonly kind: 'notice' | 'assignment';
  readonly title: string;
  readonly body: string;
  readonly readCount: number;
  readonly totalCount: number;
  readonly read: boolean;
  readonly target?: boolean;
};

export type ActivityNotification = {
  readonly id: string;
  readonly studyId: string;
  readonly activityId: string;
  readonly kind: 'reminder' | 'notice';
  readonly title: string;
  readonly description: string;
  readonly time: string;
  readonly read: boolean;
};

export const activityFixtures: readonly StudyActivity[] = [
  {
    id: 'notice-august',
    kind: 'notice',
    title: '8월 스터디 운영 방식이 바뀝니다',
    body: '4주차부터 선택 미션이 추가됩니다',
    readCount: 2,
    totalCount: 4,
    read: false,
  },
  {
    id: 'notice-july',
    kind: 'notice',
    title: '7월 스터디 운영 방식이 바뀝니다',
    body: '3주차부터 풀이 발표를 돌아가면서 합니다',
    readCount: 2,
    totalCount: 4,
    read: true,
  },
  {
    id: 'assignment-react',
    kind: 'assignment',
    title: '리액트 렌더링 최적화 정리',
    body: '리액트 렌더링 최적화 방법을 정리해주세요.',
    readCount: 1,
    totalCount: 4,
    read: false,
  },
];

export const notificationFixtures: readonly ActivityNotification[] = [
  {
    id: 'notice-reminder',
    studyId: 'frontend-cs',
    activityId: 'notice-august',
    kind: 'reminder',
    title: '공지를 확인해주세요',
    description: '4주차부터 선택 미션이 추가됩니다',
    time: '3시간 전',
    read: false,
  },
  {
    id: 'notice-new-august',
    studyId: 'frontend-cs',
    activityId: 'notice-august',
    kind: 'notice',
    title: '새 공지가 올라왔어요',
    description: '4주차부터 선택 미션이 추가됩니다',
    time: '3시간 전',
    read: false,
  },
  {
    id: 'notice-new-july',
    studyId: 'frontend-cs',
    activityId: 'notice-july',
    kind: 'notice',
    title: '새 공지가 올라왔어요',
    description: '3주차부터 풀이 발표를 돌아가면서 합니다',
    time: '3시간 전',
    read: true,
  },
];
