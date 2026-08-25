import '@testing-library/jest-dom';
import { server } from './src/mocks/msw-node';
import { studyTable, createSeedStudies } from './src/features/studies/mocks/db';
import { memberTable, createSeedMembers } from './src/features/member/mocks/db';

beforeAll(() => server.listen());
// 매 테스트마다 초기 시드 상태로 되돌립니다.
afterEach(() => {
  server.resetHandlers();
  studyTable.clear();
  memberTable.clear();
  createSeedStudies();
  createSeedMembers();
});
afterAll(() => server.close());

// jsdom은 <dialog>의 showModal/close를 구현하지 않아 테스트용으로 채워줍니다.
beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.open = true;
  };
  HTMLDialogElement.prototype.show = function show() {
    this.open = true;
  };
  HTMLDialogElement.prototype.close = function close() {
    this.open = false;
    this.dispatchEvent(new Event('close'));
  };
});
