import { render, screen, within } from '@testing-library/react';
import AttachmentList from './AttachmentList';
import type { Attachment } from '../modules/files/types';

const file: Attachment = {
  fileId: 1,
  originalFileName: '답안.pdf',
  contentType: 'application/pdf',
  size: 1258291,
  sortOrder: 0,
  imageUrl: null,
};

test('첨부 파일의 이름과 형식, 용량을 표시한다', () => {
  render(<AttachmentList attachments={[file]} />);
  const list = within(screen.getByRole('list', { name: '첨부 파일' }));
  expect(list.getByText('답안.pdf')).toBeVisible();
  expect(list.getByText('PDF · 1.2 MB')).toBeVisible();
});

test('조회 URL이 있는 첨부는 새 탭으로 열 수 있다', () => {
  render(
    <AttachmentList
      attachments={[
        {
          ...file,
          originalFileName: '결과.png',
          contentType: 'image/png',
          imageUrl: 'https://example.com/result.png?signature=test',
        },
      ]}
    />,
  );
  const link = screen.getByRole('link', { name: '결과.png 새 탭에서 열기' });
  expect(link).toHaveAttribute('href', 'https://example.com/result.png?signature=test');
  expect(link).toHaveAttribute('target', '_blank');
  expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  expect(link).not.toHaveAttribute('download');
});

test('조회 URL이 없는 첨부는 열기 링크 대신 안내를 표시한다', () => {
  render(<AttachmentList attachments={[file]} />);
  expect(screen.queryByRole('link')).not.toBeInTheDocument();
  expect(screen.getByText('미리보기 없음')).toBeVisible();
});
