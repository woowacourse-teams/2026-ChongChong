import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../config';
import { server } from '../../mocks/msw-node';
import { userTable } from '../../features/user/mocks/db';
import { login, logout } from '../../test/render';
import { uploadAttachment } from './api';
import { MOCK_UPLOAD_ORIGIN } from './mocks/db';

const requests: Request[] = [];
const captureRequest = ({ request }: { request: Request }) => {
  requests.push(request.clone());
};

beforeEach(async () => {
  requests.length = 0;
  await userTable.create({ id: 1, name: '업로더', profileImage: null });
  login('업로더');
  server.events.on('request:start', captureRequest);
});

afterEach(() => {
  server.events.removeListener('request:start', captureRequest);
  logout();
});

test('앱 인증을 S3에 전달하지 않고 파일 바이트와 서명 헤더를 전송한 뒤 완료한다', async () => {
  const id = await uploadAttachment(new File(['image-bytes'], 'image.png', { type: 'image/png' }));

  expect(requests.map((request) => [request.method, new URL(request.url).pathname])).toEqual([
    ['POST', '/api/files/presigned-url'],
    ['PUT', `/files/${id}`],
    ['POST', `/api/files/${id}/complete`],
  ]);
  expect(await requests[0].json()).toEqual({
    originalFileName: 'image.png',
    contentType: 'image/png',
    size: 11,
  });
  expect(requests[0].headers.get('Authorization')).toBe('Bearer 1');
  expect(requests[1].headers.get('Authorization')).toBeNull();
  expect(requests[1].credentials).toBe('omit');
  expect(requests[1].headers.get('Content-Type')).toBe('image/png');
  expect(requests[1].headers.get('If-None-Match')).toBe('*');
  expect(await requests[1].text()).toBe('image-bytes');
  expect(await requests[2].text()).toBe('');
});

test('S3 전송 실패 시 완료 API를 호출하지 않는다', async () => {
  server.use(
    http.put(`${MOCK_UPLOAD_ORIGIN}/files/:fileId`, () => new HttpResponse(null, { status: 403 })),
  );
  await expect(
    uploadAttachment(new File(['image'], 'image.png', { type: 'image/png' })),
  ).rejects.toThrow();
  expect(requests.map((request) => request.method)).toEqual(['POST', 'PUT']);
});

test('잘못된 발급 응답은 파일 업로드에 사용하지 않는다', async () => {
  server.use(
    http.post(`${API_URL}/files/presigned-url`, () =>
      HttpResponse.json(
        {
          fileId: 'invalid',
          uploadUrl: `${MOCK_UPLOAD_ORIGIN}/files/1`,
          expiresAt: '2026-10-22T10:00:00',
        },
        { status: 201 },
      ),
    ),
  );
  await expect(
    uploadAttachment(new File(['image'], 'image.png', { type: 'image/png' })),
  ).rejects.toThrow();
  expect(requests.map((request) => request.method)).toEqual(['POST']);
});
