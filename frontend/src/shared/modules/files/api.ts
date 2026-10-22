import api from '../../../client';
import { ApiError, handleError } from '../../api/error';
import { uploadUrlSchema } from './responseSchemas';
import { getFileContentType } from './validation';

export async function uploadAttachment(file: File): Promise<number> {
  try {
    const contentType = getFileContentType(file);
    const data: unknown = await api
      .post('/files/presigned-url', {
        json: { originalFileName: file.name, contentType, size: file.size },
      })
      .json();
    const { fileId, uploadUrl } = uploadUrlSchema.parse(data);
    // 서명 URL에는 앱 인증 헤더와 쿠키를 전달하지 않는다.
    const uploaded = await fetch(uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': contentType, 'If-None-Match': '*' },
      credentials: 'omit',
      body: file,
    });
    if (!uploaded.ok) throw new Error('파일 전송에 실패했습니다.');
    const completed = await api.post(`/files/${fileId}/complete`);
    if (completed.status !== 204) throw new Error('업로드 완료 응답이 올바르지 않습니다.');
    return fileId;
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('파일 업로드에 실패했어요. 삭제 후 다시 선택해주세요.', { cause: error }),
    });
  }
}
