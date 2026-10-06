const e2eMode = process.env.E2E_MODE === 'true';

export async function initializeMocking() {
  // .env의 USE_MSW가 true일 때 MSW를 사용합니다.
  if (process.env.USE_MSW !== 'true') {
    return;
  }
  const { worker } = await import('./msw-browser');

  // E2E에서는 기본 개발 시드를 생성하지 않습니다.
  // 각 테스트에서 필요한 목 데이터를 따로 정의하고 준비합니다.
  if (e2eMode) {
    const { initializeE2EScenario } = await import('./e2eScenario');
    await initializeE2EScenario();
  } else {
    const { seedDevelopmentData } = await import('./seed');
    seedDevelopmentData();
  }

  return worker.start();
}
