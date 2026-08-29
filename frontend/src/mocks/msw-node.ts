import { setupServer } from 'msw/node';
import { handlers } from './handlers';
import { createSeedStudies } from '../features/study/mocks/db';
import { createSeedMembers } from '../features/member/mocks/db';

createSeedStudies();
createSeedMembers();

export const server = setupServer(...handlers);
