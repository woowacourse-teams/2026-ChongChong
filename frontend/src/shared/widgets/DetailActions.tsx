import type { CSSProperties } from 'react';
import deleteIcon from '../assets/delete.svg';
import modifyIcon from '../assets/modify.svg';
import Button from '../ui/Button';
import { tokens } from '../../styles/global';

interface DetailActionsProps {
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

export default function DetailActions({ onEdit, onDelete }: DetailActionsProps) {
  return (
    <div css={actionsStyle}>
      <Button variant="neutralOutline" size="small" onClick={onEdit}>
        <span css={{ ...buttonContentStyle, color: tokens.text.primary }}>
          <img src={modifyIcon} alt="" width={18} height={18} />
          수정
        </span>
      </Button>
      <Button variant="criticalOutline" size="small" onClick={onDelete}>
        <span css={buttonContentStyle}>
          <img src={deleteIcon} alt="" width={18} height={18} />
          삭제
        </span>
      </Button>
    </div>
  );
}
