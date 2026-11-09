import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { setup } from '../../../test/render';
import FloatingPanel from './FloatingPanel';

function renderFloatingPanel() {
  return setup(
    <FloatingPanel
      aria-label="테스트 패널"
      trigger={({ bottom, ...props }) => (
        <button {...props} style={{ bottom }}>
          패널 열기
        </button>
      )}
      closeButton={({ close }) => <button onClick={close}>닫기</button>}
    >
      <button>패널 안의 버튼</button>
    </FloatingPanel>,
  );
}

async function openFloatingPanel() {
  const { user } = renderFloatingPanel();
  const trigger = screen.getByRole('button', { name: '패널 열기' });
  await user.click(trigger);

  const panel = screen.getByRole('dialog', { name: '테스트 패널' });
  const closeButton = within(panel).getByRole('button', { name: '닫기' });
  await waitFor(() => expect(closeButton).toBeVisible());

  return { user, trigger, panel, closeButton };
}

describe('FloatingPanel', () => {
  test('닫기 버튼을 누르면 패널이 닫히고 열기 버튼으로 초점이 돌아온다', async () => {
    const { user, trigger, closeButton } = await openFloatingPanel();

    await user.click(closeButton);

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toBeVisible();
    expect(trigger).toHaveFocus();
  });

  test('패널 바깥 영역을 클릭하면 닫히고 열기 버튼으로 초점이 돌아온다', async () => {
    const { user, trigger, panel } = await openFloatingPanel();

    await user.click(panel);

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  test('Escape에 해당하는 취소 이벤트가 발생하면 닫히고 열기 버튼으로 초점이 돌아온다', async () => {
    const { trigger, panel } = await openFloatingPanel();

    const cancelEvent = new Event('cancel', { cancelable: true });
    fireEvent(panel, cancelEvent);

    expect(cancelEvent.defaultPrevented).toBe(true);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });
});
