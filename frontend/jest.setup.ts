import '@testing-library/jest-dom';
import { server } from './src/mocks/msw-node';
import { studyTable, createSeedStudies } from './src/features/study/mocks/db';
import { memberTable, createSeedMembers } from './src/features/member/mocks/db';
import {
  assignmentTable,
  createSeedAssignments,
  submissionTable,
  createSeedSubmissions,
} from './src/features/assignment/mocks/db';
import { userTable, createSeedUsers } from './src/features/user/mocks/db';

beforeAll(() => server.listen());
// 매 테스트마다 초기 시드 상태로 되돌립니다.
afterEach(() => {
  server.resetHandlers();
  userTable.clear();
  studyTable.clear();
  memberTable.clear();
  assignmentTable.clear();
  submissionTable.clear();
  createSeedUsers();
  createSeedStudies();
  createSeedMembers();
  createSeedAssignments();
  createSeedSubmissions();
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
