import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';
import { createSeedStudies } from '../features/study/mocks/db';
import { createSeedMembers } from '../features/member/mocks/db';

createSeedStudies();
createSeedMembers();

export const worker = setupWorker(...handlers);
