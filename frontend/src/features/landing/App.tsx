import { useEffect, useRef } from 'react';
import {
  FinalSection,
  LandingFooter,
  MemberSection,
  NotifySection,
  OverviewSection,
  RemindSection,
  StartSection,
} from './components/FeatureSections';
import Hero from './components/Hero';
import LandingHeader from './components/LandingHeader';
import Statement from './components/Statement';
import { LANDING_STYLES } from './landing.styles';
import { useKeepScrollOnReload, usePrefersReducedMotion, useReveal } from './useLandingMotion';

// BrowserRouter / RouterProvider는 기존 프로젝트의 설정을 그대로 사용합니다.
export default function App() {
  const rootRef = useRef<HTMLDivElement>(null);
  const reducedMotion = usePrefersReducedMotion();
  useReveal(rootRef, reducedMotion);
  useKeepScrollOnReload();

  useEffect(() => {
    const previousTitle = document.title;
    document.title = '총총 — 스터디 운영을 더 가볍게';
    return () => {
      document.title = previousTitle;
    };
  }, []);

  return (
    <div ref={rootRef} className="cc-landing" lang="ko">
      <style>{LANDING_STYLES}</style>
      <LandingHeader />
      <main>
        <Hero />
        <Statement />
        <OverviewSection />
        <NotifySection />
        <RemindSection />
        <MemberSection />
        <StartSection />
        <FinalSection />
      </main>
      <LandingFooter />
    </div>
  );
}
