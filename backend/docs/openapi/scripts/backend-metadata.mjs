export const statuses = {
  todo: '대기',
  'in-progress': '진행 중',
  review: '리뷰 중',
  done: '완료',
};

const methods = new Set(['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace']);
const fields = new Set(['status', 'owner', 'description']);

export function collectOperations(spec) {
  const operations = [];
  const identifiers = new Set();
  for (const [path, item] of Object.entries(spec.paths ?? {})) {
    if (item.$ref) throw new Error(`${path}: 번들 명세를 입력하세요.`);
    for (const [method, operation] of Object.entries(item)) {
      if (!methods.has(method)) continue;
      const location = `${method.toUpperCase()} ${path}`;
      const fail = (message) => { throw new Error(`${location}: ${message}`); };
      const id = operation.operationId;
      if (typeof id !== 'string' || !id.trim() || identifiers.has(id)) {
        fail('고유한 operationId가 필요합니다.');
      }
      identifiers.add(id);
      const metadata = operation['x-backend'];
      if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) {
        fail('x-backend 객체가 필요합니다.');
      }
      if (Object.keys(metadata).some((key) => !fields.has(key))) {
        fail('x-backend에는 status, owner, description만 작성하세요.');
      }
      const { status, owner, description } = metadata;
      if (status !== null && (typeof status !== 'string' || !Object.hasOwn(statuses, status))) {
        fail('status는 null, todo, in-progress, review, done 중 하나여야 합니다.');
      }
      if (owner !== null && (typeof owner !== 'string' || !/^[a-z\d](?:[a-z\d-]{0,37}[a-z\d])?$/i.test(owner))) {
        fail('owner는 GitHub 아이디 또는 null이어야 합니다.');
      }
      if (status !== null && status !== 'todo' && owner === null) {
        fail('작업을 시작한 API에는 owner가 필요합니다.');
      }
      if (typeof description !== 'string') fail('description은 문자열이어야 합니다.');
      operations.push({
        id, method: method.toUpperCase(), path,
        summary: operation.summary ?? id,
        tags: operation.tags ?? [],
        status, owner, description,
      });
    }
  }
  if (operations.length === 0) throw new Error('명세에 API operation이 없습니다.');
  return operations;
}
