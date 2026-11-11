import { useEffect, useRef, useSyncExternalStore, type RefObject } from 'react';

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)';

function subscribeReducedMotion(onChange: () => void) {
  const media = window.matchMedia(REDUCED_MOTION_QUERY);
  media.addEventListener?.('change', onChange);
  return () => media.removeEventListener?.('change', onChange);
}

export function usePrefersReducedMotion() {
  return useSyncExternalStore(
    subscribeReducedMotion,
    () => window.matchMedia(REDUCED_MOTION_QUERY).matches,
    () => true,
  );
}

export const clamp01 = (value: number) => Math.min(1, Math.max(0, value));

/**
 * 화면 높이. 폰 브라우저는 스크롤하면 주소창이 줄어들며 innerHeight가 바뀌는데,
 * 그때마다 진행률·폰 위치가 튀지 않도록 폭이 바뀔 때(회전 등)만 새로 재요.
 */
let stableHeight = 0;
let stableWidth = 0;
export function stableViewportHeight() {
  if (!stableHeight || window.innerWidth !== stableWidth) {
    stableWidth = window.innerWidth;
    stableHeight = window.innerHeight;
  }
  return stableHeight;
}

/** 긴 구간(sticky 트랙) 안에서의 진행률. 위끝이 화면 위에 닿으면 0, 아래끝이 화면 아래에 닿으면 1. */
export function trackProgress(element: HTMLElement) {
  const rect = element.getBoundingClientRect();
  const scrollable = rect.height - stableViewportHeight();
  return scrollable > 0 ? clamp01(-rect.top / scrollable) : 0;
}

type FrameTask = { read: () => unknown; write: (measured: never) => void };

/**
 * 랜딩의 모든 스크롤 작업을 한 프레임에 모아 처리해요.
 * 먼저 모든 작업의 위치를 재고(read), 그다음 한꺼번에 화면을 바꿔요(write).
 * 재기와 바꾸기가 섞이면 브라우저가 한 프레임에 레이아웃을 여러 번 계산해서 폰에서 버벅여요.
 */
const frameTasks = new Set<{ current: FrameTask }>();
let frameId = 0;

function runFrame() {
  frameId = 0;
  const tasks = Array.from(frameTasks);
  const measured = tasks.map((task) => task.current.read());
  tasks.forEach((task, index) => task.current.write(measured[index] as never));
}

function scheduleFrame() {
  if (!frameId) frameId = requestAnimationFrame(runFrame);
}

/** 스크롤·리사이즈 때 프레임당 한 번, read로 잰 값을 write에 넘겨 불러요. 처음에도 한 번 불러요. */
export function useScrollFrame<T>(read: () => T, write: (measured: T) => void) {
  const taskRef = useRef<FrameTask>({ read, write: write as FrameTask['write'] });
  useEffect(() => {
    taskRef.current = { read, write: write as FrameTask['write'] };
  });

  useEffect(() => {
    const task = taskRef;
    if (frameTasks.size === 0) {
      window.addEventListener('scroll', scheduleFrame, { passive: true });
      window.addEventListener('resize', scheduleFrame);
    }
    frameTasks.add(task);
    scheduleFrame();
    return () => {
      frameTasks.delete(task);
      if (frameTasks.size === 0) {
        window.removeEventListener('scroll', scheduleFrame);
        window.removeEventListener('resize', scheduleFrame);
        if (frameId) cancelAnimationFrame(frameId);
        frameId = 0;
      }
    };
  }, []);
}

/**
 * `[data-reveal]` 요소가 화면에 들어오면 `is-in` 클래스를 붙여요.
 * 현황 타일(`.tile`)은 화면을 벗어나면 클래스를 떼서, 다시 들어올 때마다 애니메이션이 재생돼요.
 */
export function useReveal(rootRef: RefObject<HTMLElement | null>, reducedMotion: boolean) {
  useEffect(() => {
    const root = rootRef.current;
    if (!root) return;
    const targets = Array.from(root.querySelectorAll<HTMLElement>('[data-reveal]'));

    if (reducedMotion || typeof IntersectionObserver === 'undefined') {
      targets.forEach((target) => target.classList.add('is-in'));
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const replay = entry.target.matches('.tile');
          if (entry.isIntersecting) {
            entry.target.classList.add('is-in');
            if (!replay) observer.unobserve(entry.target);
          } else if (replay) {
            entry.target.classList.remove('is-in');
          }
        });
      },
      { threshold: 0.2 },
    );
    targets.forEach((target) => observer.observe(target));
    return () => observer.disconnect();
  }, [rootRef, reducedMotion]);
}
