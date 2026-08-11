import '@testing-library/jest-dom';
import { server } from './src/mocks/msw-node';

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
