import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from '../App';

test('버튼을 클릭하면 카운트가 1 증가한다', async () => {
  render(<App />);

  const button = screen.getByRole('button', { name: '0' });
  await userEvent.click(button);

  expect(button).toHaveTextContent('1');
});
