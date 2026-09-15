import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { useFeaturePreview } from '../useFeaturePreview';
import FeatureCarousel from './FeatureCarousel';

const features = {
  '스터디 생성': {
    title: '스터디를 간편하게 만들어요',
    description: '이름과 소개를 입력하고, 스터디를 바로 시작해요.',
  },
  '스터디 초대': {
    title: '링크 하나로, 간편하게 초대해요',
    description: '초대 링크를 복사해 공유해 보세요. 스터디원을 모으는 일도 간편해져요.',
  },
  '과제 생성': {
    title: '과제와 마감, 한눈에 확인해요',
    description: '과제 내용부터 제출 방법, 마감일까지. 같은 목표를 향해 차근차근 나아가요.',
  },
  '공지 생성': {
    title: '중요한 소식, 공지로 전달해요',
    description: '모임 일정과 안내를 공지로 남기고, 누가 확인했는지도 한곳에서 살펴봐요.',
  },
  '리마인드 알림': {
    title: '깜빡하기 전에, 한 번 더 확인해요',
    description: '놓치기 쉬운 과제와 중요한 공지. 리마인드 알림으로 다시 챙겨요.',
  },
  '제출 현황': {
    title: '함께 쌓은 진도, 한눈에 확인해요',
    description: '제출한 사람과 아직 준비 중인 사람까지. 과제 진행 상황을 한눈에 확인해요.',
  },
};

type FeatureLabel = keyof typeof features;

let reducedMotion = false;
let pageHidden = false;
let reportIntersection: (isIntersecting: boolean) => void;
const motionListeners = new Set<() => void>();
const matchMediaDescriptor = Object.getOwnPropertyDescriptor(window, 'matchMedia');
const intersectionObserverDescriptor = Object.getOwnPropertyDescriptor(
  window,
  'IntersectionObserver',
);

beforeEach(() => {
  jest.useFakeTimers();
  reducedMotion = false;
  pageHidden = false;
  motionListeners.clear();
  jest.spyOn(document, 'hidden', 'get').mockImplementation(() => pageHidden);
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: jest.fn((query: string) => ({
      matches: reducedMotion,
      media: query,
      addEventListener: (_event: string, listener: () => void) => motionListeners.add(listener),
      removeEventListener: (_event: string, listener: () => void) =>
        motionListeners.delete(listener),
    })),
  });
  Object.defineProperty(window, 'IntersectionObserver', {
    configurable: true,
    value: jest.fn((callback: IntersectionObserverCallback) => {
      const observer = { observe: jest.fn(), disconnect: jest.fn() };
      reportIntersection = (isIntersecting) => {
        callback(
          [{ isIntersecting } as IntersectionObserverEntry],
          observer as unknown as IntersectionObserver,
        );
      };
      return observer;
    }),
  });
});

afterEach(() => {
  jest.useRealTimers();
});

afterAll(() => {
  for (const [name, descriptor] of [
    ['matchMedia', matchMediaDescriptor],
    ['IntersectionObserver', intersectionObserverDescriptor],
  ] as const) {
    if (descriptor) Object.defineProperty(window, name, descriptor);
    else Reflect.deleteProperty(window, name);
  }
});

function PreviewHarness({ suspended = false }: { suspended?: boolean }) {
  const preview = useFeaturePreview({ suspended });
  return (
    <main>
      <div aria-live={preview.playing ? 'off' : 'polite'}>
        <h1>{preview.feature.title}</h1>
        <p>{preview.feature.description}</p>
      </div>
      <FeatureCarousel preview={preview} />
    </main>
  );
}

function advanceTime(milliseconds: number) {
  act(() => jest.advanceTimersByTime(milliseconds));
}

function expectFeature(label: FeatureLabel) {
  expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(features[label].title);
  expect(screen.getByText(features[label].description)).toBeInTheDocument();
  const showcase = screen.getByRole('region', { name: '총총 핵심 기능 미리보기' });
  expect(within(showcase).getByRole('img', { name: `${label} 예시 화면` })).toBeInTheDocument();
  expect(within(showcase).getAllByRole('button', { current: 'step' })).toEqual([
    within(showcase).getByRole('button', { name: `${label} 보기` }),
  ]);
}

function mousePresence(phone: HTMLElement, present: boolean) {
  const event = new MouseEvent(present ? 'pointerover' : 'pointerout', { bubbles: true });
  Object.assign(event, { pointerType: 'mouse' });
  fireEvent(phone, event);
}

