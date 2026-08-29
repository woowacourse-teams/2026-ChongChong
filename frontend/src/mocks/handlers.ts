import { handlers as studiesHandlers } from '../features/study/mocks/handlers';
import { handlers as assignmentsHandlers } from '../features/assignment/mocks/handlers';
import { handlers as memberHandlers } from '../features/member/mocks/handlers';

export const handlers = [...studiesHandlers, ...assignmentsHandlers, ...memberHandlers];
