import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../../../config';
import { server } from '../../../../../mocks/msw-node';

export const PROFILE_PREVIEW_URL = 'blob:http://localhost/study-profile';

export function mockImageUpload() {
  server.use(
    http.post(`${API_URL}/files/presigned-url`, () =>
      HttpResponse.json({
        fileId: 100,
        uploadUrl: 'https://storage.example.com/study-profile',
        expiresAt: '2099-01-01T00:00:00Z',
      }),
    ),
    http.put('https://storage.example.com/study-profile', () => new HttpResponse(null)),
    http.post(`${API_URL}/files/100/complete`, () => new HttpResponse(null, { status: 204 })),
  );
}

// jsdom에는 이미지 디코딩과 Blob URL 생성 기능이 없어 테스트에서만 대체합니다.
export function setupImageBrowserMocks() {
  const methods = [
    { target: URL, key: 'createObjectURL', value: () => PROFILE_PREVIEW_URL },
    { target: URL, key: 'revokeObjectURL', value: () => {} },
    { target: HTMLImageElement.prototype, key: 'decode', value: () => Promise.resolve() },
  ];
  const descriptors = methods.map(({ target, key }) =>
    Object.getOwnPropertyDescriptor(target, key),
  );

  beforeEach(() => {
    methods.forEach(({ target, key, value }) => {
      Object.defineProperty(target, key, { configurable: true, writable: true, value });
    });
  });

  afterEach(() => {
    methods.forEach(({ target, key }, index) => {
      const descriptor = descriptors[index];
      if (descriptor) Object.defineProperty(target, key, descriptor);
      else Reflect.deleteProperty(target, key);
    });
  });
}
