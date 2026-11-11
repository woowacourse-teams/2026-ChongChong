import { useState } from 'react';
import wordmark from '../assets/wordmark.webp';
import { useScrollFrame } from '../useLandingMotion';
import WebStartLink from './WebStartLink';

const SECTIONS = [
  { id: 'top', label: '처음' },
  { id: 'dashboard', label: '대시보드' },
  { id: 'notify', label: '제출 알림' },
  { id: 'remind', label: '리마인드' },
  { id: 'member', label: '스터디원 화면' },
  { id: 'start', label: '시작하기' },
] as const;

export default function LandingHeader() {
  const [scrolled, setScrolled] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const [indicatorVisible, setIndicatorVisible] = useState(false);

  useScrollFrame(
    () => {
      const middle = window.innerHeight / 2;
      let current = 0;
      SECTIONS.forEach(({ id }, index) => {
        const section = document.getElementById(id);
        if (section && section.getBoundingClientRect().top <= middle) current = index;
      });
      return { scrollY: window.scrollY, viewport: window.innerHeight, current };
    },
    ({ scrollY, viewport, current }) => {
      setScrolled(scrollY > 4);
      setIndicatorVisible(scrollY > viewport * 0.5);
      setActiveIndex(current);
    },
  );

  return (
    <>
      <header className={'header' + (scrolled ? ' is-scrolled' : '')}>
        <div className="container header-inner">
          <a className="brand" href="#top" aria-label="총총 처음으로">
            <img src={wordmark} alt="총총" width={200} height={100} />
          </a>
          <nav className="nav" aria-label="섹션 바로가기">
            {SECTIONS.slice(1).map(({ id, label }) => (
              <a key={id} href={`#${id}`}>
                {label}
              </a>
            ))}
          </nav>
          <WebStartLink className="header-cta" position="header" />
        </div>
      </header>

      <nav className={'indicator' + (indicatorVisible ? ' is-visible' : '')} aria-hidden="true">
        {SECTIONS.map(({ id }, index) => (
          <a
            key={id}
            href={`#${id}`}
            tabIndex={-1}
            className={index === activeIndex ? 'is-active' : undefined}
          />
        ))}
      </nav>
    </>
  );
}
