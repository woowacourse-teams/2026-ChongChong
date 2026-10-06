import { getPwaPlatform } from './pwaInstallGuide';

const safari =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) Version/26.0 Mobile/15E148 Safari/604.1';
const descriptors = ['userAgent', 'maxTouchPoints'].map((key) => ({
  key,
  descriptor: Object.getOwnPropertyDescriptor(navigator, key),
}));

afterEach(() => {
  for (const { key, descriptor } of descriptors) {
    if (descriptor) Object.defineProperty(navigator, key, descriptor);
    else Reflect.deleteProperty(navigator, key);
  }
});

test.each([
  [safari, 0, 'ios-safari'],
  [safari.replace('Version/26.0', 'CriOS/140.0'), 0, 'ios-other'],
  [safari + ' KAKAOTALK/26.0', 0, 'ios-other'],
  ['Mozilla/5.0 (iPhone) AppleWebKit/605.1.15 Mobile/15E148', 0, 'ios-other'],
  ['Mozilla/5.0 (Macintosh) Version/26.0 Safari/605.1.15', 5, 'ios-safari'],
  ['Mozilla/5.0 (Macintosh) Version/26.0 Safari/605.1.15', 0, 'desktop'],
  [
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36',
    0,
    'desktop',
  ],
  [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36',
    10,
    'desktop',
  ],
  ['Mozilla/5.0 (Linux; Android 16) Chrome/140.0 Mobile Safari/537.36', 5, 'android'],
])(
  '브라우저 UA와 터치 지원으로 설치 흐름을 구분한다: %s',
  (userAgent, maxTouchPoints, platform) => {
    Object.defineProperty(navigator, 'userAgent', { configurable: true, value: userAgent });
    Object.defineProperty(navigator, 'maxTouchPoints', {
      configurable: true,
      value: maxTouchPoints,
    });
    expect(getPwaPlatform()).toBe(platform);
  },
);
