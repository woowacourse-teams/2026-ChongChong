export const previewFeatures = [
  {
    id: 'study',
    label: '스터디 생성',
    title: '스터디를\n간편하게 만들어요',
    description: '이름과 소개를 입력하고,\n스터디를 바로 시작해요.',
  },
  {
    id: 'invite',
    label: '스터디 초대',
    title: '링크 하나로,\n간편하게 초대해요',
    description: '초대 링크를 복사해 공유해 보세요.\n스터디원을 모으는 일도 간편해져요.',
  },
  {
    id: 'assignment',
    label: '과제 생성',
    title: '과제와 마감,\n한눈에 확인해요',
    description: '과제 내용부터 제출 방법, 마감일까지.\n같은 목표를 향해 차근차근 나아가요.',
  },
  {
    id: 'notice',
    label: '공지 생성',
    title: '중요한 소식,\n공지로 전달해요',
    description: '모임 일정과 안내를 공지로 남기고,\n누가 확인했는지도 한곳에서 살펴봐요.',
  },
  {
    id: 'reminder',
    label: '리마인드 알림',
    title: '깜빡하기 전에,\n한 번 더 확인해요',
    description: '놓치기 쉬운 과제와 중요한 공지.\n리마인드 알림으로 다시 챙겨요.',
  },
  {
    id: 'submission',
    label: '제출 현황',
    title: '함께 쌓은 진도,\n한눈에 확인해요',
    description: '제출한 사람과 아직 준비 중인 사람까지.\n과제 진행 상황을 한눈에 확인해요.',
  },
] as const;

export type PreviewFeatureId = (typeof previewFeatures)[number]['id'];
