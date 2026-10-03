import assert from 'node:assert/strict';
import test from 'node:test';

import { addFrontendNotes, renderClientOnly } from './render-client-only.mjs';
import { runInNewContext } from 'node:vm';

test('renderClientOnly replaces Redoc hydration with client rendering', () => {
  const html = '<script>var container = document.getElementById(\'redoc\'); Redoc.hydrate(__redoc_state, container);</script>';

  const rendered = renderClientOnly(html);

  assert.doesNotMatch(rendered, /Redoc\.hydrate/);
  assert.match(rendered, /container\.replaceChildren\(\)/);
  assert.match(rendered, /Redoc\.init\(__redoc_state\.spec\.data, __redoc_state\.options, container\)/);
});

test('renderClientOnly rejects an unexpected Redoc template', () => {
  assert.throws(() => renderClientOnly('<script></script>'), /found 0/);
  assert.throws(() => renderClientOnly('Redoc.hydrate(__redoc_state, container); Redoc.hydrate(__redoc_state, container);'), /found 2/);
});

test('API 상세는 수정 필요 항목에만 근거를 표시하고 기존 설명을 보존한다', () => {
  const spec = { paths: { '/api/studies': {
    parameters: [],
    get: { description: '조회', 'x-frontend': { status: 'integrated', description: '연동 확인' } },
    post: { description: '생성', 'x-frontend': { status: 'needs-update', description: '<script> 필드 누락' } },
  } } };
  addFrontendNotes(spec);
  addFrontendNotes(spec);
  assert.equal(spec.paths['/api/studies'].get.description, '조회');
  assert.equal(spec.paths['/api/studies'].post.description, '**프론트 수정 필요**\n\n&lt;script&gt; 필드 누락\n\n생성');
});

test('생성 HTML의 스크립트가 프론트 상태를 Redoc에 전달한다', () => {
  const spec = { paths: { '/api/studies': { post: {
    'x-frontend': { status: 'needs-update', description: '필드 누락' },
  } } } };
  let rendered;
  runInNewContext(renderClientOnly('Redoc.hydrate(__redoc_state, container);'), {
    container: { replaceChildren() {} },
    __redoc_state: { spec: { data: spec }, options: {} },
    Redoc: { init(value) { rendered = value; } },
  });
  assert.match(rendered.paths['/api/studies'].post.description, /프론트 수정 필요/);
});
