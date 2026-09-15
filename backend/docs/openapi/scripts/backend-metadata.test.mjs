import assert from 'node:assert/strict';
import test from 'node:test';
import { collectOperations } from './backend-metadata.mjs';

function spec(metadata) {
  return { paths: { '/api/studies': {
    parameters: [],
    post: { operationId: 'createStudy', summary: '스터디 생성', 'x-backend': metadata },
  } } };
}

const unconfirmed = { status: null, owner: null, description: '' };

test('미확인과 미배정은 대기 또는 완료로 추정하지 않는다', () => {
  const [operation] = collectOperations(spec(unconfirmed));
  assert.equal(operation.status, null);
  assert.equal(operation.owner, null);
  assert.equal(operation.method, 'POST');
});

test('진행 상태와 담당자 및 여러 줄 설명을 보존한다', () => {
  const metadata = { status: 'in-progress', owner: 'frombunny', description: '신규 필드 구현\n호환성 테스트 남음' };
  const [operation] = collectOperations(spec(metadata));
  for (const field of Object.keys(metadata)) assert.equal(operation[field], metadata[field]);
});

test('누락, 오타, 잘못된 타입과 담당자 없는 진행 상태를 거부한다', () => {
  for (const metadata of [
    undefined, [], {},
    { ...unconfirmed, status: 'finished' },
    { ...unconfirmed, status: ['done'] },
    { ...unconfirmed, owner: '' },
    { ...unconfirmed, owner: '@frombunny' },
    { ...unconfirmed, description: null },
    { ...unconfirmed, stauts: 'todo' },
    ...['in-progress', 'review', 'done'].map((status) => ({ ...unconfirmed, status })),
  ]) {
    assert.throws(() => collectOperations(spec(metadata)), /POST \/api\/studies:/);
  }
});

test('중복 operationId와 미해결 path 참조를 거부한다', () => {
  const duplicate = spec(unconfirmed);
  duplicate.paths['/api/studies'].get = duplicate.paths['/api/studies'].post;
  assert.throws(() => collectOperations(duplicate), /고유한 operationId/);
  assert.throws(() => collectOperations({ paths: { '/api/studies': { $ref: './study.yaml' } } }), /번들 명세/);
});
