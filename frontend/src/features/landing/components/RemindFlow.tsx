import { useEffect, useRef, useState, type CSSProperties } from 'react';
import chongchongLogo from '../assets/chongchong-logo.png';
import assignDone from '../assets/parts/assign-done.webp';
import remindField from '../assets/parts/remind-field.webp';
import { usePrefersReducedMotion } from '../useLandingMotion';

const STEP_MS = 1500;
const LAST_STEP_EXTRA_MS = 500;
const RESUME_AFTER_CLICK_MS = STEP_MS + 1500;
const FILL = [0, 0.5, 1];

const steps = [
  {
    title: '과제 등록',
    sub: '리마인드 시각 설정',
    icon: 'M9 4h6v3H9zM8 5.5H6.5A1.5 1.5 0 0 0 5 7v12.5A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5-1.5V7a1.5 1.5 0 0 0-1.5-1.5H16M12 11v6M9 14h6',
    caption: '과제를 올리면서 리마인드 시각을 정해요',
  },
  {
    title: '리마인드',
    sub: '미제출자에게만 알림',
    icon: 'M6 10a6 6 0 1 1 12 0v4l1.5 3h-15L6 14v-4ZM10 20a2 2 0 0 0 4 0',
    caption: '그 시각이 되면 아직 안 낸 사람에게만 알림이 가요',
  },
  {
    title: '마감',
    sub: '제출 현황 확인',
    icon: 'm6 12.5 4 4 8-9',
    caption: '마감 후에는 누가 냈는지 바로 확인해요',
  },
];

function StepVisual({ step }: { step: number }) {
  if (step === 0) {
    return (
      <div className="ui">
        <img
          src={remindField}
          alt="리마인드 시각 입력란: 2026년 8월 5일 18:00, 리마인드 등록하기"
          width={780}
          height={296}
        />
      </div>
    );
  }
  if (step === 1) {
    return (
      <div
        className="ui remind-push"
        role="img"
        aria-label="미제출자가 받는 알림: [알고리즘 스터디] 과제 리마인드, 이번주 그리디 3문제 풀이"
      >
        <img src={chongchongLogo} alt="" />
        <div>
          <div className="push2-head">
            <b>총총</b>
            <span>18:00</span>
          </div>
          <p className="push2-title">[알고리즘 스터디] 과제 리마인드</p>
          <p className="push2-text">이번주 그리디 3문제 풀이</p>
        </div>
      </div>
    );
  }
  return (
    <div className="ui">
      <img src={assignDone} alt="제출 2명, 지각 1명 목록" width={780} height={436} />
    </div>
  );
}

/** 과제 등록 → 리마인드 → 마감을 화면에 보이는 동안 자동으로 반복해요. 단계를 누르면 그 단계로 넘어가요. */
export default function RemindFlow() {
  const reducedMotion = usePrefersReducedMotion();
  const panelRef = useRef<HTMLDivElement>(null);
  const [step, setStep] = useState(0);
  const [visible, setVisible] = useState(false);
  const [pausedUntil, setPausedUntil] = useState(0);

  useEffect(() => {
    const panel = panelRef.current;
    if (!panel || typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver(([entry]) => setVisible(entry.isIntersecting), {
      threshold: 0.45,
    });
    observer.observe(panel);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (reducedMotion || !visible) return;
    const base = step === 2 ? STEP_MS + LAST_STEP_EXTRA_MS : STEP_MS;
    const delay = Math.max(base, pausedUntil - Date.now());
    const timer = window.setTimeout(
      () => setStep((current) => (current + 1) % steps.length),
      delay,
    );
    return () => window.clearTimeout(timer);
  }, [step, visible, reducedMotion, pausedUntil]);

  const current = reducedMotion ? 1 : step;

  return (
    <div
      ref={panelRef}
      className="remind-panel"
      data-reveal
      style={{ '--d': '.1s' } as CSSProperties}
    >
      <ol
        className="timeline"
        aria-label="리마인드 3단계 흐름"
        style={{ '--fill': FILL[current] } as CSSProperties}
      >
        {steps.map((item, index) => (
          <li
            key={item.title}
            className={[
              index === 1 ? 'bell' : '',
              index === current ? 'active' : '',
              index < current ? 'done' : '',
            ]
              .filter(Boolean)
              .join(' ')}
          >
            <button
              type="button"
              aria-current={index === current ? 'step' : undefined}
              onClick={() => {
                setStep(index);
                setPausedUntil(Date.now() + RESUME_AFTER_CLICK_MS);
              }}
            >
              <span className="dot">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d={item.icon}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth={index === 2 ? 2.4 : 2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </span>
              <b>{item.title}</b>
              <small>{item.sub}</small>
            </button>
          </li>
        ))}
      </ol>
      <div className="flow-stage" aria-live="polite">
        {steps.map((item, index) => (
          <div
            key={item.title}
            className={'flow-slide' + (index === current ? ' show' : '')}
            aria-hidden={index !== current}
          >
            <p className="flow-cap">{item.caption}</p>
            <StepVisual step={index} />
          </div>
        ))}
      </div>
    </div>
  );
}
