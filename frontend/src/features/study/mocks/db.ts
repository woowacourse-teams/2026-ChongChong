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
    inviteLink: 'chongchong.app/join/1',
  },
  {
    id: 2,
    name: '농구 스터디',
    description: '2m 이든과 함께하는 농구 스터디',
    inviteLink: 'chongchong.app/join/2',
  },
  {
    id: 3,
    name: '키 입력 스터디',
    description: '맥북 엔터키를 부시려고 하는 디움의 스터디',
    inviteLink: 'chongchong.app/join/3',
  },
  {
    id: 4,
    name: '피즈 강의 스터디',
    description: '브론즈지만 피즈 장인 피즈의 피즈 잘하는 법 스터디',
    inviteLink: 'chongchong.app/join/4',
  },
] satisfies studySchemaType[];

export function createSeedStudies() {
  for (const mockStudy of mockStudies) {
    studyTable.create(mockStudy);
  }
}
