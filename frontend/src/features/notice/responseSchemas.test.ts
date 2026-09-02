import { isNoticeDetailResponse } from './responseSchemas';

describe('공지 상세 응답 스키마', () => {
  test('작성자 정보가 없는 공지 상세 응답을 허용한다', () => {
    const response = {
      id: 1,
      title: '공지 제목',
      content: '공지 내용',
      createdAt: '2026-09-02T10:00:00',
    };

    expect(isNoticeDetailResponse(response)).toBe(true);
  });
});
