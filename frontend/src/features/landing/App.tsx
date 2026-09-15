import { useEffect, useRef, useState, type MouseEvent } from 'react';
import appleMark from './assets/apple-mark.svg';
import chongchongLogo from './assets/chongchong-logo.png';
import googlePlayMark from './assets/google-play-mark.svg';
import FeatureCarousel from './components/FeatureCarousel';
import { TRANSITION_DURATION, useFeaturePreview } from './useFeaturePreview';
import { previewFeatures } from './previewFeatures';
import { usePostHog } from '@posthog/react';
import { Link } from 'react-router';

const pretendardFont = new URL('./assets/PretendardVariable.woff2', import.meta.url).href;
const doHyeonFont = new URL('./assets/DoHyeon-Regular.ttf', import.meta.url).href;
type StoreName = 'App Store' | 'Google Play';

// BrowserRouter / RouterProvider는 기존 프로젝트의 설정을 그대로 사용합니다.
export default function App() {
  const [activeStore, setActiveStore] = useState<StoreName | null>(null);
  const preview = useFeaturePreview({ suspended: activeStore !== null });
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const previousTitle = document.title;
    document.title = '총총 — 스터디 운영을 더 가볍게';
    return () => {
      document.title = previousTitle;
    };
  }, []);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (activeStore && !dialog.open) dialog.showModal();
    else if (!activeStore && dialog.open) dialog.close();
  }, [activeStore]);

  const closeDialog = () => dialogRef.current?.close();

  const posthog = usePostHog();

  const handleClickWeb = () => {
    posthog?.capture('web_button_clicked', {
      location: 'landing_page',
    });
  };

  const openStoreDialog = (event: MouseEvent<HTMLButtonElement>, store: StoreName) => {
    triggerRef.current = event.currentTarget;
    setActiveStore(store);
  };

  const handleClickAndroid = (event: MouseEvent<HTMLButtonElement>) => {
    posthog?.capture('android_button_clicked', {
      location: 'landing_page',
    });

    openStoreDialog(event, 'Google Play');
  };

  const handleClickIos = (event: MouseEvent<HTMLButtonElement>) => {
    posthog?.capture('ios_button_clicked', {
      location: 'landing_page',
    });

    openStoreDialog(event, 'App Store');
  };

  return (
    <div className="cc-landing" lang="ko">
      <style>{LANDING_STYLES}</style>
      <div className="cc-site-shell">
        <header className="cc-site-header">
          <Link className="cc-brand" to={'/studies'} aria-label="총총 웹사이트로 이동">
            <span className="cc-brand-mark" aria-hidden="true">
              <img src={chongchongLogo} alt="" width={232} height={256} />
            </span>
            <span>총총</span>
          </Link>
        </header>

        <main className="cc-hero" aria-labelledby="cc-hero-title">
          <section className="cc-copy">
            <div
              className="cc-feature-copy"
              aria-live={preview.playing ? 'off' : 'polite'}
              aria-atomic="true"
            >
              <div key={preview.feature.id} className="cc-feature-copy-content">
                <p className="cc-eyebrow">
                  {preview.feature.label}
                  <span className="cc-feature-number">
                    {String(preview.activeIndex + 1).padStart(2, '0')} /{' '}
                    {String(previewFeatures.length).padStart(2, '0')}
                  </span>
                </p>
                <h1 id="cc-hero-title">{preview.feature.title}</h1>
                <p className="cc-lead">{preview.feature.description}</p>
              </div>
            </div>

            <div className="cc-actions" aria-label="총총 이용 경로">
              <Link
                className="cc-destination cc-destination--web"
                to={'/studies'}
                onClick={handleClickWeb}
              >
                <svg
                  className="cc-destination-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.8" />
                  <path
                    d="M3.5 12h17M12 3c2.4 2.5 3.7 5.5 3.7 9S14.4 18.5 12 21c-2.4-2.5-3.7-5.5-3.7-9S9.6 5.5 12 3Z"
                    stroke="currentColor"
                    strokeWidth="1.8"
                  />
                </svg>
                <span className="cc-destination-copy">
                  <span className="cc-destination-name">웹사이트</span>
                </span>
              </Link>

              <button
                className="cc-destination cc-store-button"
                data-store="App Store"
                type="button"
                aria-haspopup="dialog"
                aria-controls="cc-release-dialog"
                onClick={handleClickIos}
              >
                <span className="cc-store-mark" aria-hidden="true">
                  <img src={appleMark} alt="" width={24} height={24} />
                </span>
                <span className="cc-destination-copy">
                  <span className="cc-destination-name">App Store</span>
                </span>
              </button>

              <button
                className="cc-destination cc-store-button"
                data-store="Google Play"
                type="button"
                aria-haspopup="dialog"
                aria-controls="cc-release-dialog"
                onClick={handleClickAndroid}
              >
                <span className="cc-store-mark" aria-hidden="true">
                  <img src={googlePlayMark} alt="" width={24} height={24} />
                </span>
                <span className="cc-destination-copy">
                  <span className="cc-destination-name">Google Play</span>
                </span>
              </button>
            </div>
          </section>

          <FeatureCarousel preview={preview} />
        </main>
      </div>

      <dialog
        ref={dialogRef}
        className="cc-release-dialog"
        id="cc-release-dialog"
        aria-labelledby="cc-release-dialog-title"
        aria-describedby="cc-release-dialog-description"
        onClose={() => {
          setActiveStore(null);
          if (triggerRef.current?.isConnected) triggerRef.current.focus();
        }}
        onClick={(event) => {
          if (event.target === event.currentTarget) closeDialog();
        }}
      >
        <div className="cc-release-dialog-panel">
          <button
            className="cc-release-dialog-close"
            type="button"
            aria-label="준비 중 안내 닫기"
            onClick={closeDialog}
          >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="m7 7 10 10M17 7 7 17"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
          </button>
          <span className="cc-release-dialog-bunny" aria-hidden="true">
            <img src={chongchongLogo} alt="" width={232} height={256} />
          </span>
          <p className="cc-release-dialog-store">{activeStore ?? 'App Store'}</p>
          <h2 id="cc-release-dialog-title">열심히 준비하고 있어요</h2>
          <p className="cc-release-dialog-description" id="cc-release-dialog-description">
            총총이가 마지막 점검 중이에요.
            <br />
            조금만 기다려 주세요.
          </p>
          <button className="cc-release-dialog-confirm" type="button" onClick={closeDialog}>
            알겠어요
          </button>
        </div>
      </dialog>
    </div>
  );
}

