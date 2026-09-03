export const FEATURE_CAROUSEL_STYLES = `
  .cc-feature-showcase {
    --phone-scale: .86;
    position: relative;
    display: flex;
    min-width: 0;
    width: 100%;
    padding: 28px 0 24px;
    flex-direction: column;
    align-items: center;
    text-align: center;
    animation: cc-enter-right var(--motion-entry) 80ms both;
  }
  .cc-feature-phone-stage {
    position: relative;
    isolation: isolate;
    padding: 0 0 24px;
  }
  .cc-feature-phone-stage::before {
    position: absolute;
    z-index: -1;
    inset: 10% -26% -2%;
    background: radial-gradient(ellipse, #e4f7ec 0%, #effaf4 45%, transparent 72%);
    content: '';
    pointer-events: none;
  }
  .cc-feature-phone-size {
    position: relative;
    width: calc(316px * var(--phone-scale));
    height: calc(636px * var(--phone-scale));
  }
  .cc-feature-phone {
    position: absolute;
    width: 316px;
    height: 636px;
    padding: 7px;
    border: 1px solid #090b0a;
    border-radius: 44px;
    background: #111412;
    box-shadow: 0 22px 48px #15382420, 0 4px 12px #15382418, inset 0 0 0 1px #383d39;
    transform: scale(var(--phone-scale));
    transform-origin: top left;
    user-select: none;
    -webkit-user-select: none;
    -webkit-tap-highlight-color: transparent;
  }
  .cc-feature-phone-screen {
    height: 620px;
    overflow: hidden;
    border-radius: 36px;
    background: #fff;
    box-shadow: 0 0 0 1px #11231912;
    pointer-events: none;
  }
  .cc-feature-status {
    position: relative;
    display: flex;
    height: 44px;
    padding: 0 20px;
    align-items: center;
    justify-content: space-between;
    color: #18221d;
    font-size: 12px;
    font-weight: 650;
    letter-spacing: 0;
  }
  .cc-feature-status svg { width: 48px; height: 16px; }
  .cc-feature-island {
    position: absolute;
    top: 14px;
    left: 50%;
    width: 76px;
    height: 16px;
    border-radius: 20px;
    background: #151917;
    transform: translateX(-50%);
  }
  .cc-feature-viewport { width: 300px; height: 558px; overflow: hidden; }
  .cc-feature-track { display: flex; width: 100%; height: 100%; will-change: transform; }
  .cc-feature-slide { flex: 0 0 100%; width: 300px; height: 558px; overflow: hidden; text-align: left; }
  .cc-feature-home-indicator { display: grid; height: 18px; place-items: center; background: #fff; }
  .cc-feature-home-indicator span { width: 96px; height: 4px; border-radius: 8px; background: #18221d; }
  .cc-feature-dots { display: flex; gap: 4px; align-items: center; }
  .cc-feature-dot { display: grid; width: 28px; height: 36px; place-items: center; border-radius: 50%; cursor: pointer; -webkit-tap-highlight-color: transparent; }
  .cc-feature-dot:hover { background: #eff7f2; }
  .cc-feature-dot:disabled { cursor: default; }
  .cc-feature-dot:focus-visible { outline: 3px solid var(--brand-deep); outline-offset: 3px; }
  .cc-feature-dot span { width: 5px; height: 5px; border-radius: 8px; background: #cbd7cf; transition: width 200ms, background 200ms; }
  .cc-feature-dot[aria-current='step'] span { width: 20px; background: var(--brand); }
  @media (min-width: 721px) and (max-height: 820px) {
    .cc-feature-showcase { --phone-scale: .74; padding-top: 16px; padding-bottom: 16px; }
    .cc-feature-phone-stage { padding-bottom: 20px; }
  }
  @media (max-width: 720px) {
    .cc-feature-showcase { --phone-scale: .82; padding: 20px 0 32px; }
    .cc-feature-phone-stage { padding-bottom: 28px; }
    .cc-feature-dot { width: 28px; height: 44px; }
  }
  @media (max-width: 360px) {
    .cc-feature-showcase { --phone-scale: .76; }
    .cc-feature-dot { width: 24px; }
  }
  @media (prefers-reduced-motion: reduce) {
    .cc-feature-showcase { animation: none; }
    .cc-feature-dot span { transition: none; }
    .cc-feature-track { transition: none !important; }
  }
`;
