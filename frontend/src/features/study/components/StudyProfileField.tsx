import { useId, useRef } from 'react';
import type { CSSObject } from '@emotion/react';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import ProfileImageActionSheet from './ProfileImageActionSheet';
import { IMAGE_ACCEPT } from '../../../shared/modules/files/validation';
import { tokens } from '../../../styles/global';
import camera from '../assets/profile-camera.svg';
import StudyAvatar from './StudyAvatar';
import { Field } from '../../../shared/ui/inputs/Field';

interface Props {
  imageUrl?: string | null;
  disabled: boolean;
  errorText?: string;
  helpText?: string;
  onSelect: (file: File) => void;
  onReset: () => void;
}

const StudyProfileFieldStyle = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  margin: '24px 0 8px',
} satisfies CSSObject;

const SubTextContainerStyle = {
  margin: '8px 0 0',
  minHeight: 'calc(2 * var(--line-height-18))',
  width: '100%',
  overflowWrap: 'anywhere',
} satisfies CSSObject;

const ProfileButtonStyle = {
  position: 'relative',
  width: 88,
  height: 88,
  padding: 0,
  border: 0,
  borderRadius: '50%',
  background: 'transparent',
  cursor: 'pointer',
  '&:focus-visible': { outline: `2px solid ${tokens.bg.brand}`, outlineOffset: 4 },
} satisfies CSSObject;

const CameraBadgeStyle = {
  position: 'absolute',
  right: -2,
  bottom: -2,
  width: 28,
  height: 28,
  boxSizing: 'border-box',
  border: '2px solid white',
  borderRadius: '50%',
  background: tokens.bg.brand,
  display: 'grid',
  placeItems: 'center',
} satisfies CSSObject;

export default function StudyProfileField({
  imageUrl,
  disabled,
  errorText,
  helpText,
  onSelect,
  onReset,
}: Props) {
  const [open, openBottomSheet, closeBottomSheet] = useBooleanState();
  const subTextId = useId();
  const hasSubText = Boolean(errorText || helpText);
  const input = useRef<HTMLInputElement>(null);

  function selectProfileImage() {
    if (disabled) return;
    closeBottomSheet();
    input.current?.click();
  }

  function selectDefaultProfileImage() {
    if (disabled) return;
    closeBottomSheet();
    onReset();
  }

  return (
    <div css={StudyProfileFieldStyle}>
      <button
        type="button"
        aria-label="스터디 프로필 이미지 변경"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-describedby={hasSubText ? subTextId : undefined}
        aria-invalid={Boolean(errorText)}
        disabled={disabled}
        onClick={openBottomSheet}
        css={ProfileButtonStyle}
      >
        <StudyAvatar imageUrl={imageUrl} alt="스터디 프로필" />
        <span css={CameraBadgeStyle}>
          <img src={camera} alt="" width={16} height={16} />
        </span>
      </button>
      <div id={subTextId} css={SubTextContainerStyle}>
        <Field.SubText errorText={errorText} helpText={helpText} />
      </div>
      <input
        ref={input}
        type="file"
        hidden
        aria-label="프로필 이미지 파일"
        accept={IMAGE_ACCEPT}
        aria-describedby={hasSubText ? subTextId : undefined}
        aria-invalid={Boolean(errorText)}
        disabled={disabled}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = '';
          if (file && !disabled) onSelect(file);
        }}
      />
      <ProfileImageActionSheet
        open={open && !disabled}
        hasImage={Boolean(imageUrl)}
        actions={{
          onClose: closeBottomSheet,
          onSelect: selectProfileImage,
          onReset: selectDefaultProfileImage,
        }}
      />
    </div>
  );
}
