import { readFile } from 'node:fs/promises';
import { collectOperations } from './backend-metadata.mjs';

const input = process.argv[2];
if (!input) throw new Error('번들 JSON 파일 경로를 입력하세요.');
const operations = collectOperations(JSON.parse(await readFile(input, 'utf8')));
console.log(`백엔드 메타데이터 검증 완료: ${operations.length}개 API`);
