import type { ImageSourcePropType } from 'react-native';

export type NoticeImage = {
  readonly id: string;
  readonly name: string;
  readonly source: ImageSourcePropType;
};
export type NoticeRecipient = {
  readonly id: string;
  readonly name: string;
  readonly readAt: string | null;
  readonly remindedAt: string | null;
  readonly reminderAvailable?: boolean;
};
export type Notice = {
  readonly id: string;
  readonly title: string;
  readonly body: string;
  readonly createdLabel: string;
  readonly timeLabel: string;
  readonly images: readonly NoticeImage[];
  readonly reminders: readonly string[];
  readonly recipients: readonly NoticeRecipient[];
};
export type NoticeDraft = Pick<
  Notice,
  'title' | 'body' | 'images' | 'reminders'
>;
export const noticeBody =
  '8월부터 스터디 운영 방식을 조금 바꾸려고 합니다. 끝까지 읽고 읽음 버튼을 눌러주세요.\n\n1. 모임 시간\n매주 화요일 저녁 9시로 고정합니다. 기존에는 요일을 매주 투표로 정했는데, 일정이 계속 밀리는 문제가 있었습니다. 8월 첫째 주부터 적용합니다람쥐가 노래를한다\n\n2. 발표 순서\n발표 순서는 다음과 같습니다. 매주 월요일 랜덤으로 순서를 공지합니다 이런 느낌으로 스크롤을 쭈욱하게 해주면됩니다8월부터 스터디 운영 방식을 조금 바꾸려고 합니다. 끝까지 읽고 읽음 버튼을 눌러주세요.\n\n1. 모임 시간\n매주 화요일 저녁 9시로 고정합니다. 기존에는 요일을 매주 투표로 정했는데, 일정이 계속 밀리는 문제가 있었습니다. 8월 첫째 주부터 적용합니다람쥐가 노래를한다\n\n2. 발표 순서\n발표 순서는 다음과 같습니다.\n\n여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가 이제 마지막 줄입여기가';
export const sampleImage: NoticeImage = {
  id: 'figma-dog',
  name: 'image.png',
  source: require('../../../assets/images/notices/original.png'),
};
export const recipients: readonly NoticeRecipient[] = [
  {
    id: 'reader-1',
    name: '안몰리니',
    readAt: '8월 3일 21:02',
    remindedAt: null,
  },
  {
    id: 'reader-2',
    name: '안몰리니',
    readAt: '8월 3일 21:02',
    remindedAt: null,
  },
  { id: 'self', name: '피즈', readAt: null, remindedAt: '8월 3일 21:02' },
  {
    id: 'reader-4',
    name: '피즈',
    readAt: null,
    remindedAt: '8월 3일 21:02',
    reminderAvailable: true,
  },
];
export function createNoticeFixtures(member: boolean): readonly Notice[] {
  return ['august', 'july', 'june'].map((month, index) => ({
    id: `notice-${month}`,
    title: `${member ? 8 : 8 - index}월 스터디 운영 방식이 바뀝니다`,
    body: noticeBody,
    createdLabel: '11월 21일 23:59 작성',
    timeLabel:
      index === 0
        ? '5시간 전'
        : member
          ? index === 1
            ? '5시간 전'
            : '12시간 전'
          : `${index}달 전`,
    images:
      index === 0
        ? [sampleImage]
        : index === 2 && member
          ? [sampleImage, { ...sampleImage, id: 'figma-dog-2' }]
          : [],
    reminders: index === 0 ? ['2026-08-05T18:00'] : [],
    recipients: recipients.flatMap((recipient) => {
      if (member && index === 2 && recipient.id === 'self') return [];
      const readAt = member
        ? index === 0 && recipient.id === 'self'
          ? '8월 3일 21:14'
          : recipient.readAt
        : index > 0
          ? '8월 3일 21:02'
          : recipient.readAt;
      return [{ ...recipient, readAt }];
    }),
  }));
}
