import { act, renderHook, screen, waitFor } from '@testing-library/react';
import { QueryClient } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../config';
import { server } from '../../mocks/msw-node';
import { createWrapper, login, logout, setup } from '../../test/render';
import { userTable } from '../user/mocks/db';
import { createStudy, editStudy, fetchStudies, fetchStudyInfo } from './api';
import StudyList from './components/StudyList';
import StudyManagementPage from './pages/StudyManagementPage';
import { STUDY_URLS } from './urls';
import useEditStudy from './hooks/useEditStudy';
import studyQueries from './queries';

beforeEach(async () => {
  await userTable.create({
    id: 1,
    name: '사진 테스트',
    profileImage: 'https://example.com/user.png',
  });
  login('사진 테스트');
});
afterEach(logout);

test('수정 성공 후 캐시된 목록과 관리 정보를 무효화한다', async () => {
  const { studyId } = await createStudy({ name: '사진 스터디', description: '' });
  const queryClient = new QueryClient();
  queryClient.setQueryData(studyQueries.list().queryKey, await fetchStudies());
  queryClient.setQueryData(studyQueries.info(studyId).queryKey, await fetchStudyInfo(studyId));
  const { result } = renderHook(() => useEditStudy(studyId), {
    wrapper: createWrapper({ queryClient }),
  });
  act(() => result.current.mutate({ name: '사진 스터디', description: '', imageFileId: 30 }));
  await waitFor(() =>
    expect(queryClient.getQueryState(studyQueries.info(studyId).queryKey)?.isInvalidated).toBe(
      true,
    ),
  );
  expect(queryClient.getQueryState(studyQueries.list().queryKey)?.isInvalidated).toBe(true);
});

test('생성·교체한 이미지가 목록과 관리 정보 응답에 반영되며 일반 수정은 이미지를 보존한다', async () => {
  const { studyId } = await createStudy({ name: '사진 스터디', description: '', imageFileId: 10 });
  expect(await fetchStudyInfo(studyId)).toEqual(
    expect.objectContaining({ imageUrl: expect.stringContaining('/images/10') }),
  );
  await editStudy(studyId, { name: '사진 스터디', description: '', imageFileId: 20 });
  await editStudy(studyId, { name: '변경한 이름', description: '' });
  expect(await fetchStudyInfo(studyId)).toEqual(
    expect.objectContaining({ imageUrl: expect.stringContaining('/images/20') }),
  );
  const { studies } = await fetchStudies();
  expect(studies[0].imageUrl).toContain('/images/20');
  const { container } = setup(<StudyList studies={studies} />, { wrapper: createWrapper() });
  expect(container.querySelector('img[src$="/images/20"]')).toBeInTheDocument();
});

test('생성 요청에 이미지 ID와 기본 공개 범위를 전달한다', async () => {
  let body: unknown;
  server.use(
    http.post(`${API_URL}${STUDY_URLS.create}`, async ({ request }) => {
      body = await request.json();
      return HttpResponse.json({ studyId: 1 }, { status: 201 });
    }),
  );
  await createStudy({ name: '사진 스터디', description: '', imageFileId: 10 });
  expect(body).toEqual({ name: '사진 스터디', description: '', imageFileId: 10, isPublic: false });
});

test('관리 페이지에 서버 프로필 이미지를 표시한다', async () => {
  const { studyId } = await createStudy({ name: '사진 스터디', description: '', imageFileId: 10 });
  setup(<StudyManagementPage />, {
    wrapper: createWrapper({
      initialEntries: [`/studies/${studyId}/management`],
      routes: (element) => <Route path="/studies/:studyId/management" element={element} />,
    }),
  });
  expect(await screen.findByAltText('사진 스터디 프로필')).toHaveAttribute(
    'src',
    expect.stringContaining('/images/10'),
  );
});
