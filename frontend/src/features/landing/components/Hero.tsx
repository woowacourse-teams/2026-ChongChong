import { useEffect, useRef, useState } from 'react';
import chongchongLogo from '../assets/chongchong-logo.png';
import checkMascot from '../assets/mascots/check-icon.webp';
import megaphoneMascot from '../assets/mascots/footer-icon.webp';
import homeLeaderScreen from '../assets/screens/home-leader.webp';
import {
  clamp01,
  stableViewportHeight,
  trackProgress,
  usePrefersReducedMotion,
  useScrollFrame,
} from '../useLandingMotion';
import InstallAppButton from './InstallAppButton';
import Phone from './Phone';
import WebStartLink from './WebStartLink';

const noop = () => {};

/**
 * 진행률에 따른 제목·폰 움직임.
 * 데스크톱: 폰이 0.62에 다 올라오고, 0.55를 넘으면 카드가 0.3초 간격으로 연달아 나와요.
 * 폰: 제목은 천천히 사라지고 폰은 0.05~0.7에 고르게 올라온 뒤, 0.72를 넘으면 카드가 폰 가장자리에 겹쳐 나와요.
 * 위로 되돌아가 chipsHide 아래로 내려가면 카드는 다시 숨어요.
 */
const MOTION = {
  desktop: {
    fade: 4.5,
    lift: 160,
    hideAt: 0.24,
    riseStart: 0.12,
    riseSpeed: 2,
    chipsShow: 0.55,
    chipsHide: 0.3,
  },
  mobile: {
    fade: 2,
    lift: 120,
    hideAt: 0.5,
    riseStart: 0.05,
    riseSpeed: 1.54,
    chipsShow: 0.72,
    chipsHide: 0.45,
  },
};
const MOBILE_QUERY = '(max-width: 860px)';

/** 브라우저가 스크롤 연동 애니메이션을 지원하면 CSS(landing.styles.ts)가 움직이고, JS는 시작 위치만 알려줘요. */
const supportsScrollTimeline = () =>
  typeof CSS !== 'undefined' &&
  typeof CSS.supports === 'function' &&
  CSS.supports('animation-timeline: view()');

const chips = [
  {
    icon: chongchongLogo,
    caption: '8월 스터디 운영 방식이 바뀝니다',
    count: '2/4',
    label: ' 읽음',
  },
  { icon: checkMascot, caption: '리액트 렌더링 최적화 정리', count: '1/4', label: ' 제출' },
  {
    icon: megaphoneMascot,
    caption: '바니님, 오늘도 화이팅!',
    label: '리마인드는 총총이 대신 보낼게요',
  },
];

export default function Hero() {
  const trackRef = useRef<HTMLElement>(null);
  const copyRef = useRef<HTMLDivElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const shiftRef = useRef(0);
  const cssDrivenRef = useRef(false);
  const [chipsOn, setChipsOn] = useState(false);
  const reducedMotion = usePrefersReducedMotion();

  // 폰이 처음엔 버튼 아래에서 시작하도록, 화면 높이에 맞춰 밀어둘 거리를 계산해요.
  useEffect(() => {
    const layout = () => {
      const copy = copyRef.current;
      const stage = stageRef.current;
      if (!copy || !stage) return;
      const naturalTop = stableViewportHeight() - stage.offsetHeight;
      const shift = Math.max(0, copy.offsetTop + copy.offsetHeight + 48 - naturalTop);
      shiftRef.current = shift;
      stage.style.setProperty('--shift', `${shift}px`);
    };
    // 주소창이 줄어드는 것처럼 높이만 바뀔 때는 다시 재지 않아요(폰이 튀는 원인).
    let lastWidth = window.innerWidth;
    const onResize = () => {
      if (window.innerWidth === lastWidth) return;
      lastWidth = window.innerWidth;
      layout();
    };
    cssDrivenRef.current = supportsScrollTimeline();
    layout();
    window.addEventListener('resize', onResize);
    window.addEventListener('load', layout);
    return () => {
      window.removeEventListener('resize', onResize);
      window.removeEventListener('load', layout);
    };
  }, []);

  // 카드 등장은 항상 JS가, 폰·제목 움직임은 CSS가 못 할 때만 JS가 맡아요(transform·opacity만 바꿔요).
  useScrollFrame(
    () => ({
      progress: trackRef.current ? trackProgress(trackRef.current) : 0,
      motion: window.matchMedia(MOBILE_QUERY).matches ? MOTION.mobile : MOTION.desktop,
    }),
    ({ progress, motion }) => {
      if (progress > motion.chipsShow) setChipsOn(true);
      else if (progress < motion.chipsHide) setChipsOn(false);

      const copy = copyRef.current;
      const stage = stageRef.current;
      if (reducedMotion) {
        // 움직임 줄이기: 이동·확대 없이 폰이 다 올라온 위치를 기준으로 두 상태만 바꿔요.
        const raised = progress > motion.riseStart + 0.5 / motion.riseSpeed;
        if (copy) {
          copy.style.opacity = raised ? '0' : '1';
          copy.style.transform = 'none';
          copy.style.visibility = raised ? 'hidden' : '';
        }
        if (stage) {
          stage.style.transform = `translate3d(-50%, ${raised ? 0 : shiftRef.current}px, 0)`;
        }
        return;
      }
      if (cssDrivenRef.current) return;

      // 스크롤 연동 애니메이션을 지원하지 않는 브라우저에서만 JS로 움직여요.
      if (copy) {
        copy.style.opacity = String(clamp01(1 - progress * motion.fade));
        copy.style.transform = `translate3d(0, ${(-motion.lift * progress).toFixed(1)}px, 0)`;
        copy.style.visibility = progress > motion.hideAt ? 'hidden' : '';
      }
      if (stage) {
        const rise = clamp01((progress - motion.riseStart) * motion.riseSpeed);
        const offset = (1 - rise) * shiftRef.current;
        stage.style.transform = `translate3d(-50%, ${offset.toFixed(1)}px, 0) scale(${(0.92 + rise * 0.08).toFixed(4)})`;
      }
    },
  );

  return (
    <section ref={trackRef} className="hero" id="top" data-section aria-labelledby="cc-hero-title">
      <div className="hero-sticky">
        <div className="hero-bg" />
        <div ref={copyRef} className="container hero-copy">
          <h1 id="cc-hero-title" className="hero-title">
            공지 확인부터
            <br className="m-br" /> 과제 제출까지
            <br />
            마침내 <span className="accent">총총</span> 하나로
          </h1>
          <p className="hero-sub">
            누가 읽었는지, 누가 냈는지 세지 마세요.
            <br />
            스터디 리드의 일은 총총이 대신 챙길게요.
          </p>
          <div className="hero-actions">
            <WebStartLink className="btn btn-primary" position="hero" />
            <InstallAppButton onActiveChange={noop} />
          </div>
        </div>
        <div ref={stageRef} className={'hero-stage' + (chipsOn ? ' chips-on' : '')}>
          {chips.map((chip, index) => (
            <div key={chip.caption} className={`chip chip-${index + 1}`} aria-hidden="true">
              <img src={chip.icon} alt="" />
              <div>
                <small>{chip.caption}</small>
                <b>
                  {chip.count && <span className="num">{chip.count}</span>}
                  {chip.label}
                </b>
              </div>
            </div>
          ))}
          <Phone
            src={homeLeaderScreen}
            label="스터디 홈 화면. 안내 중인 공지 2개, 진행 중인 과제 1개와 공지별 읽음 수, 과제별 제출 수가 보입니다."
          />
        </div>
      </div>
    </section>
  );
}
