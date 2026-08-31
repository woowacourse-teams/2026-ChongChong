import { Collection } from '@msw/data';
import { z } from 'zod';

const studySchema = z.object({
  id: z.number(),
  name: z.string(),
  description: z.string(),
  inviteLink: z.string(),
});

export const studyTable = new Collection({
  schema: studySchema,
});

type studySchemaType = z.infer<typeof studySchema>;

export const mockStudies = [
  {
    id: 1,
    name: '스프링 스터디',
    description: '스프링 초고수 바니와 함께하는 스터디',
    inviteLink: 'https://chongchong.app/join?token=1',
  },
  {
    id: 2,
    name: '농구 스터디',
    description: '2m 이든과 함께하는 농구 스터디',
    inviteLink: 'https://chongchong.app/join?token=2',
  },
  {
    id: 3,
    name: '키 입력 스터디',
    description: '맥북 엔터키를 부시려고 하는 디움의 스터디',
    inviteLink: 'https://chongchong.app/join?token=3',
  },
  {
    id: 4,
    name: '피즈 강의 스터디',
    description: '브론즈지만 피즈 장인 피즈의 피즈 잘하는 법 스터디',
    inviteLink: 'https://chongchong.app/join?token=4',
  },
  {
    id: 5,
    name: '내가 리더인 유령 스터디',
    description: '아무런 활동도 없는 유령 스터디',
    inviteLink: 'chongchong.ghost.app/join/5',
  },
  {
    id: 6,
    name: '내가 멤버인 유령 스터디',
    description: '아무런 활동도 없는 유령 스터디',
    inviteLink: 'chongchong.ghost.app/join/6',
  },
] satisfies studySchemaType[];

export function createSeedStudies() {
  for (const mockStudy of mockStudies) {
    studyTable.create(mockStudy);
  }
}
