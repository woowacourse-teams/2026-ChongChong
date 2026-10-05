export type PwaPlatform = 'ios-safari' | 'ios-other' | 'android' | 'desktop';
export type PwaGuideIcon = 'menu' | 'share' | 'add' | 'app' | 'browser';

interface InstallStep {
  icon: PwaGuideIcon;
  title: string;
  description: string;
}

export function getPwaPlatform(): PwaPlatform {
  const userAgent = navigator.userAgent;
  const isIos =
    /iPad|iPhone|iPod/i.test(userAgent) ||
    (/Macintosh/i.test(userAgent) && navigator.maxTouchPoints > 1);
  if (isIos) {
    const isSafari =
      /Version\/[\d.]+.*Safari/i.test(userAgent) &&
      !/CriOS|FxiOS|EdgiOS|OPiOS|DuckDuckGo|GSA|KAKAOTALK|NAVER|Daum|Instagram|FBAN|FBAV|Line\/|LinkedInApp|Twitter/i.test(
        userAgent,
      );
    return isSafari ? 'ios-safari' : 'ios-other';
  }
  return /Android/i.test(userAgent) ? 'android' : 'desktop';
}

export function getSafariInstallUrl() {
  return new URL('/studies', window.location.origin).href;
}

const safariSteps: InstallStep[] = [
  {
    icon: 'share',
    title: '공유 메뉴 열기',
    description: 'Safari에서 공유 버튼 또는 더 보기(···) → 공유를 누르세요.',
  },
  {
    icon: 'add',
    title: '홈 화면에 추가',
    description: '공유 목록에서 선택하세요. 보이지 않으면 ‘동작 편집’을 확인해 주세요.',
  },
  {
    icon: 'app',
    title: '총총 아이콘으로 열기',
    description: '‘웹 앱으로 열기’가 보이면 켜고 추가한 뒤, 홈 화면의 총총을 여세요.',
  },
];

const guides: Record<
  PwaPlatform,
  {
    actionLabel: string;
    title: string;
    description: string;
    steps: InstallStep[];
  }
> = {
  'ios-safari': {
    actionLabel: '다음으로',
    title: 'Safari 설치 안내',
    description: 'iPhone은 Safari에서 홈 화면에 추가하면 알림도 받을 수 있어요.',
    steps: safariSteps,
  },
  'ios-other': {
    actionLabel: '다음으로',
    title: 'Safari에서 이어서 설치하기',
    description: 'Safari에서 총총을 열고 홈 화면에 추가해 주세요.',
    steps: safariSteps,
  },
  android: {
    actionLabel: '앱으로 시작하기',
    title: 'Android 설치 안내',
    description: 'Android에서는 아래 버튼으로 설치를 시작할 수 있어요.',
    steps: [
      {
        icon: 'add',
        title: '앱으로 시작하기 선택',
        description:
          '설치 창이 없으면 Chrome 메뉴(⋮)의 ‘앱 설치’ 또는 ‘홈 화면에 추가’를 누르세요.',
      },
      {
        icon: 'app',
        title: '총총 앱 열기',
        description: '홈 화면의 총총 아이콘을 눌러 앱을 열어 주세요.',
      },
    ],
  },
  desktop: {
    actionLabel: '앱으로 시작하기',
    title: '브라우저 설치 안내',
    description: 'PC에서는 앱을 설치하거나 브라우저에서 그대로 이용할 수 있어요.',
    steps: [
      {
        icon: 'add',
        title: 'Chrome·Edge에서 설치',
        description: '아래 버튼이나 주소창 설치 아이콘, 메뉴의 ‘앱 설치’를 누르세요.',
      },
      {
        icon: 'app',
        title: '총총 아이콘으로 열기',
        description: '설치 후 총총 아이콘을 눌러 시작하세요.',
      },
    ],
  },
};

export function getPwaInstallGuide(platform: PwaPlatform) {
  return guides[platform];
}
