import type { Member } from '../../../member/types';
import Avatar from '../../../../shared/ui/Avatar';
import Checkbox from '../../../../shared/ui/inputs/Checkbox';
import { Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';

interface SelectSubmitterFieldProps {
  member: Member;
  isChecked: boolean;
  onChangeSubmitters: (memberId: number) => void;
}

const selectSubmitterFieldLabelStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  minHeight: 68,
  padding: `0 ${tokens.spacing[4]}`,
  border: tokens.border.default,
  borderRadius: tokens.radius.md,
  cursor: 'pointer',
};

export function SelectSubmitterField({
  member,
  isChecked,
  onChangeSubmitters,
}: SelectSubmitterFieldProps) {
  return (
    <label css={selectSubmitterFieldLabelStyle}>
      <Avatar
        src={member.profileImage}
        alt={`${member.name} 프로필`}
        size={28}
        fallback={<span aria-hidden="true">{member.name.slice(0, 1)}</span>}
        css={{
          background: tokens.color.optionSub6,
          fontSize: tokens.fontSize[12],
        }}
      />
      <Paragraph variant="subtitle" css={{ flex: 1 }}>
        {member.name}
        {member.role === 'LEADER' ? ' (나)' : ''}
      </Paragraph>
      <Checkbox
        aria-label={`${member.name}${member.role === 'LEADER' ? ' (나)' : ''}`}
        checked={isChecked}
        onChange={() => onChangeSubmitters(member.id)}
      />
    </label>
  );
}

const selectAllSubmittersFieldStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  cursor: 'pointer',
};

interface SelectAllSubmittersFieldProps {
  checked: boolean;
  disabled: boolean;
  selectedCount: number;
  onChange: () => void;
}

export function SelectAllSubmittersField({
  checked,
  disabled,
  selectedCount,
  onChange,
}: SelectAllSubmittersFieldProps) {
  return (
    <label css={selectAllSubmittersFieldStyle}>
      <Checkbox aria-label="전체 선택" checked={checked} disabled={disabled} onChange={onChange} />
      <Paragraph variant="paragraph" css={{ flex: 1 }}>
        전체 선택
      </Paragraph>
      <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
        {selectedCount}명 선택
      </Paragraph>
    </label>
  );
}
