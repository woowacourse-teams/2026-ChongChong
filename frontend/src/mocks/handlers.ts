import { handlers as studiesHandlers } from '../features/studies/mocks/handlers';
import { handlers as assignmentsHandlers } from '../features/assignment/mocks/handlers';

export const handlers = [...studiesHandlers, ...assignmentsHandlers];
