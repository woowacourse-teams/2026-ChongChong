import { screen, within } from '@testing-library/react';
import { Link, Route } from 'react-router';
import { createWrapper, setup } from '../../../test/render';
import Guideline from '../Guideline';
import { useGuidelineController } from '../hooks/useGuidelineController';

const storageKey = 'chongchong:guideline';

function GuidelineEntry() {
  const controller = useGuidelineController();
  return <Guideline controller={controller} />;
}

function renderGuideline() {
  return setup(<GuidelineEntry />, {
    wrapper: createWrapper({
      initialEntries: ['/studies'],
      routes: (element) => (
        <>
          <Route
            path="/studies"
            element={
              <>
                <Link to="/elsewhere">다른 화면</Link>
                {element}
              </>
            }
          />
          <Route path="/elsewhere" element={<Link to="/studies">목록으로</Link>} />
          <Route path="/studies/join" element={<Link to="/studies">참여 후 목록으로</Link>} />
          <Route path="/studies/mypage" element={<Link to="/studies">설정 후 목록으로</Link>} />
        </>
      ),
    }),
  });
}

beforeEach(() => localStorage.clear());
afterEach(() => localStorage.clear());

test('첫 페이지에는 이전 버튼이 없고 마지막 페이지에는 다음 버튼이 없다', async () => {
  const { user } = renderGuideline();
  const dialog = screen.getByRole('dialog');
  expect(within(dialog).queryByRole('button', { name: '이전 가이드' })).not.toBeInTheDocument();

  for (let page = 1; page < 5; page += 1) {
    await user.click(within(dialog).getByRole('button', { name: '다음 가이드' }));
  }

  expect(within(dialog).queryByRole('button', { name: '다음 가이드' })).not.toBeInTheDocument();
  expect(within(dialog).getByRole('button', { name: '이전 가이드' })).toBeInTheDocument();
  expect(JSON.parse(localStorage.getItem(storageKey)!)).toEqual({ page: 5, dismissed: false });
  await user.click(within(dialog).getByRole('button', { name: '이전 가이드' }));
  expect(JSON.parse(localStorage.getItem(storageKey)!)).toEqual({ page: 4, dismissed: false });
});

test('일반 닫기는 현재 페이지를 보존하고 목록에 재진입하면 다시 보여 준다', async () => {
  localStorage.setItem(storageKey, JSON.stringify({ page: 3, dismissed: false }));
  const { user } = renderGuideline();
  const heading = within(screen.getByRole('dialog')).getByRole('heading').textContent;

  await user.click(screen.getByRole('button', { name: '가이드 닫기' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  await user.click(screen.getByRole('link', { name: '다른 화면' }));
  await user.click(screen.getByRole('link', { name: '목록으로' }));

  expect(within(screen.getByRole('dialog')).getByRole('heading')).toHaveTextContent(heading!);
  expect(JSON.parse(localStorage.getItem(storageKey)!)).toEqual({ page: 3, dismissed: false });
});

test('다시 보지 않기는 페이지를 초기화하고 새 방문에서도 가이드를 숨긴다', async () => {
  localStorage.setItem(storageKey, JSON.stringify({ page: 4, dismissed: false }));
  const { user, unmount } = renderGuideline();
  await user.click(screen.getByRole('button', { name: '다시 보지 않기' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(JSON.parse(localStorage.getItem(storageKey)!)).toEqual({ page: 1, dismissed: true });

  unmount();
  renderGuideline();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('알림 켜기는 지원 여부와 관계없이 마이페이지로 이동하고 돌아오면 3페이지를 복원한다', async () => {
  localStorage.setItem(storageKey, JSON.stringify({ page: 3, dismissed: false }));
  const { user } = renderGuideline();
  await user.click(await screen.findByRole('button', { name: '알림 켜기' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  await user.click(screen.getByRole('link', { name: '설정 후 목록으로' }));
  expect(await screen.findByRole('button', { name: '알림 켜기' })).toBeInTheDocument();
});

test('초대 링크 입력 화면에 다녀와도 4페이지를 기억한다', async () => {
  localStorage.setItem(storageKey, JSON.stringify({ page: 4, dismissed: false }));
  const { user } = renderGuideline();
  await user.click(screen.getByRole('button', { name: '초대 링크 입력하기' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  await user.click(screen.getByRole('link', { name: '참여 후 목록으로' }));
  expect(screen.getByRole('button', { name: '초대 링크 입력하기' })).toBeInTheDocument();
});

test('마지막 페이지의 시작 버튼으로 닫아도 다음 방문의 가이드를 영구 숨김 처리하지 않는다', async () => {
  localStorage.setItem(storageKey, JSON.stringify({ page: 5, dismissed: false }));
  const { user } = renderGuideline();
  await user.click(screen.getByRole('button', { name: '스터디 시작하기' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(JSON.parse(localStorage.getItem(storageKey)!)).toEqual({ page: 5, dismissed: false });
});
