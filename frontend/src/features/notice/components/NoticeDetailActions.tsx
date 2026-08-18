import type { CSSProperties } from 'react';
import deleteIcon from '../../../shared/assets/delete.svg';
import modifyIcon from '../../../shared/assets/modify.svg';
import Button from '../../../shared/ui/Button';
import { tokens } from '../../../styles/global';

interface NoticeDetailActionsProps {
  onEdit: () => void;
  onDelete: () => void;
}

const actionsStyle = {
  display: 'flex',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const buttonContentStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

export default function NoticeDetailActions({ onEdit, onDelete }: NoticeDetailActionsProps) {
  return (
    <div css={actionsStyle}>
      <Button variant="neutralOutline" size="small" onClick={onEdit}>
        <span css={buttonContentStyle}>
          <img src={modifyIcon} alt="수정" width={18} height={18} />
          수정
        </span>
      </Button>
      <Button variant="criticalOutline" size="small" onClick={onDelete}>
        <span css={buttonContentStyle}>
          <img src={deleteIcon} alt="삭제" width={18} height={18} />
          삭제
        </span>
      </Button>
    </div>
  );
}
