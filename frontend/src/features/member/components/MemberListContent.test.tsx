import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import userEvent from '@testing-library/user-event';
import { InviteLinkBox } from './MemberListContent';
import { createWrapper } from '../../../test/render';
import { server } from '../../../mocks/msw-node';
import { BASE_URL } from '../../../../config';
import { STUDY_URLS } from '../../studies/urls';

const STUDY_INVITE_LINK_URL = `${BASE_URL}${STUDY_URLS.inviteLink}`;

function mockStudyInviteLink() {
  server.use(
    http.get(STUDY_INVITE_LINK_URL, () =>
      HttpResponse.json({
        inviteLink: 'mock-chongchong-invite-link123',
      }),
    ),
  );
}

describe('초대 링크 테스트', () => {
  test('버튼을 클릭하면 초대 링크가 복사된다', async () => {
    mockStudyInviteLink();
    const user = userEvent.setup();

    const writeText = jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();

    render(<InviteLinkBox studyId={1} />, { wrapper: createWrapper() });

    const copyButton = await screen.findByRole('button', {
      name: '링크 복사',
    });

    await user.click(copyButton);

    // 이후에 API 값으로 전환
    expect(writeText).toHaveBeenCalledWith('mock-chongchong-invite-link123');

    writeText.mockRestore();
  });
});
