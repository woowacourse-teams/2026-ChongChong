import assert from 'node:assert/strict';
import test from 'node:test';
import { renderBoard } from './build-backend-board.mjs';

function spec(metadata, summary = '스터디 생성') {
  return { paths: { '/api/studies': { post: {
    operationId: 'createStudy', summary, tags: ['Study'], 'x-backend': metadata,
  } } } };
}

test('보드에는 미확인 API와 상세 문서 링크가 표시된다', () => {
  const html = renderBoard(spec({ status: null, owner: null, description: '' }));
  assert.match(html, /index.html#operation\/createStudy/);
  assert.match(html, /미확인/);
  assert.match(html, /미배정/);
  assert.match(html, /등록된 설명 없음/);
  assert.match(html, /전체 1개 API/);
});

test('설명과 API 이름은 HTML로 실행되지 않고 줄바꿈을 보존한다', () => {
  const html = renderBoard(spec({ status: 'review', owner: 'frombunny', description: '<script>alert(1)</script>\n호환성 확인' }, '<img src=x onerror=alert(1)>'));
  assert.doesNotMatch(html, /<script>alert|<img src=x/);
  assert.match(html, /&lt;script&gt;alert\(1\)&lt;\/script&gt;\n호환성 확인/);
  assert.match(html, /frombunny/);
  assert.match(html, /리뷰 중/);
});

test('보드 빌드에서도 잘못된 메타데이터를 거부한다', () => {
  assert.throws(() => renderBoard(spec({ status: 'done', owner: null, description: '' })), /owner/);
});
