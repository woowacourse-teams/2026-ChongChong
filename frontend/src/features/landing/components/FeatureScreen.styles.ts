export const FEATURE_SCREEN_STYLES = `
  .cc-demo-screen {
    position: relative;
    width: 300px;
    height: 558px;
    flex: 0 0 300px;
    overflow: hidden;
    background: var(--bg-default);
    user-select: none;
    pointer-events: none;
  }
  .cc-demo-app-canvas {
    --safe-top: 0px;
    --safe-bottom: 0px;
    --font-family-base: "Chongchong Pretendard", "Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
    position: relative;
    display: flex;
    flex-direction: column;
    width: 400px;
    height: 744px;
    overflow: hidden;
    transform: scale(.75);
    transform-origin: top left;
    background: var(--bg-default);
    color: var(--text-default);
    font-family: var(--font-family-base);
    font-size: 16px;
    font-weight: 400;
    line-height: 1.15;
    letter-spacing: normal;
    text-align: left;
  }
  .cc-demo-app-canvas *, .cc-demo-app-canvas *::before, .cc-demo-app-canvas *::after { box-sizing: border-box; }
  .cc-demo-app-canvas > main { min-height: 0; overflow: hidden; }
  .cc-demo-app-canvas > main > * { flex-shrink: 0; }
  .cc-demo-back { display: grid; width: 32px; height: 32px; place-items: center; }
  .cc-demo-study-art { display: flex; align-items: center; justify-content: center; margin: 56px 0; }
  .cc-demo-tabs {
    position: relative;
    display: flex;
    flex: 0 0 64px;
    height: 64px;
    align-items: center;
    justify-content: space-between;
    padding: var(--spacing-2) var(--spacing-10);
    border-top: 1px solid var(--border-default);
    border-top-left-radius: var(--radius-lg);
    border-top-right-radius: var(--radius-lg);
    background: var(--bg-default);
  }
  .cc-demo-tabs > div { display: flex; flex-direction: column; align-items: center; gap: 4px; }
  .cc-demo-tabs img { display: block; flex-shrink: 0; width: 22px; height: 22px; }
  .cc-demo-tabs p { margin: 0; color: var(--text-placeholder); font-size: var(--font-size-12); font-weight: 400; line-height: var(--line-height-18); letter-spacing: var(--letter-spacing-default); text-align: center; }
  .cc-demo-tabs .cc-demo-tab-active { color: var(--text-brand); }
  .cc-demo-study-content { display: flex; flex: 1; flex-direction: column; }
  .cc-demo-study-sections { display: flex; flex: 1; flex-direction: column; gap: var(--spacing-6); min-height: 0; }
  .cc-demo-study-sections > section { display: flex; flex: 1; flex-direction: column; min-height: 0; }
  .cc-demo-incoming-banner {
    position: absolute;
    z-index: 110;
    top: 12px;
    right: 13px;
    left: 13px;
    padding: 20px 16px 9px;
    border: 1px solid rgba(255, 255, 255, .98);
    border-radius: 28px;
    background: rgba(247, 249, 247, .95);
    box-shadow: 0 16px 37px #143a2729, 0 1px 5px #143a271a;
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);
    opacity: 1;
    transform: translateY(0);
  }
  .cc-demo-incoming-banner--animated { animation: cc-demo-notification-arrive 1.2s cubic-bezier(.2, .8, .2, 1) both; }
  .cc-demo-incoming-banner-content { display: flex; align-items: flex-start; gap: 12px; }
  .cc-demo-notification-app { display: grid; place-items: center; flex-shrink: 0; width: 48px; height: 48px; border: 1px solid #d7efdf; border-radius: 13px; background: #e3f7ea; }
  .cc-demo-notification-content { flex: 1; min-width: 0; }
  .cc-demo-notification-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; color: #35443a; font-size: 13px; line-height: 20px; }
  .cc-demo-notification-meta > span { color: #8b968e; font-size: 12px; }
  .cc-demo-notification-title { margin: 0 0 7px; color: #24392c; font-size: 15px; font-weight: 750; line-height: 21px; letter-spacing: -.045em; }
  .cc-demo-notification-assignment { margin: 0; color: #4b5e52; font-size: 13px; font-weight: 600; line-height: 21px; }
  .cc-demo-notification-description { margin: 0; color: #77877c; font-size: 13px; line-height: 20px; letter-spacing: -.025em; }
  .cc-demo-notification-handle { width: 36px; height: 4px; margin: 13px auto 0; border-radius: 7px; background: #c5cec8; }
  @keyframes cc-demo-notification-arrive {
    0%, 46% { opacity: 0; transform: translateY(-140%); }
    86% { opacity: 1; transform: translateY(4px); }
    100% { opacity: 1; transform: translateY(0); }
  }
  @media (prefers-reduced-motion: reduce) {
    .cc-demo-incoming-banner--animated { animation: none; }
  }
`;
