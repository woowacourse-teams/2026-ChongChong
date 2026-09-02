import { Collection } from '@msw/data';
import { z } from 'zod';

const assignmentSchema = z.object({
  id: z.number(),
  studyId: z.number(),
  title: z.string(),
  content: z.string(),
  submissionMethod: z.string(),
  closeAt: z.string(),
  completeUserIds: z.array(z.number()),
});

export const assignmentTable = new Collection({
  schema: assignmentSchema,
});

export type AssignmentSchemaType = z.infer<typeof assignmentSchema>;

export const mockAssigments = [
  {
    id: 1,
    studyId: 2,
    title: '3점슛 10번 하기',
    content: '3점슛 10번씩 제발 해오세요.',
    submissionMethod: '링크로 제출하세요',
    closeAt: '2026-08-10T23:59:59',
    completeUserIds: [],
  },
  {
    id: 2,
    studyId: 2,
    title: '엄청 긴 제목'.repeat(100),
    content: '엄청 긴 내용'.repeat(100),
    submissionMethod: '영상 링크로 제출하세요',
    closeAt: '2026-08-20T23:59:59',
    completeUserIds: [1, 5],
  },
  {
    id: 3,
    studyId: 2,
    title: 'NBA 경기 분석하기',
    content: '이번 주 NBA 경기 하나를 골라서 전술 분석 글을 작성해주세요.',
    submissionMethod: '노션 링크로 제출하세요',
    closeAt: '2026-08-31T23:59:59',
    completeUserIds: [5],
  },
  {
    id: 4,
    studyId: 2,
    title: '자유투 성공률 측정',
    content: '자유투 50개를 던지고 성공 개수를 기록해서 제출해주세요.',
    submissionMethod: '텍스트로 제출하세요',
    closeAt: '2026-09-07T23:59:59',
    completeUserIds: [1, 5],
  },
  {
    id: 5,
    studyId: 2,
    title: '드리블 루틴 연습 일지',
    content: '2주 동안 매일 드리블 루틴을 연습하고 일지를 작성해주세요.',
    submissionMethod: '링크로 제출하세요',
    closeAt: '2026-09-30T23:59:59',
    completeUserIds: [],
  },
  {
    id: 6,
    studyId: 1,
    title: '1주차 스프링 컨테이너',
    content: 'IoC 컨테이너와 빈 생명주기를 정리하고 예제 코드를 작성해주세요.',
    submissionMethod: 'GitHub 저장소 링크로 제출하세요',
    closeAt: '2026-08-10T23:59:59',
    completeUserIds: [1, 2, 3, 4, 5],
  },
  {
    id: 7,
    studyId: 1,
    title: '2주차 의존성 주입',
    content: '생성자 주입과 필드 주입의 차이를 정리하고 리팩터링 예제를 올려주세요.',
    submissionMethod: 'GitHub PR 링크로 제출하세요',
    closeAt: '2026-08-17T23:59:59',
    completeUserIds: [2, 3, 5],
  },
  {
    id: 8,
    studyId: 1,
    title: '3주차 AOP',
    content: '로깅 기능을 AOP로 구현하고 적용 결과를 정리해주세요.',
    submissionMethod: '노션 링크로 제출하세요',
    closeAt: '2026-08-24T23:59:59',
    completeUserIds: [2, 4],
  },
  {
    id: 9,
    studyId: 1,
    title: '4주차 트랜잭션',
    content: '트랜잭션 전파 옵션을 실험하고 결과를 기록해주세요.',
    submissionMethod: 'GitHub 저장소 링크로 제출하세요',
    closeAt: '2026-08-31T23:59:59',
    completeUserIds: [3],
  },
  {
    id: 10,
    studyId: 1,
    title: '5주차 JPA 연관관계',
    content: '양방향 연관관계 매핑 시 주의할 점을 정리해주세요.',
    submissionMethod: '노션 링크로 제출하세요',
    closeAt: '2026-09-07T23:59:59',
    completeUserIds: [2, 3, 4],
  },
  {
    id: 11,
    studyId: 1,
    title: '6주차 N+1 문제',
    content: 'N+1 문제를 재현하고 fetch join으로 해결한 과정을 기록해주세요.',
    submissionMethod: 'GitHub PR 링크로 제출하세요',
    closeAt: '2026-09-14T23:59:59',
    completeUserIds: [],
  },
  {
    id: 12,
    studyId: 1,
    title: '7주차 테스트 코드',
    content: '슬라이스 테스트와 통합 테스트를 각각 작성해주세요.',
    submissionMethod: 'GitHub 저장소 링크로 제출하세요',
    closeAt: '2026-09-21T23:59:59',
    completeUserIds: [1, 2],
  },
  {
    id: 13,
    studyId: 1,
    title: '8주차 동시성',
    content: '동시성 문제를 재현하고 해결 과정을 기록해주세요.',
    submissionMethod: 'GitHub PR 링크로 제출하세요',
    closeAt: '2026-09-28T23:59:59',
    completeUserIds: [4, 5],
  },
] satisfies AssignmentSchemaType[];

export function createSeedAssignments() {
  for (const mockAssignment of mockAssigments) {
    assignmentTable.create(mockAssignment);
  }
}

const submissionSchema = z.object({
  id: z.number(),
  assignmentId: z.number(),
  userId: z.number(),
  submitted: z.boolean().default(true),
  content: z.string().nullable().default(null),
  link: z.string().nullable().default(null),
  createdAt: z.string().nullable().default(null),
});

export const submissionTable = new Collection({
  schema: submissionSchema,
});

export type SubmissionSchemaType = z.infer<typeof submissionSchema>;
type SubmissionSchemaInput = z.input<typeof submissionSchema>;

