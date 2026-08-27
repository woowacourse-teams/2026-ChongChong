import api from '../../client';
import type {
  NoticeListResponse,
  NoticeReadStatus,
  NoticeDetail,
  MemberReadStatus,
  NoticeFormValues,
  CreateNoticeResponse,
  UpdateNoticeValue,
  UpdateNoticeReadResponse,
} from './types';

export async function fetchNoticeList(studyId: number, cursor?: number) {
  const response = await api.get(`/studies/${studyId}/notices`, {
    searchParams: cursor === undefined ? undefined : { cursor },
  });
  return await response.json<NoticeListResponse>();
}

export async function fetchNoticeReadStatus(studyId: number, noticeId: number) {
  const response = await api.get(`/studies/${studyId}/notices/${noticeId}/status`);
  return await response.json<NoticeReadStatus>();
}

export async function fetchNoticeDetail(studyId: number, noticeId: number) {
  const response = await api.get(`/studies/${studyId}/notices/${noticeId}`);
  return await response.json<NoticeDetail>();
}

export async function fetchNoticeMyRead(studyId: number, noticeId: number) {
  const response = await api.get(`/studies/${studyId}/notices/${noticeId}/status/me`);
  return await response.json<MemberReadStatus>();
}

export async function createNotice(studyId: number, noticeId: number, values: NoticeFormValues) {
  const response = await api.post(`/studies/${studyId}/notices`, {
    json: values,
  });
  return await response.json<CreateNoticeResponse>();
}

export async function deleteNotice(studyId: number, noticeId: number) {
  await api.delete(`/studies/${studyId}/notices/${noticeId}`);
}

export async function updateNotice(studyId: number, noticeId: number, values: UpdateNoticeValue) {
  await api.delete(`/studies/${studyId}/notices/${noticeId}`, {
    json: values,
  });
}

export async function updateNoticeRead(studyId: number, noticeId: number) {
  const response = await api.patch(`/studies/${studyId}/notices/${noticeId}/read`);
  return await response.json<UpdateNoticeReadResponse>();
}
