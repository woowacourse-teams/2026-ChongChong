/**
 * jest는 모듈을 CommonJS로 변환해 실행하기 때문에 `import.meta`가 남아 있으면
 * SyntaxError가 발생한다. react-router처럼 ESM 전용으로 배포되는 의존성을 위해
 * 테스트 환경에서만 `import.meta`를 빈 객체로 치환한다.
 */
function importMetaStub({ types: t }) {
  return {
    name: 'test-import-meta-stub',
    visitor: {
      MetaProperty(path) {
        const { meta, property } = path.node;
        if (meta.name === 'import' && property.name === 'meta') {
          path.replaceWith(t.objectExpression([]));
        }
      },
    },
  };
}

export default {
  presets: [
    ['@babel/preset-env', { targets: 'defaults, not dead' }],
    ['@babel/preset-react', { runtime: 'automatic', importSource: '@emotion/react' }],
    '@babel/preset-typescript',
  ],
  env: {
    test: {
      plugins: [importMetaStub],
    },
  },
};
