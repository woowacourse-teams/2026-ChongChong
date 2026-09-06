import assert from 'node:assert/strict';
import test from 'node:test';

import { renderClientOnly } from './render-client-only.mjs';

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
