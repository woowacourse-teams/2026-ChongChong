import type { MockStudy } from './db';

// 기존 공개 스터디 2개와 추가 23개로 10 → 10 → 5개 페이징을 확인합니다.
export const mockStudies = [
  {
    id: 1,
    name: '스프링 스터디',
    description: '스프링 초고수 바니와 함께하는 스터디',
    inviteLink: 'spring',
  },
  {
    id: 2,
    isPublic: true,
    name: '농구 스터디',
    description: '2m 이든과 함께하는 농구 스터디',
    inviteLink: 'basketball',
  },
  {
    id: 3,
    name: '키 입력 스터디',
    description: '맥북 엔터키를 부시려고 하는 디움의 스터디',
    inviteLink: 'enter',
  },
  {
    id: 4,
    isPublic: true,
    name: '피즈 강의 스터디',
    description: '브론즈지만 피즈 장인 피즈의 피즈 잘하는 법 스터디',
    inviteLink: 'bronze',
  },
  {
    id: 5,
    name: '내가 리더인 유령 스터디',
    description: '아무런 활동도 없는 유령 스터디',
    inviteLink: 'leader-ghost',
  },
  {
    id: 6,
    name: '내가 멤버인 유령 스터디',
    description: '아무런 활동도 없는 유령 스터디',
    inviteLink: 'member-ghost',
  },
  {
    id: 7,
    name: '내가 멤버이면서 읽어야할 공지가 하나만 있는 스터디',
    description: '공지가 하나',
    inviteLink: 'only-one-notice',
  },
  {
    id: 8,
    name: '내가 멤버이면서 제출해야할 과제가 하나만 있는 스터디',
    description: '과제가 하나',
    inviteLink: 'only-one-assignment',
  },
  {
    id: 9,
    name: '과제 공개 범위 확인 스터디',
    description: '스터디원 화면에서 과제 공개 범위를 확인하는 스터디',
    inviteLink: 'assignment-visibility-preview',
  },
  {
    id: 10,
    name: '공지와 과제가 많은 스터디',
    description: '공지와 과제가 각각 30개 있는 무한 스크롤 확인용 스터디',
    inviteLink: 'infinite-scroll',
  },
  ...Array.from({ length: 23 }, (_, index) => ({
    id: index + 1000,
    isPublic: true,
    name: `공개 스터디 ${String(index + 1).padStart(2, '0')}`,
    description: `함께 공부하는 공개 스터디 ${index + 1}입니다.`,
    inviteLink: `public-study-${index + 1}`,
  })),
] satisfies MockStudy[];
