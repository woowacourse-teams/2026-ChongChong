import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';
import { createSeedStudies } from '../features/study/mocks/db';
import { createSeedMembers } from '../features/member/mocks/db';
import { createSeedUsers } from '../features/user/mocks/db';

createSeedUsers();
createSeedStudies();
createSeedMembers();

export const worker = setupWorker(...handlers);
