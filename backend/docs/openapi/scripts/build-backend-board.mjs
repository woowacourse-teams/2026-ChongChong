import { copyFile, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { collectOperations, statuses } from './backend-metadata.mjs';

export function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[character]);
}

export function renderBoard(spec) {
  const operations = collectOperations(spec);
  const rows = operations.map((operation) => {
    const { id, method, path, summary, status, owner, description } = operation;
    const href = `index.html#operation/${encodeURIComponent(id)}`;
    const notes = description.trim() ? description.split(/\n(?=후속 예정:)/).map((line) =>
      `<p class="note${line.startsWith('후속 예정:') ? ' followup' : ''}">${escapeHtml(line)}</p>`
    ).join('\n') : '<p class="muted">등록된 설명 없음</p>';
    return `<tr role="row">
      <td role="cell" class="method-cell"><span class="method" data-method="${escapeHtml(method)}">${escapeHtml(method)}</span></td>
      <td role="cell" class="name-cell"><a class="api-title" href="${escapeHtml(href)}">${escapeHtml(summary)}</a></td>
      <td role="cell" class="notes"><span class="mobile-label" aria-hidden="true">개발 설명</span><div class="note-body">${notes}</div></td>
      <td role="cell" class="uri-cell"><code>${escapeHtml(path)}</code></td>
      <td role="cell"><span class="mobile-label" aria-hidden="true">상태</span><span class="badge" data-state="${escapeHtml(status ?? 'unconfirmed')}">${status === null ? '미확인' : statuses[status]}</span></td>
      <td role="cell"><span class="mobile-label" aria-hidden="true">담당자</span><div class="owner"><span class="avatar" aria-hidden="true">${escapeHtml(owner ? owner.slice(0, 1).toUpperCase() : '—')}</span>${escapeHtml(owner ?? '미배정')}</div></td>
    </tr>`;
  }).join('\n');
  return `<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>백엔드 개발 보드 | 총총 API</title><meta name="description" content="총총 API의 백엔드 개발 상태와 담당자, 개발 설명을 확인합니다.">
<link rel="stylesheet" href="board.css"></head>
<body><main><nav class="topbar" aria-label="문서"><div class="brand">총총<span>/</span><span>개발 문서</span></div><a class="doc-link" href="index.html">API 문서로 이동<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M7 17 17 7M7 7h10v10"/></svg></a></nav>
<header class="page-heading"><h1>백엔드 개발 보드</h1></header>
<div class="results-bar"><p>전체 ${operations.length}개 API</p>
<details class="status-guide"><summary>상태 기준 안내</summary><p>현재 명세의 구현 상태입니다. 완료는 배포나 후속 작업의 완료를 뜻하지 않습니다. 미확인은 상태 확인 전, 미배정은 담당자 지정 전입니다.</p></details></div>
<div class="table-scroll" role="region" aria-label="API 목록. 좌우로 스크롤할 수 있습니다." tabindex="0">
<table role="table"><caption>백엔드 API별 작업 현황</caption>
<thead role="rowgroup"><tr role="row"><th role="columnheader" scope="col">메서드</th><th role="columnheader" scope="col">이름</th><th role="columnheader" scope="col">개발 설명</th><th role="columnheader" scope="col">URI</th><th role="columnheader" scope="col">상태</th><th role="columnheader" scope="col">담당자</th></tr></thead><tbody role="rowgroup">${rows}</tbody></table></div>
</main></body></html>\n`;
}

async function main(inputPath, outputPath) {
  const spec = JSON.parse(await readFile(inputPath, 'utf8'));
  await writeFile(outputPath, renderBoard(spec));
  for (const asset of ['board.css']) {
    await copyFile(new URL(`../board/${asset}`, import.meta.url), join(dirname(outputPath), asset));
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const [, , input, output] = process.argv;
  if (!input || !output) throw new Error('번들 JSON 파일과 출력 HTML 경로를 입력하세요.');
  await main(input, output);
}
