import { handlers as studiesHandlers } from '../features/study/mocks/handlers';
import { handlers as assignmentsHandlers } from '../features/assignment/mocks/handlers';
import { handlers as memberHandlers } from '../features/member/mocks/handlers';
import { handlers as loginHandlers } from '../features/login/mocks/handlers';
import { handlers as mypageHandlers } from '../features/mypage/mocks/handlers';

export const handlers = [
  ...studiesHandlers,
  ...assignmentsHandlers,
  ...memberHandlers,
  ...loginHandlers,
  ...mypageHandlers,
];
