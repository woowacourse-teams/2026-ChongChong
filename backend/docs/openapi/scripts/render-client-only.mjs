import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const hydration = 'Redoc.hydrate(__redoc_state, container);';

export function addFrontendNotes(spec) {
  for (const item of Object.values(spec.paths ?? {})) {
    for (const operation of Object.values(item)) {
      if (operation?.['x-frontend']?.status !== 'needs-update') continue;
      const note = operation['x-frontend'].description.replace(/[&<>]/g,
        (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[character]);
      const heading = '**프론트 수정 필요**';
      if (!operation.description?.startsWith(heading)) {
        operation.description = `${heading}\n\n${note}\n\n${operation.description ?? ''}`;
      }
    }
  }
}

const clientRendering = [
  'container.replaceChildren();',
  `(${addFrontendNotes.toString()})(__redoc_state.spec.data);`,
  'Redoc.init(__redoc_state.spec.data, __redoc_state.options, container);',
].join('\n      ');

export function renderClientOnly(html) {
  const occurrences = html.split(hydration).length - 1;

  if (occurrences !== 1) {
    throw new Error(`Expected one Redoc hydration call, found ${occurrences}.`);
  }

  return html.replace(hydration, clientRendering);
}

async function main(outputPath) {
  const html = await readFile(outputPath, 'utf8');
  await writeFile(outputPath, renderClientOnly(html));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const outputPath = process.argv[2];

  if (!outputPath) {
    throw new Error('Provide the generated HTML file path.');
  }

  await main(outputPath);
}
