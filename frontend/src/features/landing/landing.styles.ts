/**
 * 랜딩 전용 스타일. 모든 규칙은 `.cc-landing` 아래로 한정해 다른 화면에 새지 않게 했어요.
 * 스크롤 연동 값(--p 등)과 애니메이션 상태 클래스(is-in, chips-on 등)는 컴포넌트가 붙여요.
 */
export const LANDING_STYLES = `
body:has(.cc-landing) { display: block; min-width: 0; margin: 0; padding: 0; overflow: visible; background: #fff; }
#root:has(.cc-landing), #app:has(.cc-landing) { width: 100%; max-width: none; margin: 0; padding: 0; text-align: initial; }
.cc-landing {
    --brand: #00c471;
    --brand-strong: #00a862;
    --brand-soft: #e6f9f0;
    --grey-900: #191f28;
    --grey-800: #333d4b;
    --grey-700: #4e5968;
    --grey-600: #6b7684;
    --grey-500: #8b95a1;
    --grey-300: #d1d6db;
    --grey-200: #e5e8eb;
    --grey-100: #f2f4f6;
    --grey-50: #f9fafb;
    --header: 60px;
    --content: 1140px;
    --gutter: 24px;
    --ease: cubic-bezier(.16, 1, .3, 1);
    --phone: 320px; }
.cc-landing *, .cc-landing *::before, .cc-landing *::after { box-sizing: border-box; }
html:has(.cc-landing) { -webkit-text-size-adjust: 100%; scroll-behavior: smooth; }
.cc-landing {
    margin: 0;
    color: var(--grey-900);
    background: #fff;
    font-family: "Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, system-ui, "Apple SD Gothic Neo", sans-serif;
    line-height: 1.5;
    letter-spacing: -.02em;
    word-break: keep-all;
    -webkit-font-smoothing: antialiased;
    overflow-x: clip; }
.cc-landing h1, .cc-landing h2, .cc-landing h3, .cc-landing p, .cc-landing ul, .cc-landing ol { margin: 0; padding: 0; }
.cc-landing ul, .cc-landing ol { list-style: none; }
.cc-landing a { color: inherit; text-decoration: none; }
.cc-landing img { display: block; max-width: 100%; height: auto; }
.cc-landing button { font: inherit; color: inherit; }
.cc-landing :focus-visible { outline: 3px solid var(--brand); outline-offset: 3px; }
.cc-landing .container { width: min(calc(100% - var(--gutter) * 2), var(--content)); margin: 0 auto; }
.cc-landing .header {
    position: fixed; inset: 0 0 auto; z-index: 100;
    height: var(--header);
    background: rgba(255, 255, 255, .9);
    backdrop-filter: saturate(180%) blur(20px);
    -webkit-backdrop-filter: saturate(180%) blur(20px);
    transition: box-shadow .2s ease; }
.cc-landing .header.is-scrolled { box-shadow: 0 1px 0 rgba(0, 0, 0, .06); }
.cc-landing .header-inner { display: flex; height: 100%; align-items: center; gap: 40px; }
.cc-landing .brand { display: flex; align-items: center; }
.cc-landing .brand img { width: 74px; height: auto; }
.cc-landing .nav { display: flex; gap: 28px; font-size: 15px; font-weight: 500; color: var(--grey-700); }
.cc-landing .nav a:hover { color: var(--grey-900); }
.cc-landing .header-cta {
    margin-left: auto;
    display: inline-flex; align-items: center; height: 36px; padding: 0 14px;
    border-radius: 8px; background: var(--brand); color: #fff;
    font-size: 14px; font-weight: 600;
    transition: background-color .15s ease, transform .15s ease; }
.cc-landing .header-cta:hover { background: var(--brand-strong); }
.cc-landing .header-cta:active { transform: scale(.96); }
.cc-landing .indicator {
    position: fixed; z-index: 90; left: 28px; top: 50%;
    display: grid; gap: 6px; transform: translateY(-50%);
    opacity: 0; transition: opacity .3s ease; }
.cc-landing .indicator.is-visible { opacity: 1; }
.cc-landing .indicator a { display: block; width: 14px; height: 3px; border-radius: 2px; background: var(--grey-300); transition: width .3s var(--ease), background-color .3s ease; }
.cc-landing .indicator a.is-active { width: 24px; background: var(--grey-900); }
.cc-landing [data-reveal] { opacity: 0; transform: translateY(40px); transition: opacity 1s var(--ease), transform 1s var(--ease); transition-delay: var(--d, 0s); }
.cc-landing [data-reveal].is-in { opacity: 1; transform: none; }
.cc-landing .phone {
    position: relative; flex: none;
    width: var(--phone);
    padding: calc(var(--phone) * .012);
    border-radius: calc(var(--phone) * .152);
    background: #0b0b0d;
    box-shadow:
      0 0 0 1.5px #a9adb4,
      0 0 0 2.5px #d9dce0,
      0 44px 90px -32px rgba(25, 31, 40, .32),
      0 16px 34px -18px rgba(25, 31, 40, .22); }
.cc-landing .screen {
    --u: calc(100cqw / 390);
    position: relative; overflow: hidden;
    aspect-ratio: 390 / 844;
    border-radius: calc(var(--phone) * .14);
    background: #fff;
    container-type: inline-size;
    isolation: isolate; }
.cc-landing .screen::after { 
    content: ""; position: absolute; z-index: 20; left: 50%; top: calc(11 * var(--u));
    width: calc(120 * var(--u)); height: calc(34 * var(--u));
    border-radius: 99px; background: #000; transform: translateX(-50%); }
.cc-landing .screen > img { width: 100%; height: auto; }
.cc-landing .hero { position: relative; height: 230vh; }
.cc-landing .hero-sticky { position: sticky; top: 0; height: 100vh; height: 100svh; overflow: hidden; }
.cc-landing .hero-bg {
    position: absolute; inset: 0;
    background:
      radial-gradient(60% 50% at 50% 100%, rgba(0, 196, 113, .16), transparent 70%),
      linear-gradient(180deg, #fff 0%, #f3fbf7 100%); }
.cc-landing .hero-copy {
    position: relative; z-index: 3;
    padding-top: calc(var(--header) + clamp(48px, 9vh, 104px));
    text-align: center;
    will-change: opacity, transform; }
.cc-landing .hero-title { font-size: clamp(40px, 6.2vw, 76px); font-weight: 700; line-height: 1.28; letter-spacing: -.035em; }
.cc-landing .hero-title .accent { color: var(--brand); }
.cc-landing .hero-sub { margin-top: 22px; color: var(--grey-600); font-size: clamp(17px, 1.6vw, 21px); font-weight: 500; line-height: 1.6; }
.cc-landing .hero-actions { display: flex; justify-content: center; gap: 10px; margin-top: 36px; }
.cc-landing .btn {
    display: inline-flex; align-items: center; justify-content: center;
    height: 56px; padding: 0 28px; border: 0; border-radius: 16px;
    font-size: 17px; font-weight: 600; cursor: pointer;
    transition: background-color .15s ease, transform .15s ease; }
.cc-landing .btn:active { transform: scale(.96); }
.cc-landing .btn-primary { background: var(--brand); color: #fff; }
.cc-landing .btn-primary:hover { background: var(--brand-strong); }
.cc-landing .btn-ghost { background: var(--grey-100); color: var(--grey-800); }
.cc-landing .btn-ghost:hover { background: var(--grey-200); }
.cc-landing .hero-stage {
    --phone: min(clamp(250px, 24vw, 330px), calc((100svh - var(--header) - 48px) / 2.14));
    position: absolute; z-index: 2; left: 50%; bottom: 0;
    transform: translate3d(-50%, 0, 0) scale(.92);
    will-change: transform;
    transform-origin: 50% 100%;
    padding-bottom: clamp(16px, 4vh, 48px); }
.cc-landing .m-br { display: none; }
.cc-landing .chip {
    position: absolute; z-index: 4;
    display: flex; align-items: center; gap: 12px;
    padding: 14px 18px 14px 14px; border-radius: 20px;
    background: rgba(255, 255, 255, .94);
    box-shadow: 0 16px 40px rgba(25, 31, 40, .12), 0 0 0 1px rgba(25, 31, 40, .04);
    width: max-content;
    white-space: nowrap;
    opacity: 0;
    transform: translateY(18px) scale(.86); }
.cc-landing .hero-stage.chips-on .chip { animation: cc-chip-pop .55s cubic-bezier(.34, 1.56, .64, 1) var(--delay) forwards; }
@keyframes cc-chip-pop {
    0% { opacity: 0; transform: translateY(18px) scale(.86); }
    60% { opacity: 1; transform: translateY(-4px) scale(1.04); }
    100% { opacity: 1; transform: none; }
  }
.cc-landing .chip img { width: 40px; height: 40px; border-radius: 12px; background: var(--brand-soft); padding: 4px; object-fit: contain; }
.cc-landing .chip small { display: block; color: var(--grey-500); font-size: 13px; }
.cc-landing .chip b { display: block; font-size: 15px; font-weight: 600; }
.cc-landing .chip .num { color: var(--brand); }
.cc-landing .chip-1 { --delay: 0s; right: calc(100% + 28px); top: 22%; }
.cc-landing .chip-2 { --delay: .3s; left: calc(100% + 28px); top: 38%; }
.cc-landing .chip-3 { --delay: .6s; right: calc(100% + 12px); top: 58%; }
.cc-landing .statement { position: relative; height: 240vh; }
.cc-landing .statement-sticky { position: sticky; top: 0; display: grid; height: 100vh; height: 100svh; place-items: center; }
.cc-landing .statement p { text-align: center; font-size: clamp(30px, 4.6vw, 56px); font-weight: 700; line-height: 1.42; letter-spacing: -.035em; }
.cc-landing .statement .w { color: var(--grey-200); transition: color .25s ease; }
.cc-landing .statement .w.on { color: var(--grey-900); }
.cc-landing .statement .w.on.hl { color: var(--brand); }
.cc-landing .eyebrow { color: var(--brand); font-size: 17px; font-weight: 600; }
.cc-landing .title { margin-top: 12px; font-size: clamp(32px, 4.2vw, 52px); font-weight: 700; line-height: 1.34; letter-spacing: -.035em; }
.cc-landing .desc { margin-top: 20px; color: var(--grey-600); font-size: clamp(16px, 1.45vw, 19px); line-height: 1.7; }
.cc-landing .split-sec { padding: clamp(120px, 18vh, 200px) 0; }
.cc-landing .split-sec.grey { background: var(--grey-50); }
.cc-landing .split { --phone: min(clamp(250px, 25vw, 330px), calc((100svh - var(--header) - 56px) / 2.14)); display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: clamp(48px, 7vw, 112px); align-items: center; }
.cc-landing .split .visual { position: relative; display: flex; justify-content: center; }
.cc-landing .split.reverse .copy { order: 2; }
.cc-landing .facts { display: grid; gap: 14px; margin-top: 36px; }
.cc-landing .fact { display: flex; gap: 12px; align-items: flex-start; color: var(--grey-700); font-size: 16px; line-height: 1.6; }
.cc-landing .fact::before { content: ""; flex: none; width: 20px; height: 20px; margin-top: 2px; border-radius: 50%; background: var(--brand-soft) url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20'%3E%3Cpath d='m6 10.2 2.6 2.6L14 7.4' fill='none' stroke='%2300c471' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") center / 20px no-repeat; }
.cc-landing .steps-sec { padding: clamp(120px, 18vh, 200px) 0; }
.cc-landing .steps-head { text-align: center; }
.cc-landing .steps { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-top: 64px; }
.cc-landing .step { display: flex; flex-direction: column; overflow: hidden; border-radius: 28px; background: var(--grey-100); }
.cc-landing .step-copy { padding: 32px 32px 0; }
.cc-landing .step-num { color: var(--brand); font-size: 15px; font-weight: 700; }
.cc-landing .step h3 { margin-top: 8px; font-size: 24px; font-weight: 700; letter-spacing: -.03em; }
.cc-landing .step p { margin-top: 8px; color: var(--grey-600); font-size: 16px; line-height: 1.6; }
.cc-landing .final { padding: clamp(120px, 20vh, 220px) 0; text-align: center; background: linear-gradient(180deg, #fff 0%, #ecfaf3 100%); }
.cc-landing .final .mascot { width: 168px; margin: 0 auto 28px; }
.cc-landing .final .title { margin-top: 0; }
.cc-landing .final .hero-actions { margin-top: 40px; }
.cc-landing .final-note { margin-top: 18px; color: var(--grey-500); font-size: 14px; }
.cc-landing .footer { padding: 56px 0 72px; background: var(--grey-50); color: var(--grey-600); font-size: 14px; }
.cc-landing .footer-top { display: flex; justify-content: space-between; align-items: center; gap: 24px; }
.cc-landing .footer .brand img { width: 64px; filter: grayscale(1); opacity: .55; }
.cc-landing .footer-links { display: flex; flex-wrap: wrap; gap: 12px 24px; font-weight: 500; }
.cc-landing .footer-links a:hover { color: var(--grey-900); }
.cc-landing .footer-info { margin-top: 28px; color: var(--grey-500); font-size: 13px; line-height: 1.8; }
.cc-landing .sec-head { text-align: center; }
.cc-landing .sec-head .desc { margin-left: auto; margin-right: auto; }
.cc-landing .ui { overflow: hidden; border-radius: 22px; background: #fff; box-shadow: 0 1px 0 rgba(25, 31, 40, .04), 0 18px 40px -12px rgba(25, 31, 40, .14); }
.cc-landing .ui img { width: 100%; }
.cc-landing .bento-sec { padding: clamp(120px, 18vh, 200px) 0 clamp(80px, 12vh, 140px); }
.cc-landing .bento { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 16px; margin-top: 64px; }
.cc-landing .tile { position: relative; display: flex; flex-direction: column; gap: 32px; min-height: 480px; padding: 36px; overflow: hidden; border-radius: 32px; background: var(--grey-100); }
.cc-landing .tile.full { grid-column: span 6; }
.cc-landing .tile.half { grid-column: span 3; }
.cc-landing .tile h3 { font-size: clamp(22px, 2vw, 28px); font-weight: 700; line-height: 1.36; letter-spacing: -.03em; }
.cc-landing .tile-copy p { margin-top: 10px; max-width: 420px; color: var(--grey-600); font-size: 16px; line-height: 1.65; }
.cc-landing .tile > .ui { margin-top: auto; }
.cc-landing .tile.full { display: grid; grid-template-columns: 1fr; gap: 36px; min-height: 0; padding: 44px 44px 48px; }
.cc-landing .tile.full .tile-copy p { max-width: 560px; }
.cc-landing .pair { display: grid; grid-template-columns: 1fr 1fr; gap: 56px; align-items: end; width: min(100%, 900px); margin: 0 auto; padding-left: 20px; }
.cc-landing .tile.half { min-height: 0; padding-bottom: 32px; }
.cc-landing .tile.half > .ui { width: min(100%, 400px); align-self: center; margin-top: auto; margin-bottom: auto; }
.cc-landing .cal-stack { position: relative; width: min(100%, 400px); align-self: center; margin-top: auto; padding-bottom: 44px; }
.cc-landing .cal-stack .due { position: absolute; right: -24px; bottom: 0; width: 88%; box-shadow: 0 24px 48px -14px rgba(25, 31, 40, .24); }
.cc-landing .tile .cal-stack .ui { transform: translateY(40px); transition: transform 1.1s var(--ease) .15s; }
.cc-landing .tile .cal-stack .due { transform: translate(16px, 56px); transition-delay: .4s; }
.cc-landing .tile.is-in .cal-stack .ui { transform: none; }
.cc-landing .ui-stack { position: relative; align-self: end; padding-top: 84px; }
.cc-landing .ui-stack .badge { position: absolute; top: 0; left: -20px; width: 74%; box-shadow: 0 24px 48px -14px rgba(25, 31, 40, .22); }
.cc-landing .tile .ui { transform: translateY(40px); transition: transform 1.1s var(--ease) .15s; }
.cc-landing .tile.is-in .ui { transform: none; }
.cc-landing .tile .ui.badge { transform: translate(-10px, 56px); transition-delay: .35s; }
.cc-landing .tile.is-in .ui.badge { transform: none; }
.cc-landing .dark-sec { padding: clamp(120px, 18vh, 200px) 0; background: #0f1418; color: #fff; overflow: hidden; }
.cc-landing .dark-sec .eyebrow { color: #3ee08f; }
.cc-landing .dark-sec .desc { color: rgba(255, 255, 255, .62); }
.cc-landing .push-stage { position: relative; display: grid; gap: 14px; padding: 24px 0; }
.cc-landing .glow { position: absolute; inset: -20% -10%; background: radial-gradient(50% 50% at 50% 50%, rgba(0, 196, 113, .28), transparent 70%); filter: blur(10px); pointer-events: none; }
.cc-landing .push2 {
    position: relative; display: flex; gap: 14px; align-items: flex-start;
    width: min(calc(100% - var(--i) * 28px), 440px); padding: 16px 18px; border-radius: 24px;
    background: rgba(255, 255, 255, .92); color: var(--grey-900);
    box-shadow: 0 20px 50px rgba(0, 0, 0, .35);
    margin-left: calc(var(--i) * 28px);
    opacity: 0; transform: translateY(-24px) scale(.96);
    transition: opacity .7s var(--ease), transform .9s var(--ease);
    transition-delay: calc(.25s + var(--i) * .22s); }
.cc-landing .push-stage.is-in .push2 { opacity: calc(1 - var(--i) * .18); transform: none; }
.cc-landing .push2 img { flex: none; width: 44px; height: 44px; padding: 5px; border-radius: 12px; background: var(--brand-soft); object-fit: contain; }
.cc-landing .push2 > div { min-width: 0; flex: 1; }
.cc-landing .push2-head { display: flex; justify-content: space-between; color: var(--grey-500); font-size: 13px; }
.cc-landing .push2-head b { color: var(--grey-900); font-weight: 600; }
.cc-landing .push2-title { margin-top: 2px; font-size: 15px; font-weight: 600; }
.cc-landing .push2-text { color: var(--grey-700); font-size: 14px; }
.cc-landing .remind-panel { position: relative; padding: 40px 36px 44px; border-radius: 32px; background: var(--grey-100); overflow: hidden; }
.cc-landing .timeline { position: relative; display: grid; grid-template-columns: repeat(3, 1fr); text-align: center; }
.cc-landing .timeline::before, .cc-landing .timeline::after { content: ""; position: absolute; left: 16.6%; top: 19px; height: 3px; border-radius: 2px; }
.cc-landing .timeline::before { right: 16.6%; background: var(--grey-300); }
.cc-landing .timeline::after { width: calc(66.8% * var(--fill, 0)); background: var(--brand); transition: width .5s var(--ease); }
.cc-landing .timeline li { position: relative; }
.cc-landing .timeline button { display: grid; justify-items: center; gap: 2px; width: 100%; padding: 0; border: 0; background: none; font: inherit; color: inherit; cursor: pointer; }
.cc-landing .timeline .dot { position: relative; z-index: 1; display: grid; width: 40px; height: 40px; place-items: center; margin-bottom: 8px; border-radius: 50%; background: #fff; color: var(--grey-500); box-shadow: inset 0 0 0 3px var(--grey-300); transition: background-color .3s ease, box-shadow .3s ease, color .3s ease, transform .3s ease; }
.cc-landing .timeline .dot svg { width: 20px; height: 20px; }
.cc-landing .timeline .done .dot { color: var(--brand); box-shadow: inset 0 0 0 3px var(--brand); }
.cc-landing .timeline .active .dot { background: var(--brand); color: #fff; box-shadow: 0 0 0 8px rgba(0, 196, 113, .16); transform: scale(1.08); }
.cc-landing .timeline .bell.active .dot svg { animation: cc-bell-ring .7s ease .15s; transform-origin: 50% 10%; }
@keyframes cc-bell-ring { 0%, 100% { transform: rotate(0); } 15% { transform: rotate(18deg); } 30% { transform: rotate(-16deg); } 45% { transform: rotate(12deg); } 60% { transform: rotate(-8deg); } 75% { transform: rotate(4deg); } }
.cc-landing .timeline b { font-size: 16px; font-weight: 700; color: var(--grey-500); transition: color .3s ease; }
.cc-landing .timeline .done b { color: var(--grey-800); }
.cc-landing .timeline .active b { color: var(--brand-strong); }
.cc-landing .timeline small { color: var(--grey-600); font-size: 13px; }
.cc-landing .flow-stage { display: grid; margin-top: 36px; }
.cc-landing .flow-slide { grid-area: 1 / 1; display: grid; align-content: start; justify-items: center; gap: 16px; opacity: 0; transform: translateY(14px); transition: opacity .4s ease, transform .6s var(--ease); pointer-events: none; }
.cc-landing .flow-slide.show { opacity: 1; transform: none; pointer-events: auto; }
.cc-landing .flow-cap { color: var(--grey-800); font-size: 16px; font-weight: 600; text-align: center; }
.cc-landing .flow-slide .ui { width: min(100%, 380px); }
.cc-landing .flow-slide.show .ui { animation: cc-flow-pop .65s cubic-bezier(.34, 1.56, .64, 1); }
@keyframes cc-flow-pop { 0% { opacity: 0; transform: translateY(22px) scale(.94); } 100% { opacity: 1; transform: none; } }
.cc-landing .remind-push { display: flex; gap: 12px; align-items: flex-start; padding: 16px 18px; border-radius: 22px; text-align: left; }
.cc-landing .remind-push img { flex: none; width: 42px; height: 42px; padding: 5px; border-radius: 12px; background: var(--brand-soft); object-fit: contain; }
.cc-landing .remind-push > div { min-width: 0; flex: 1; }
.cc-landing .step { min-height: 420px; }
.cc-landing .step-art { display: flex; flex: 1; align-items: center; justify-content: center; padding: 28px 24px 32px; }
.cc-landing .step-art .ui { width: 100%; }
.cc-landing .step-art .mascot-art { width: 140px; }
@media (max-width: 1080px) {
  .cc-landing .pair { gap: 40px; }
  .cc-landing .chip-1, .cc-landing .chip-3 { right: calc(100% - 24px); }
  .cc-landing .chip-2 { left: calc(100% - 24px); }
  .cc-landing .indicator { display: none; }
}
@media (max-width: 860px) {
  /* 폰에서는 스크롤마다 다시 그려야 하는 흐림 효과·큰 그림자를 가볍게 해요 */
  .cc-landing .header { background: rgba(255, 255, 255, .97); backdrop-filter: none; -webkit-backdrop-filter: none; }
  .cc-landing .phone { box-shadow: 0 0 0 1.5px #a9adb4, 0 0 0 2.5px #d9dce0, 0 18px 36px -18px rgba(25, 31, 40, .28); }
  .cc-landing .hero-bg { background: linear-gradient(180deg, #fff 0%, #f3fbf7 100%); }
  .cc-landing { --gutter: 20px; }
  .cc-landing .nav { display: none; }
  .cc-landing .chip { gap: 8px; padding: 9px 12px 9px 9px; border-radius: 16px; max-width: calc(100vw - 28px); box-shadow: 0 12px 28px rgba(25, 31, 40, .16), 0 0 0 1px rgba(25, 31, 40, .05); backdrop-filter: none; -webkit-backdrop-filter: none; background: #fff; }
  .cc-landing .chip img { width: 30px; height: 30px; border-radius: 9px; padding: 3px; }
  .cc-landing .chip small { font-size: 11px; }
  .cc-landing .chip b { font-size: 13px; }
  .cc-landing .chip-1 { right: auto; left: -14px; top: 22%; }
  .cc-landing .chip-2 { left: auto; right: -14px; top: 41%; }
  .cc-landing .chip-3 { right: auto; left: -14px; top: 60%; }
  .cc-landing .m-br { display: inline; }
  .cc-landing .hero { height: 200vh; }
  .cc-landing .hero-stage { --phone: min(72vw, 300px, calc((100svh - var(--header) - 40px) / 2.14)); }
  .cc-landing .split, .cc-landing .split.reverse { grid-template-columns: 1fr; text-align: center; }
  .cc-landing .split.reverse .copy { order: 0; }
  .cc-landing .facts { justify-items: center; text-align: left; }
  .cc-landing .split { --phone: min(72vw, 300px); }
  .cc-landing .steps { grid-template-columns: 1fr; margin-top: 48px; }
  .cc-landing .bento { grid-template-columns: 1fr; margin-top: 44px; }
  .cc-landing .tile.full, .cc-landing .tile.half { grid-column: auto; }
  .cc-landing .tile, .cc-landing .tile.full { display: flex; min-height: 0; padding: 28px 20px 24px; }
  .cc-landing .pair { grid-template-columns: 1fr; gap: 28px; }
  .cc-landing .ui-stack { padding-top: 64px; margin-left: 20px; }
  .cc-landing .ui-stack .badge { left: -20px; width: 88%; }
  .cc-landing .cal-stack .due { right: -8px; }
  .cc-landing .step { min-height: 0; }
  .cc-landing .push-stage { justify-items: center; }
  .cc-landing .push2 { margin-left: 0; text-align: left; }
  .cc-landing .remind-panel { padding: 28px 16px 28px; }
  .cc-landing .timeline b { font-size: 14px; }
  .cc-landing .timeline small { font-size: 12px; }
  .cc-landing .flow-stage { margin-top: 28px; }
  .cc-landing .flow-cap { font-size: 15px; }
  .cc-landing .hero-actions { flex-direction: column; align-items: stretch; width: min(100%, 360px); margin-left: auto; margin-right: auto; }
  .cc-landing .footer-top { flex-direction: column; align-items: flex-start; }
}
@media (max-width: 360px) {
  .cc-landing .hero-title { font-size: 34px; }
}
@media (prefers-reduced-motion: reduce) {
  html:has(.cc-landing) { scroll-behavior: auto; }
  .cc-landing [data-reveal] { opacity: 1; transform: none; transition: none; }
  .cc-landing .hero-stage.chips-on .chip, .cc-landing .flow-slide .ui, .cc-landing .timeline .dot svg { animation: none !important; }
  .cc-landing .timeline::after, .cc-landing .flow-slide { transition: none; }
  .cc-landing .hero-stage.chips-on .chip { opacity: 1; transform: none; }
}

/* ---------- 첫 화면: 스크롤 연동 애니메이션 ----------
 * 지원하는 브라우저(최신 Safari·Chrome)에서는 브라우저가 스크롤과 같은 박자로 직접 움직여요.
 * JS로 움직이면 폰에서 스크롤보다 한 박자 늦어 버벅여 보여요. 지원하지 않으면 Hero.tsx가 JS로 움직여요.
 * 진행 구간(contain)은 JS의 trackProgress와 같아요: 첫 화면 위끝이 화면 위에 닿을 때 0%, 아래끝이 화면 아래에 닿을 때 100%.
 */
@supports (animation-timeline: view()) {
  .cc-landing .hero { view-timeline-name: --cc-hero; view-timeline-axis: block; }
  .cc-landing .hero-copy, .cc-landing .hero-stage {
    animation-timing-function: linear;
    animation-fill-mode: both;
    animation-duration: auto;
    animation-timeline: --cc-hero;
    animation-range: contain 0% contain 100%;
  }
  .cc-landing .hero-copy { animation-name: cc-hero-copy; }
  .cc-landing .hero-stage { animation-name: cc-hero-stage; }
  @media (max-width: 860px) {
    .cc-landing .hero-copy { animation-name: cc-hero-copy-m; }
    .cc-landing .hero-stage { animation-name: cc-hero-stage-m; }
  }
}
/* 데스크톱: 제목은 22%에 사라지고, 폰은 12%~62%에 올라와요(남은 구간에서 카드가 나와요). */
@keyframes cc-hero-copy {
  0% { opacity: 1; transform: translate3d(0, 0, 0); visibility: visible; }
  22.2% { opacity: 0; transform: translate3d(0, -35.5px, 0); }
  24% { visibility: visible; }
  24.1% { visibility: hidden; }
  100% { opacity: 0; transform: translate3d(0, -160px, 0); visibility: hidden; }
}
@keyframes cc-hero-stage {
  0%, 12% { transform: translate3d(-50%, var(--shift, 0px), 0) scale(.92); }
  62%, 100% { transform: translate3d(-50%, 0, 0) scale(1); }
}
/* 폰: 제목은 50%까지 서서히, 폰은 5%~70%에 고르게 올라오고, 그 뒤 카드가 나와요. */
@keyframes cc-hero-copy-m {
  0% { opacity: 1; transform: translate3d(0, 0, 0); visibility: visible; }
  50% { opacity: 0; transform: translate3d(0, -60px, 0); visibility: visible; }
  50.1% { visibility: hidden; }
  100% { opacity: 0; transform: translate3d(0, -120px, 0); visibility: hidden; }
}
@keyframes cc-hero-stage-m {
  0%, 5% { transform: translate3d(-50%, var(--shift, 0px), 0) scale(.92); }
  70%, 100% { transform: translate3d(-50%, 0, 0) scale(1); }
}
/* 움직임 줄이기: 스크롤 연동 움직임은 끄고, Hero.tsx가 제목·폰을 단계별로(이동 없이) 바꿔요. */
@media (prefers-reduced-motion: reduce) {
  .cc-landing .hero-copy, .cc-landing .hero-stage { animation: none; }
}
/* 높이가 낮은 화면(가로로 눕힌 폰 등): 첫 화면을 고정하지 않고 제목·버튼 아래에 폰을 그대로 둬요. */
@media (max-height: 560px) {
  .cc-landing .hero { height: auto; }
  .cc-landing .hero-sticky { position: relative; height: auto; overflow: visible; padding-bottom: 32px; }
  .cc-landing .hero-copy {
    animation: none; opacity: 1 !important; transform: none !important; visibility: visible !important; }
  .cc-landing .hero-stage {
    --phone: min(260px, 60vw);
    position: relative; left: auto; bottom: auto;
    width: max-content; margin: 48px auto 0;
    animation: none; transform: none !important; }
  .cc-landing .hero-stage .chip { opacity: 1; transform: none; animation: none; }
}

/* ---------- 앱 설치 버튼과 안내 창 (InstallAppButton) ---------- */
.cc-landing .cc-app-button { gap: 8px; border: 0; }
.cc-landing .cc-app-button svg { width: 20px; height: 20px; flex: none; }
.cc-landing .cc-app-button:disabled { cursor: wait; opacity: .6; }
.cc-landing .cc-install-dialog { width: min(calc(100% - 48px), 390px); max-width: none; max-height: calc(100dvh - 32px); padding: 0; overflow: auto; overscroll-behavior: contain; border: 0; color: var(--grey-900); background: transparent; }
.cc-landing .cc-install-dialog::backdrop { background: rgba(16, 32, 25, .42); backdrop-filter: blur(5px); -webkit-backdrop-filter: blur(5px); }
.cc-landing .cc-install-dialog-panel { position: relative; padding: 54px 32px 32px; border-radius: 30px; background: #fff; box-shadow: 0 24px 80px rgba(16, 32, 25, .22); text-align: center; animation: cc-dialog-in .36s var(--ease) both; }
.cc-landing .cc-install-dialog-bunny { display: grid; width: 94px; height: 94px; margin: 0 auto 16px; place-items: center; border-radius: 28px; background: var(--brand-soft); }
.cc-landing .cc-install-dialog-bunny img { width: 72px; height: 78px; object-fit: contain; }
.cc-landing .cc-install-dialog-label { margin: 0 0 8px; color: var(--brand-strong); font-size: 14px; font-weight: 700; }
.cc-landing .cc-install-dialog h2 { font-size: 26px; font-weight: 700; line-height: 1.2; }
.cc-landing .cc-install-dialog-description { margin-top: 12px; color: var(--grey-600); font-size: 15px; line-height: 1.55; }
.cc-landing .cc-install-steps { margin: 20px 0 0; padding-left: 24px; font-size: 15px; line-height: 1.65; text-align: left; list-style: decimal; }
.cc-landing .cc-install-steps li + li { margin-top: 12px; }
.cc-landing .cc-install-dialog button { border: 0; cursor: pointer; }
.cc-landing .cc-install-dialog-confirm { width: 100%; height: 48px; margin-top: 24px; border-radius: 14px; color: #fff; background: var(--brand); font-weight: 700; }
.cc-landing .cc-install-dialog-confirm:hover { background: var(--brand-strong); }
.cc-landing .cc-install-dialog-close { position: absolute; top: 16px; right: 16px; display: grid; width: 44px; height: 44px; place-items: center; border-radius: 50%; color: var(--grey-600); background: var(--grey-100); }
.cc-landing .cc-install-dialog-close svg { width: 20px; height: 20px; }
@keyframes cc-dialog-in { from { opacity: 0; transform: translateY(16px) scale(.97); } to { opacity: 1; transform: none; } }
@media (prefers-reduced-motion: reduce) { .cc-landing .cc-install-dialog-panel { animation: none; } }
`;
