import { test, expect } from '@playwright/test';
import type { E2EScenario } from '../../../../mocks/e2eScenario';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    sessionStorage.setItem(
      'e2e:scenario',
      JSON.stringify({
        currentUserId: 7,
        users: [
          {
            id: 7,
            name: '깡이',
            profileImage: null,
          },
        ],
        studies: [
          {
            id: 1,
            name: '정겨운 밥상',
            description: '정겨운 밥상 메뉴 알림',
            inviteLink: 'pangyo',
          },
        ],
        members: [
          {
            id: 1,
            studyId: 1,
            userId: 7,
            name: '깡이',
            profileImage: null,
            role: 'LEADER',
          },
        ],
      } satisfies E2EScenario),
    );
  });
});

test('로그인한 사용자가 가입한 스터디가 목록에 표시된다', async ({ page }) => {
  await page.goto('/studies/');
  await expect(page).toHaveTitle('총총');
  await expect(page.getByText('정겨운 밥상', { exact: true })).toBeVisible();
});
