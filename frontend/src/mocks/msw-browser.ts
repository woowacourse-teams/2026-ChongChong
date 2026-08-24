import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';
import { createSeedStudies } from '../features/studies/mocks/db';

createSeedStudies();

export const worker = setupWorker(...handlers);