export const mockSubmissions = [
  {
    id: 1,
    assignmentId: 2,
    userId: 1,
    content: '왼손 50개, 오른손 50개 다 찍었습니다.\n오른손 마지막 10개는 좀 흔들립니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-19T09:12:05',
  },
  {
    id: 2,
    assignmentId: 2,
    userId: 5,
    content: '영상 올렸습니다. 왼손이 아직 어색해서 다음 주에 더 연습할게요.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-18T12:29:16',
  },
  {
    id: 3,
    assignmentId: 3,
    userId: 5,
    content: '레이커스 경기로 분석했습니다.\n스크린 이후 미스매치 공략 위주로 정리했어요.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-30T09:12:05',
  },
  {
    id: 4,
    assignmentId: 4,
    userId: 1,
    content: '50개 중 34개 성공했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-06T09:12:05',
  },
  {
    id: 5,
    assignmentId: 4,
    userId: 5,
    content: '50개 중 41개 성공. 후반부에 팔이 풀리니까 확률이 올라갔습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-05T12:29:16',
  },
  {
    id: 6,
    assignmentId: 6,
    userId: 1,
    content: 'IoC 컨테이너 정리하고 빈 생명주기 콜백 예제까지 작성했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-09T09:12:05',
  },
  {
    id: 7,
    assignmentId: 6,
    userId: 2,
    content:
      'BeanFactory와 ApplicationContext 차이를 중심으로 정리했습니다.\n예제는 XML 대신 자바 설정으로 작성했어요.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-08T12:29:16',
  },
  {
    id: 8,
    assignmentId: 6,
    userId: 3,
    content: '빈 스코프까지 같이 정리하다 보니 분량이 좀 길어졌습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-07T15:46:27',
  },
  {
    id: 9,
    assignmentId: 6,
    userId: 4,
    content: '@PostConstruct와 InitializingBean 실행 순서를 직접 찍어봤습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-09T18:03:38',
  },
  {
    id: 10,
    assignmentId: 6,
    userId: 5,
    content:
      '컨테이너 초기화 과정을 디버거로 따라가면서 정리했습니다.\n중간에 막힌 부분은 주석으로 남겨뒀습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-08T21:20:49',
  },
  {
    id: 11,
    assignmentId: 7,
    userId: 2,
    content: '생성자 주입으로 리팩터링한 PR입니다. 순환 참조 하나 발견해서 같이 고쳤습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-16T09:12:05',
  },
  {
    id: 12,
    assignmentId: 7,
    userId: 3,
    content:
      '필드 주입을 쓰던 서비스 3개를 생성자 주입으로 바꿨습니다.\n테스트에서 목 주입이 훨씬 편해졌습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-15T12:29:16',
  },
  {
    id: 13,
    assignmentId: 7,
    userId: 5,
    content: '@RequiredArgsConstructor 사용 여부까지 비교해봤습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-14T15:46:27',
  },
  {
    id: 14,
    assignmentId: 8,
    userId: 2,
    content: '로깅 AOP 적용했습니다. 포인트컷 표현식이 제일 헷갈렸습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-23T09:12:05',
  },
  {
    id: 15,
    assignmentId: 8,
    userId: 4,
    content:
      '실행 시간 측정 어드바이스로 구현했고, 적용 전후 로그를 첨부했습니다.\n프록시가 안 걸리는 내부 호출 케이스도 정리했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-22T12:29:16',
  },
  {
    id: 16,
    assignmentId: 9,
    userId: 3,
    content: 'REQUIRED와 REQUIRES_NEW를 각각 롤백시켜서 결과를 표로 정리했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-08-30T09:12:05',
  },
  {
    id: 17,
    assignmentId: 10,
    userId: 2,
    content: '양방향 매핑에서 연관관계 주인을 잘못 잡았을 때 생기는 문제를 재현했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-06T09:12:05',
  },
  {
    id: 18,
    assignmentId: 10,
    userId: 3,
    content:
      'cascade와 orphanRemoval 조합별 동작을 정리했습니다.\n예제 코드는 저장소 링크에 있습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-05T12:29:16',
  },
  {
    id: 19,
    assignmentId: 10,
    userId: 4,
    content: '단방향으로 충분한 경우가 많다는 걸 정리하면서 알게 됐습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-04T15:46:27',
  },
  {
    id: 20,
    assignmentId: 12,
    userId: 1,
    content: '슬라이스 테스트는 @WebMvcTest로, 통합 테스트는 @SpringBootTest로 작성했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-20T09:12:05',
  },
  {
    id: 21,
    assignmentId: 12,
    userId: 2,
    content:
      '테스트 실행 시간 차이가 커서 측정값도 같이 남겼습니다.\n통합 테스트는 컨텍스트 캐싱 여부에 따라 편차가 큽니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-19T12:29:16',
  },
  {
    id: 22,
    assignmentId: 13,
    userId: 4,
    content:
      '재고 감소 로직에서 동시성 문제를 재현하고 비관적 락으로 해결했습니다.\n낙관적 락 버전도 같이 넣어뒀습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-27T09:12:05',
  },
  {
    id: 23,
    assignmentId: 13,
    userId: 5,
    content: '스레드 100개로 테스트했고 synchronized로는 부족한 이유까지 정리했습니다.',
    link: 'http://localhost:8080',
    createdAt: '2026-09-26T12:29:16',
  },
  {
    id: 24,
    assignmentId: 7,
    userId: 1,
    submitted: false,
  },
] satisfies SubmissionSchemaInput[];

export function createSeedSubmissions() {
  for (const mockSubmission of mockSubmissions) {
    submissionTable.create(mockSubmission);
  }
}
