import { parseParentPath } from '../parseParentPath';

describe('parseParentPath 테스트', () => {
  test('부모 경로를 반환한다.', () => {
    const parentPath = parseParentPath('/studies/1/notices/1');

    expect(parentPath).toBe('/studies/1/notices');
  });

  test('/로 끝나는 경우 /를 제거한 뒤 부모 경로를 반환한다.', () => {
    const parentPath = parseParentPath('/studies/1/notices/1/');

    expect(parentPath).toBe('/studies/1/notices');
  });

  test('제출 상세 페이지 경로가 들어오면 과제 상세 페이지 경로를 반환한다.', () => {
    const parentPath = parseParentPath('/studies/1/assignments/1/submissions/1');

    expect(parentPath).toBe('/studies/1/assignments/1');
  });
});
