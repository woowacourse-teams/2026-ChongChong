import '@testing-library/jest-dom';
import { server } from './src/mocks/msw-node';
import { userTable } from './src/features/user/mocks/db';
import { studyTable } from './src/features/study/mocks/db';
import { memberTable } from './src/features/member/mocks/db';
import { assignmentTable } from './src/features/assignment/mocks/db';
import { submissionTable } from './src/features/assignment/mocks/db';
import { noticeRecipientTable, noticeTable } from './src/features/notice/mocks/db';

// TODO: 이해하기 어려운 코드에는 주석을 추가합니다
const nodeStructuredClone = globalThis.structuredClone;
globalThis.structuredClone = (value, options) => {
  const clone = nodeStructuredClone(value, options);
  const visited = new WeakSet<object>();

  function normalizeRecordPrototypes(current: unknown) {
    if (current === null || typeof current !== 'object' || visited.has(current)) return;
    const isArray = Array.isArray(current);
    if (!isArray && Object.prototype.toString.call(current) !== '[object Object]') return;

    visited.add(current);
    Object.setPrototypeOf(current, isArray ? Array.prototype : Object.prototype);
    Object.values(current).forEach(normalizeRecordPrototypes);
  }

  normalizeRecordPrototypes(clone);
  return clone;
};

beforeAll(() => server.listen());
beforeEach(() => {
  // jsdom은 window.scrollTo를 구현하지 않아서 mock을 설정합니다.
  jest.spyOn(window, 'scrollTo').mockImplementation(() => {});
});
afterEach(() => {
  server.resetHandlers();
  userTable.clear();
  studyTable.clear();
  memberTable.clear();
  assignmentTable.clear();
  submissionTable.clear();
  noticeTable.clear();
  noticeRecipientTable.clear();
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
