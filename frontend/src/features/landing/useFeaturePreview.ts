import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { previewFeatures } from './previewFeatures';

const SLIDE_INTERVAL = 3000;
export const TRANSITION_DURATION = 280;
const total = previewFeatures.length;
const wrapIndex = (index: number) => (index + total) % total;

function subscribeToMotion(onChange: () => void) {
  const media = window.matchMedia('(prefers-reduced-motion: reduce)');
  media.addEventListener('change', onChange);
  return () => media.removeEventListener('change', onChange);
}

function subscribeToVisibility(onChange: () => void) {
  document.addEventListener('visibilitychange', onChange);
  return () => document.removeEventListener('visibilitychange', onChange);
}

const getReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const getVisibility = () => !document.hidden;

export function useFeaturePreview({ suspended = false } = {}) {
  const [slide, setSlide] = useState({ position: 1, moving: false });
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [inView, setInView] = useState(true);
  const showcaseRef = useRef<HTMLElement>(null);
  const reducedMotion = useSyncExternalStore(subscribeToMotion, getReducedMotion, () => true);
  const visible = useSyncExternalStore(subscribeToVisibility, getVisibility, () => false);
  const activeIndex = wrapIndex(slide.position - 1);
  const playing = !reducedMotion && visible && inView && !hovered && !focused && !suspended;

  useEffect(() => {
    const element = showcaseRef.current;
    if (!element || typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver(([entry]) => setInView(entry.isIntersecting), {
      threshold: 0.25,
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!playing || slide.moving) return;
    const timer = window.setTimeout(() => {
      setSlide((current) => ({ position: current.position + 1, moving: true }));
    }, SLIDE_INTERVAL);
    return () => window.clearTimeout(timer);
  }, [playing, slide]);

  useEffect(() => {
    if (!slide.moving) return;
    // A cloned edge screen keeps the loop seamless. Settle even if transitionend
    // is skipped by a background tab or a change to reduced motion.
    const timer = window.setTimeout(
      () => {
        setSlide((current) => ({ position: wrapIndex(current.position - 1) + 1, moving: false }));
      },
      reducedMotion ? 0 : TRANSITION_DURATION + 60,
    );
    return () => window.clearTimeout(timer);
  }, [slide.moving, reducedMotion]);

  function selectFeature(index: number) {
    if (slide.moving) return;
    const position = wrapIndex(index) + 1;
    setSlide({ position, moving: !reducedMotion && position !== slide.position });
    // Explicit selection restarts a full interval without discarding DOM focus.
    setFocused(false);
  }

  function settleTransition() {
    setSlide((current) => ({ position: wrapIndex(current.position - 1) + 1, moving: false }));
  }

  return {
    showcaseRef,
    activeIndex,
    feature: previewFeatures[activeIndex],
    slide,
    reducedMotion,
    playing,
    selectFeature,
    settleTransition,
    setHovered,
    setFocused,
  };
}

export type FeaturePreview = ReturnType<typeof useFeaturePreview>;
