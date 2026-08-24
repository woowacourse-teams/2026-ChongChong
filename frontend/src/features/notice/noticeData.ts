import type { NoticeListItem } from './components/NoticeList';

export const notices: NoticeListItem[] = [
  {
    id: 1,
    title: '8월 스터디 운영 방식이 바뀝니다',
    description: '8월부터 스터디 운영 방식을 조금 바꾸려고 합니다.',
    createdAt: '5시간 전',
    isRead: false,
    readCount: 2,
    totalCount: 4,
    reminderText: '1분 뒤 리마인드',
  },
  {
    id: 2,
    title: '다음 주 스터디는 온라인으로 진행합니다',
    description: '장소 대관 일정으로 인해 다음 주는 온라인으로 만나요.',
    createdAt: '8월 1일',
    isRead: true,
    readCount: 4,
    totalCount: 4,
  },
];

export const notice = {
  title: '8월 스터디 운영 방식이 바뀝니다',
  author: '바니',
  createdAt: '5시간 전',
  content: `8월부터 스터디 운영 방식을 조금 바꾸려고 합니다. 끝까지 읽고 읽음 버튼을 눌러주세요.

1. 모임 시간
매주 화요일 저녁 9시로 고정합니다. 기존에는 요일을 매주 투표로 정했는데, 일정이 계속 밀리는 문제가 있었습니다. 8월 첫째 주부터 적용합니다.

2. 발표 순서
발표 순서는 다음과 같습니다. 매주 월요일 랜덤으로 순서를 공지합니다. 발표 자료는 모임 하루 전까지 공유해주세요.

3. 코드 리뷰
발표가 없는 주에도 서로의 코드를 한 번씩 확인합니다. 리뷰할 저장소와 범위는 스터디 채널에 남겨주세요. 리뷰는 정답을 알려주기보다 궁금한 점과 다른 선택지를 함께 적어주시면 좋겠습니다.

4. 불참 안내
참석이 어려운 경우 모임 시작 전까지 알려주세요. 미리 공유해주시면 발표 순서를 다음 주로 조정하겠습니다.

운영 방식은 한 달 동안 적용한 뒤 회고에서 다시 이야기해보겠습니다. 불편한 점이나 더 좋은 방법이 있다면 언제든 스터디 채널에 남겨주세요.

긴 글 읽어주셔서 감사합니다. 다음 모임에서 만나요!`,
  readMemberNames: ['디움', '피즈'],
  unreadMembers: [
    { id: 1, name: '안톨리니', remindedAt: '8월 3일 21:02 보냄' },
    { id: 2, name: '이든', remindedAt: '8월 3일 21:02 보냄' },
  ],
  totalCount: 4,
  reminderText: '1분 뒤 리마인드 · 8월 5일 21:00',
};
