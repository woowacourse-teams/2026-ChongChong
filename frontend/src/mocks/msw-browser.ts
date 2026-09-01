import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';
import { createSeedStudies } from '../features/study/mocks/db';
import { createSeedMembers } from '../features/member/mocks/db';
import { createSeedUsers } from '../features/user/mocks/db';
import { createSeedAssignments, createSeedSubmissions } from '../features/assignment/mocks/db';

createSeedUsers();
createSeedStudies();
createSeedMembers();
createSeedAssignments();
createSeedSubmissions();

export const worker = setupWorker(...handlers);
