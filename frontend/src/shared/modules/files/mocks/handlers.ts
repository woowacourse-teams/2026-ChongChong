import { http, HttpResponse } from 'msw';
import { z } from 'zod';
import { API_URL } from '../../../../../config';
import { findUserFromHeader } from '../../../../mocks/auth';
import { fileTable, MOCK_UPLOAD_ORIGIN } from './db';

const uploadRequest = z.object({
  originalFileName: z.string().trim().min(1).max(255),
  contentType: z.string().trim().min(1).max(255),
  size: z.number().int().min(0).max(5_242_880),
});
let nextFileId = 1;

export const handlers = [
  http.post(`${API_URL}/files/presigned-url`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const parsed = uploadRequest.safeParse(await request.json());
    if (!parsed.success) {
      return HttpResponse.json(
        { code: 'INVALID_REQUEST', message: '파일 정보를 확인해주세요.' },
        { status: 400 },
      );
    }

    while (fileTable.findFirst((q) => q.where({ id: nextFileId }))) nextFileId++;
    const fileId = nextFileId++;
    await fileTable.create({
      id: fileId,
      ownerUserId: user.id,
      ...parsed.data,
      uploadedSize: null,
      status: 'PENDING',
    });
    return HttpResponse.json(
      {
        fileId,
        uploadUrl: `${MOCK_UPLOAD_ORIGIN}/files/${fileId}`,
        expiresAt: new Date(Date.now() + 600_000).toISOString(),
      },
      { status: 201 },
    );
  }),

  http.put(`${MOCK_UPLOAD_ORIGIN}/files/:fileId`, async ({ request, params }) => {
    const file = fileTable.findFirst((q) => q.where({ id: Number(params.fileId) }));
    if (!file) return new HttpResponse(null, { status: 404 });
    if (
      request.headers.get('Content-Type') !== file.contentType ||
      request.headers.get('If-None-Match') !== '*'
    ) {
      return new HttpResponse(null, { status: 403 });
    }
    if (file.uploadedSize !== null) return new HttpResponse(null, { status: 412 });
    const size = (await request.arrayBuffer()).byteLength;
    await fileTable.update(file, {
      data(record) {
        record.uploadedSize = size;
      },
    });
    return new HttpResponse(null, { status: 200 });
  }),

  http.post(`${API_URL}/files/:fileId/complete`, async ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const file = fileTable.findFirst((q) => q.where({ id: Number(params.fileId) }));
    if (!file)
      return HttpResponse.json(
        { code: 'FILE_NOT_FOUND', message: '파일을 찾을 수 없습니다.' },
        { status: 404 },
      );
    if (file.ownerUserId !== user.id)
      return HttpResponse.json(
        { code: 'FILE_NOT_OWNED', message: '본인의 파일만 첨부할 수 있습니다.' },
        { status: 403 },
      );
    if (file.status === 'COMPLETED') return new HttpResponse(null, { status: 204 });
    if (file.uploadedSize === null)
      return HttpResponse.json(
        { code: 'FILE_UPLOAD_NOT_FOUND', message: '업로드한 파일을 찾을 수 없습니다.' },
        { status: 409 },
      );
    if (file.uploadedSize !== file.size)
      return HttpResponse.json(
        { code: 'FILE_SIZE_MISMATCH', message: '업로드한 파일 크기가 일치하지 않습니다.' },
        { status: 400 },
      );
    await fileTable.update(file, {
      data(record) {
        record.status = 'COMPLETED';
      },
    });
    return new HttpResponse(null, { status: 204 });
  }),
];