const LANDING_STYLES = `
@font-face {
      font-family: "Chongchong Pretendard";
      src: url("${pretendardFont}") format("woff2-variations");
      font-style: normal;
      font-weight: 45 920;
      font-display: swap;
    }
    @font-face {
      font-family: "Chongchong Do Hyeon";
      src: url("${doHyeonFont}") format("truetype");
      font-style: normal;
      font-weight: 400;
      font-display: swap;
    }

    .cc-landing {
      --surface-page-base: #ffffff;
      --surface-page: var(--surface-page-base);
      --surface-card: #ffffff;
      --surface-soft: #eaf8f1;
      --surface-control: var(--surface-card);
      --surface-control-hover: #f4f6f5;
      --glass-border: rgba(255, 255, 255, .92);
      --brand: #00b96b;
      --brand-deep: #007f4d;
      --brand-dark: #005c38;
      --brand-light: #58dea2;
      --ink: #102019;
      --muted: #63736b;
      --line: rgba(16, 32, 25, .12);
      --on-brand: #ffffff;
      --black: #050706;
      --frame-light: #f8faf9;
      --frame-mid: #919b96;
      --frame-dark: #252b28;
      --frame-highlight: #eff3f1;
      --alpha-card: rgba(255, 255, 255, .82);
      --alpha-white-95: rgba(255, 255, 255, .95);
      --alpha-white-90: rgba(255, 255, 255, .9);
      --alpha-white-50: rgba(255, 255, 255, .5);
      --alpha-white-45: rgba(255, 255, 255, .45);
      --alpha-white-26: rgba(255, 255, 255, .26);
      --alpha-brand-35: rgba(0, 127, 77, .35);
      --alpha-brand-30: rgba(88, 222, 162, .3);
      --alpha-brand-28: rgba(0, 127, 77, .28);
      --alpha-brand-24: rgba(88, 222, 162, .24);
      --alpha-brand-22: rgba(0, 127, 77, .22);
      --alpha-brand-20: rgba(0, 127, 77, .2);
      --alpha-brand-14: rgba(0, 185, 107, .14);
      --alpha-brand-13: rgba(0, 127, 77, .13);
      --alpha-brand-10: rgba(0, 185, 107, .1);
      --alpha-brand-08: rgba(0, 185, 107, .08);
      --shadow-dialog: 0 24px 80px rgba(16, 32, 25, .22);
      --glass-saturation: 145%;
      --space-1: 4px;
      --space-2: 8px;
      --space-3: 12px;
      --space-4: 16px;
      --space-5: 20px;
      --space-6: 24px;
      --space-8: 32px;
      --space-10: 40px;
      --content: 1180px;
      --radius-button: 14px;
      --control-radius: var(--radius-button);
      --action-bar-gap: 6px;
      --line-thin: 1px;
      --focus-ring-width: 3px;
      --focus-ring-offset: 3px;
      --sr-only-size: 1px;
      --header-height: 76px;
      --hero-visual-min: 340px;
      --hero-narrow-column: 290px;
      --hero-gap: clamp(32px, 6vw, 88px);
      --hero-orbit-width: min(54vw, 680px);
      --title-max-width: 740px;
      --lead-max-width: 560px;
      --actions-max-width: 600px;
      --brand-mark-size: 40px;
      --brand-mark-radius: 14px;
      --brand-type-size: 1.25rem;
      --control-height: 58px;
      --control-icon-size: 22px;
      --control-name-size: var(--font-button);
      --control-hover-transform: translateY(-2px) scale(1.01);
      --control-active-transform: translateY(1px) scale(.97);
      --dialog-width: 390px;
      --dialog-overlay: rgba(16, 32, 25, .42);
      --dialog-backdrop-blur: 5px;
      --dialog-padding-top: 54px;
      --dialog-radius: 30px;
      --dialog-bunny-box: 94px;
      --dialog-bunny-radius: 28px;
      --dialog-bunny-width: 72px;
      --dialog-bunny-height: 78px;
      --dialog-title-size: 1.65rem;
      --dialog-body-size: .95rem;
      --dialog-confirm-height: 48px;
      --dialog-confirm-radius: 14px;
      --dialog-control-size: 44px;
      --dialog-close-icon-size: 20px;
      --dialog-bunny-start-y: 18px;
      --dialog-bunny-overshoot-y: -8px;
      --dialog-folded-clip: inset(48% 48% 48% 48% round 30px);
      --dialog-open-clip: inset(0 0 0 0 round 30px);
      --entry-distance: 24px;
      --copy-mobile-padding-top: clamp(12px, 3vh, 32px);
      --motion-micro: 180ms cubic-bezier(.16, 1, .3, 1);
      --motion-entry: 560ms cubic-bezier(.16, 1, .3, 1);
      --motion-dialog: 430ms cubic-bezier(.2, 0, .2, 1);
      --font-display: clamp(2.35rem, 5.6vw, 5.5rem);
      --font-display-family: "Chongchong Do Hyeon", "Chongchong Pretendard", sans-serif;
      --font-display-weight: 400;
      --font-display-line-height: 1.02;
      --font-display-tracking: -.032em;
      --font-lead: clamp(.98rem, 1.4vw, 1.2rem);
      --font-label: .875rem;
      --font-button: .875rem;
    }

    html:has(.cc-landing) {
      overflow-x: clip;
      overflow-y: auto;
      background: #ffffff;
      color-scheme: light;
    }
    body:has(.cc-landing) {
      display: block;
      min-width: 0;
      margin: 0;
      padding: 0;
      overflow: visible;
      background: #ffffff;
    }
    #root:has(.cc-landing), #app:has(.cc-landing) {
      width: 100%;
      max-width: none;
      margin: 0;
      padding: 0;
      text-align: initial;
    }
    .cc-landing, .cc-landing *, .cc-landing *::before, .cc-landing *::after {
      box-sizing: border-box;
    }
    .cc-landing {
      width: 100%;
      min-height: 100vh;
      color: var(--ink);
      background: var(--surface-page-base);
      color-scheme: light;
      font-family: "Chongchong Pretendard", Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      line-height: 1.5;
      letter-spacing: -.025em;
      text-align: start;
      -webkit-font-smoothing: antialiased;
    }
    .cc-landing :where(.cc-brand, .cc-destination, .cc-feature-dot, .cc-release-dialog button) { font: inherit; }
    .cc-landing :where(button.cc-destination, .cc-feature-dot, .cc-release-dialog button) { border: 0; padding: 0; background: none; }
    .cc-landing :where(a.cc-brand, a.cc-destination) { color: inherit; text-decoration: none; }
    .cc-landing :where(a.cc-brand, a.cc-destination):hover { color: var(--ink); text-decoration: none; }
    .cc-sr-only {
      position: absolute;
      width: var(--sr-only-size);
      height: var(--sr-only-size);
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      clip-path: inset(50%);
    }

    .cc-site-shell {
      position: relative;
      display: grid;
      min-height: 100vh;
      min-height: 100dvh;
      grid-template-rows: auto minmax(0, 1fr);
      overflow: hidden;
      border: 0;
      border-radius: 0;
      background: var(--surface-page);
      isolation: isolate;
    }
    .cc-site-header {
      position: relative;
      z-index: 10;
      display: flex;
      width: min(calc(100% - var(--space-10) * 2), var(--content));
      min-height: var(--header-height);
      margin: 0 auto;
      align-items: center;
      justify-content: space-between;
    }
    .cc-brand {
      display: inline-flex;
      align-items: center;
      gap: var(--space-3);
      font-size: var(--brand-type-size);
      font-family: "Chongchong Pretendard", Pretendard, sans-serif;
      font-weight: 850;
    }
    .cc-brand-mark {
      display: grid;
      width: var(--brand-mark-size);
      height: var(--brand-mark-size);
      place-items: center;
    }
    .cc-brand-mark img { width: 100%; height: 100%; object-fit: contain; }

    .cc-hero {
      position: relative;
      z-index: 2;
      display: grid;
      width: min(calc(100% - var(--space-10) * 2), var(--content));
      min-height: 0;
      margin: 0 auto;
      grid-template-columns: minmax(0, 1.08fr) minmax(var(--hero-visual-min), .92fr);
      align-items: center;
      gap: var(--hero-gap);
    }
    .cc-hero::before {
      position: absolute;
      z-index: -1;
      right: -14%;
      width: var(--hero-orbit-width);
      aspect-ratio: 1;
      border: var(--line-thin) solid var(--alpha-brand-14);
      border-radius: 50%;
      content: none;
    }
    .cc-copy {
      z-index: 2;
      align-self: center;
      animation: cc-enter-left var(--motion-entry) both;
    }
    .cc-eyebrow {
      display: inline-flex;
      margin: 0 0 var(--space-5);
      align-items: center;
      color: var(--brand-deep);
      font-size: var(--font-label);
      font-weight: 720;
    }
    .cc-feature-copy h1 {
      max-width: var(--title-max-width);
      margin: 0;
      font-size: var(--font-display);
      font-family: var(--font-display-family);
      font-weight: var(--font-display-weight);
      line-height: var(--font-display-line-height);
      letter-spacing: var(--font-display-tracking);
      word-break: keep-all;
    }
    .cc-feature-copy h1, .cc-feature-copy .cc-lead { white-space: pre-line; }
    .cc-feature-copy-content { animation: cc-feature-copy-in ${TRANSITION_DURATION}ms ease-out both; }
    .cc-feature-number { margin-left: 14px; color: var(--muted); font-size: 11px; font-weight: 500; letter-spacing: .08em; font-variant-numeric: tabular-nums; }
    .cc-lead {
      max-width: var(--lead-max-width);
      margin: var(--space-6) 0 0;
      color: var(--muted);
      font-size: var(--font-lead);
      font-weight: 520;
      line-height: 1.65;
      word-break: keep-all;
    }
    .cc-actions {
      display: grid;
      max-width: var(--actions-max-width);
      margin-top: var(--space-8);
      grid-template-columns: minmax(0, .85fr) minmax(0, 1fr) minmax(0, 1.18fr);
      gap: var(--action-bar-gap);
    }
    .cc-destination {
      position: relative;
      display: flex;
      min-width: 0;
      min-height: var(--control-height);
      padding: var(--space-2);
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      border: var(--line-thin) solid var(--line);
      border-radius: var(--control-radius);
      color: var(--ink);
      background: var(--surface-control);
      cursor: pointer;
      text-align: center;
      transition: transform var(--motion-micro), background-color var(--motion-micro), color var(--motion-micro);
      -webkit-tap-highlight-color: transparent;
    }
    .cc-destination > * { position: relative; z-index: 1; }
    .cc-destination:hover {
      border-color: var(--line);
      background: var(--surface-control-hover);
      transform: var(--control-hover-transform);
    }
    .cc-destination:active { transform: var(--control-active-transform); }
    .cc-destination:focus-visible, .cc-brand:focus-visible {
      outline: var(--focus-ring-width) solid var(--brand-deep);
      outline-offset: var(--focus-ring-offset);
    }
    .cc-destination--web {
      color: var(--ink);
      border-color: var(--line);
      background: var(--surface-control);
    }
    .cc-destination--web:hover { background: var(--surface-control-hover); }
    .cc-store-button {
      color: var(--ink);
      border-color: var(--line);
      background: var(--surface-control);
    }
    .cc-store-button:hover { background: var(--surface-control-hover); }
    .cc-store-button[data-store="App Store"] .cc-store-mark img { filter: none; }
    .cc-store-button[data-store="Google Play"] {
      color: var(--ink);
      border-color: var(--line);
      background: var(--surface-control);
    }
    .cc-store-button[data-store="Google Play"]:hover { background: var(--surface-control-hover); }
    .cc-store-mark {
      display: grid;
      width: var(--control-icon-size);
      height: var(--control-icon-size);
      flex: 0 0 var(--control-icon-size);
      place-items: center;
    }
    .cc-store-mark img { display: block; width: var(--control-icon-size); height: var(--control-icon-size); object-fit: contain; }
    .cc-destination-icon { width: var(--control-icon-size); height: var(--control-icon-size); flex: 0 0 var(--control-icon-size); }
    .cc-destination-copy { display: grid; min-width: 0; }
    .cc-destination-name {
      font-size: var(--control-name-size);
      font-weight: 700;
      line-height: 1.2;
      white-space: normal;
      overflow-wrap: anywhere;
    }

    .cc-release-dialog {
      width: min(calc(100% - var(--space-8) * 2), var(--dialog-width));
      max-width: none;
      max-height: calc(100vh - 32px);
      max-height: calc(100dvh - 32px);
      padding: 0;
      overflow: auto;
      overscroll-behavior: contain;
      border: 0;
      color: var(--ink);
      background: transparent;
    }
    .cc-release-dialog::backdrop {
      background: var(--dialog-overlay);
      backdrop-filter: blur(var(--dialog-backdrop-blur));
      -webkit-backdrop-filter: blur(var(--dialog-backdrop-blur));
      animation: cc-dialog-backdrop-in 180ms ease-out both;
    }
    .cc-release-dialog[open] .cc-release-dialog-panel {
      animation: cc-dialog-unfold var(--motion-dialog) both;
    }
    .cc-release-dialog-panel {
      position: relative;
      overflow: hidden;
      padding: var(--dialog-padding-top) var(--space-8) var(--space-8);
      border: var(--line-thin) solid var(--line);
      border-radius: var(--dialog-radius);
      background: var(--surface-card);
      box-shadow: var(--shadow-dialog);
      text-align: center;
    }
    .cc-release-dialog-bunny {
      display: grid;
      width: var(--dialog-bunny-box);
      height: var(--dialog-bunny-box);
      margin: 0 auto var(--space-4);
      place-items: center;
      border-radius: var(--dialog-bunny-radius);
      background: var(--surface-soft);
    }
    .cc-release-dialog-bunny img {
      width: var(--dialog-bunny-width);
      height: var(--dialog-bunny-height);
      object-fit: contain;
      animation: cc-dialog-bunny-hop 700ms cubic-bezier(.16, 1, .3, 1) 120ms both;
    }
    .cc-release-dialog-store {
      margin: 0 0 var(--space-2);
      color: var(--brand-deep);
      font-size: var(--font-label);
      font-weight: 800;
    }
    .cc-release-dialog h2 {
      margin: 0;
      font-size: var(--dialog-title-size);
      line-height: 1.15;
      text-wrap: balance;
    }
    .cc-release-dialog-description {
      margin: var(--space-3) 0 0;
      color: var(--muted);
      font-size: var(--dialog-body-size);
      line-height: 1.55;
      word-break: keep-all;
    }
    .cc-release-dialog-confirm {
      width: 100%;
      min-height: var(--dialog-confirm-height);
      margin-top: var(--space-6);
      border-radius: var(--dialog-confirm-radius);
      color: var(--on-brand);
      background: var(--brand-deep);
      font-weight: 800;
      cursor: pointer;
      transition: transform var(--motion-micro), background-color var(--motion-micro);
    }
    .cc-release-dialog-confirm:hover { background: var(--brand-dark); }
    .cc-release-dialog-confirm:active { transform: scale(.97); }
    .cc-release-dialog-close {
      position: absolute;
      top: var(--space-4);
      right: var(--space-4);
      display: grid;
      width: var(--dialog-control-size);
      height: var(--dialog-control-size);
      place-items: center;
      border-radius: 50%;
      color: var(--muted);
      background: var(--surface-soft);
      cursor: pointer;
    }
    .cc-release-dialog-close svg { width: var(--dialog-close-icon-size); height: var(--dialog-close-icon-size); }
    .cc-release-dialog button:focus-visible { outline: var(--focus-ring-width) solid var(--brand-deep); outline-offset: var(--focus-ring-offset); }

    @keyframes cc-enter-left {
      from { opacity: 0; transform: translateX(calc(var(--entry-distance) * -1)); }
      to { opacity: 1; transform: none; }
    }
    @keyframes cc-enter-right {
      from { opacity: 0; transform: translateX(var(--entry-distance)); }
      to { opacity: 1; transform: none; }
    }
    @keyframes cc-feature-copy-in {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: none; }
    }
    @keyframes cc-dialog-backdrop-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes cc-dialog-unfold {
      from { opacity: 0; clip-path: var(--dialog-folded-clip); }
      to { opacity: 1; clip-path: var(--dialog-open-clip); }
    }
    @keyframes cc-dialog-bunny-hop {
      from { opacity: 0; transform: translateY(var(--dialog-bunny-start-y)) scale(.82); }
      62% { opacity: 1; transform: translateY(var(--dialog-bunny-overshoot-y)) scale(1.06); }
      to { opacity: 1; transform: none; }
    }

    @media (max-width: 900px) {
      .cc-site-header, .cc-hero { width: min(calc(100% - var(--space-6) * 2), var(--content)); }
      .cc-hero { grid-template-columns: minmax(0, 1fr) var(--hero-narrow-column); gap: var(--space-6); }
      .cc-actions { grid-template-columns: minmax(0, .85fr) minmax(0, 1fr) minmax(0, 1.18fr); }
    }

    @media (max-width: 720px) {
      .cc-landing {
        --header-height: 60px;
        --brand-mark-size: 34px;
        --brand-mark-radius: 12px;
        --brand-type-size: 1.08rem;
        --control-height: 52px;
        --control-icon-size: 18px;
        --control-name-size: .75rem;
        --dialog-width: 360px;
        --dialog-padding-top: 48px;
        --dialog-radius: 26px;
        --dialog-bunny-box: 82px;
        --dialog-bunny-width: 62px;
        --dialog-bunny-height: 68px;
        --font-display: clamp(2.05rem, 10vw, 3.15rem);
        --font-lead: .95rem;
        --action-bar-gap: 5px;
      }
      .cc-site-header { min-height: var(--header-height); }
      .cc-brand-mark img { width: 100%; height: 100%; }
      .cc-hero {
        grid-template-columns: 1fr;
        grid-template-rows: auto minmax(0, 1fr);
        align-content: start;
        gap: var(--space-4);
        text-align: center;
      }
      .cc-copy { padding-top: var(--copy-mobile-padding-top); }
      .cc-eyebrow { margin-bottom: var(--space-4); }
      .cc-feature-copy h1 { font-size: var(--font-display); }
      .cc-lead { margin: var(--space-4) auto 0; font-size: var(--font-lead); line-height: 1.55; }
      .cc-actions { margin-top: var(--space-5); max-width: none; grid-template-columns: minmax(0, .85fr) minmax(0, 1fr) minmax(0, 1.18fr); }
      .cc-destination { padding: var(--space-2); gap: var(--space-1); }
      .cc-destination-name { font-size: var(--control-name-size); }
      .cc-release-dialog { width: min(calc(100% - var(--space-6) * 2), var(--dialog-width)); }
      .cc-release-dialog-panel { padding: var(--dialog-padding-top) var(--space-6) var(--space-6); border-radius: var(--dialog-radius); }
      .cc-release-dialog-bunny { width: var(--dialog-bunny-box); height: var(--dialog-bunny-box); }
      .cc-release-dialog-bunny img { width: var(--dialog-bunny-width); height: var(--dialog-bunny-height); }
    }

    @media (max-height: 700px) and (max-width: 720px) {
      .cc-landing {
        --header-height: 52px;
        --control-height: 48px;
        --font-display: clamp(1.8rem, 8.7vw, 2.35rem);
        --font-lead: .875rem;
      }
      .cc-site-header { min-height: var(--header-height); }
      .cc-copy { padding-top: var(--space-1); }
      .cc-eyebrow { margin-bottom: var(--space-3); }
      .cc-feature-copy h1 { font-size: var(--font-display); }
      .cc-lead { margin-top: var(--space-3); font-size: var(--font-lead); }
      .cc-actions { margin-top: var(--space-4); }
    }

    @media (max-width: 360px) {
      .cc-landing { --control-icon-size: 16px; --control-name-size: .6875rem; }
      .cc-destination { padding: 8px 4px; gap: 3px; }
    }

    @media (prefers-reduced-motion: reduce) {
      .cc-copy, .cc-feature-copy-content { animation: none; }
      .cc-destination { transition: none; }
      .cc-destination:hover, .cc-destination:active { transform: none; }
      .cc-release-dialog-bunny img, .cc-release-dialog[open] .cc-release-dialog-panel, .cc-release-dialog::backdrop { animation: none; }
    }
`;
