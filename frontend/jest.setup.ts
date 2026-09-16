import '@testing-library/jest-dom';
import { server } from './src/mocks/msw-node';
import { userTable } from './src/features/user/mocks/db';
import { studyTable } from './src/features/study/mocks/db';
import { memberTable } from './src/features/member/mocks/db';
import { assignmentTable } from './src/features/assignment/mocks/db';
import { submissionTable } from './src/features/assignment/mocks/db';

beforeAll(() => server.listen());
afterEach(() => {
  userTable.clear();
  studyTable.clear();
  memberTable.clear();
  assignmentTable.clear();
  submissionTable.clear();
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
