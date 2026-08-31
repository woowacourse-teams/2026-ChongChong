import api from '../../client';
import type { NoticeFormValues, UpdateNoticeValue } from './types';
import {
  isNoticeListResponse,
  isNoticeReadStatusResponse,
  isNoticeDetailResponse,
  isMyReadStatusResponse,
  isCreateNoticeResponse,
  isUpdateReadStatusResponse,
} from './responseSchemas';

export async function fetchNoticeList(studyId: number, cursor?: number) {
  try {
    const response = await api.get(`/studies/${studyId}/notices`, {
      searchParams: cursor === undefined ? undefined : { cursor },
    });
    const data: unknown = await response.json();

    if (!isNoticeListResponse(data)) {
      throw new Error('공지 목록 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('공지 목록을 불러오는데 실패했습니다.');
  }
}

export async function fetchNoticeReadStatus(studyId: number, noticeId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/notices/${noticeId}/status`);
    const data: unknown = await response.json();

    if (!isNoticeReadStatusResponse(data)) {
      throw new Error('공지 읽음 현황 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('공지 읽음 현황을 불러오는데 실패했습니다.');
  }
}

export async function fetchNoticeDetail(studyId: number, noticeId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/notices/${noticeId}`);
    const data: unknown = await response.json();

    if (!isNoticeDetailResponse(data)) {
      throw new Error('공지 상세 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('공지 정보를 불러오는데 실패했습니다.');
  }
}

export async function fetchNoticeMyRead(studyId: number, noticeId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/notices/${noticeId}/status/me`);
    const data: unknown = await response.json();

    if (!isMyReadStatusResponse(data)) {
      throw new Error('내 공지 읽음 상태 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('내 공지 읽음 상태를 불러오는데 실패했습니다.');
  }
}

export async function createNotice(studyId: number, values: NoticeFormValues) {
  try {
    const response = await api.post(`/studies/${studyId}/notices`, {
      json: values,
    });
    const data: unknown = await response.json();

    if (!isCreateNoticeResponse(data)) {
      throw new Error('공지 생성 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('공지 생성에 실패했습니다.');
  }
}

export async function deleteNotice(studyId: number, noticeId: number) {
  try {
    await api.delete(`/studies/${studyId}/notices/${noticeId}`);
  } catch {
    throw new Error('공지 삭제에 실패했습니다.');
  }
}

export async function updateNotice(studyId: number, noticeId: number, values: UpdateNoticeValue) {
  try {
    await api.patch(`/studies/${studyId}/notices/${noticeId}`, {
      json: values,
    });
  } catch {
    throw new Error('공지 수정에 실패했습니다.');
  }
}

export async function updateNoticeRead(studyId: number, noticeId: number) {
  try {
    const response = await api.patch(`/studies/${studyId}/notices/${noticeId}/read`);
    const data: unknown = await response.json();

    if (!isUpdateReadStatusResponse(data)) {
      throw new Error('공지 읽음 처리 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('공지 읽음 처리에 실패했습니다.');
  }
}