describe('랜딩페이지 기능 자동 전환', () => {
  test('기능 선택 점으로 왼쪽 제목·설명과 핸드폰 화면을 함께 바꾸고 별도 재생·이동 버튼은 표시하지 않는다', () => {
    render(<PreviewHarness />);
    const showcase = screen.getByRole('region', { name: '총총 핵심 기능 미리보기' });

    expect(within(showcase).getAllByRole('button')).toHaveLength(6);
    expect(
      within(showcase).queryByRole('button', { name: /자동 전환|이전 기능|다음 기능/ }),
    ).not.toBeInTheDocument();

    for (const label of Object.keys(features) as FeatureLabel[]) {
      fireEvent.click(screen.getByRole('button', { name: `${label} 보기` }));
      expectFeature(label);
      advanceTime(500);
    }
  });

  test('자동으로 모든 기능과 설명을 순서대로 보여주고 마지막에서 첫 기능으로 돌아온다', () => {
    render(<PreviewHarness />);

    expectFeature('스터디 생성');
    for (const label of [
      '스터디 초대',
      '과제 생성',
      '공지 생성',
      '리마인드 알림',
      '제출 현황',
      '스터디 생성',
    ] as const) {
      advanceTime(3000);
      expectFeature(label);
      advanceTime(500);
    }
  });

  test('리마인드 화면에서만 도착 알림을 표시하고 다시 들어올 때 새 알림으로 재생한다', () => {
    const { container } = render(<PreviewHarness />);
    const incomingBanner = () => container.querySelector('.cc-demo-incoming-banner');

    expect(incomingBanner()).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '리마인드 알림 보기' }));
    const firstNotification = incomingBanner();
    expect(firstNotification).toBeInTheDocument();
    advanceTime(500);

    fireEvent.click(screen.getByRole('button', { name: '제출 현황 보기' }));
    expect(firstNotification).not.toBeInTheDocument();
    expect(incomingBanner()).not.toBeInTheDocument();
    advanceTime(500);

    fireEvent.click(screen.getByRole('button', { name: '리마인드 알림 보기' }));
    const nextNotification = incomingBanner();
    expect(nextNotification).toBeInTheDocument();
    expect(nextNotification).not.toBe(firstNotification);
  });

  test('직접 선택한 화면은 포커스를 유지한 채 전체 표시 시간을 보장하고 자동 전환을 이어간다', () => {
    render(<PreviewHarness />);
    advanceTime(2500);
    const assignmentDot = screen.getByRole('button', { name: '과제 생성 보기' });

    act(() => assignmentDot.focus());
    fireEvent.click(assignmentDot);
    expectFeature('과제 생성');
    expect(assignmentDot).toHaveFocus();
    advanceTime(340);
    advanceTime(2999);
    expectFeature('과제 생성');
    advanceTime(1);
    expectFeature('공지 생성');
  });

  test('선택 점에서 방향키를 연속으로 누르면 기능을 계속 탐색하고 Home·End와 양 끝 순환을 지원한다', () => {
    render(<PreviewHarness />);
    const initialDot = screen.getByRole('button', { name: '스터디 생성 보기' });
    act(() => initialDot.focus());

    for (const [key, label] of [
      ['ArrowRight', '스터디 초대'],
      ['ArrowRight', '과제 생성'],
      ['End', '제출 현황'],
      ['ArrowRight', '스터디 생성'],
      ['ArrowLeft', '제출 현황'],
      ['Home', '스터디 생성'],
    ] as const) {
      fireEvent.keyDown(document.activeElement ?? initialDot, { key });
      expectFeature(label);
      advanceTime(500);
    }
  });

  test('숨겨진 탭과 화면 밖에서는 자동 전환을 멈추고 다시 보이면 전체 간격 후 재개한다', () => {
    render(<PreviewHarness />);
    advanceTime(2500);

    pageHidden = true;
    fireEvent(document, new Event('visibilitychange'));
    advanceTime(12_000);
    expectFeature('스터디 생성');

    pageHidden = false;
    fireEvent(document, new Event('visibilitychange'));
    act(() => reportIntersection(false));
    advanceTime(12_000);
    expectFeature('스터디 생성');

    act(() => reportIntersection(true));
    advanceTime(2999);
    expectFeature('스터디 생성');
    advanceTime(1);
    expectFeature('스터디 초대');
  });

  test('스토어 안내 등으로 중단된 동안 화면을 유지하고 중단 해제 후 자동 전환한다', () => {
    const { rerender } = render(<PreviewHarness />);
    advanceTime(2500);

    rerender(<PreviewHarness suspended />);
    advanceTime(12_000);
    expectFeature('스터디 생성');

    rerender(<PreviewHarness />);
    advanceTime(2999);
    expectFeature('스터디 생성');
    advanceTime(1);
    expectFeature('스터디 초대');
  });

  test('핸드폰에 마우스를 올리거나 선택 점에 포커스가 머무는 동안 자동 전환을 잠시 멈춘다', () => {
    render(<PreviewHarness />);
    const phone = screen.getByRole('img', { name: '스터디 생성 예시 화면' });

    mousePresence(phone, true);
    advanceTime(12_000);
    expectFeature('스터디 생성');

    mousePresence(phone, false);
    const dot = screen.getByRole('button', { name: '스터디 생성 보기' });
    act(() => dot.focus());
    advanceTime(12_000);
    expectFeature('스터디 생성');

    act(() => dot.blur());
    advanceTime(3000);
    expectFeature('스터디 초대');
  });

  test('동작 줄이기 설정에서는 직접 선택만 허용하고 설정이 해제되면 자동 전환을 재개한다', () => {
    reducedMotion = true;
    render(<PreviewHarness />);

    advanceTime(30_000);
    expectFeature('스터디 생성');
    fireEvent.click(screen.getByRole('button', { name: '리마인드 알림 보기' }));
    expectFeature('리마인드 알림');
    fireEvent.click(screen.getByRole('button', { name: '제출 현황 보기' }));
    expectFeature('제출 현황');
    advanceTime(30_000);
    expectFeature('제출 현황');

    act(() => {
      reducedMotion = false;
      motionListeners.forEach((listener) => listener());
    });
    advanceTime(3000);
    expectFeature('스터디 생성');
  });
});
