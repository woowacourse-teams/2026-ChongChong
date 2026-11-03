import { useRef } from 'react';
import type { CSSObject } from '@emotion/react';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import BottomSheet from '../../../shared/ui/date-time-picker/BottomSheet';
import { IMAGE_ACCEPT } from '../../../shared/modules/files/validation';
import { tokens } from '../../../styles/global';
import camera from '../assets/profile-camera.svg';
import StudyAvatar from './StudyAvatar';

interface Props {
  imageUrl?: string | null;
  disabled: boolean;
  onSelect: (file: File) => void;
  onReset: () => void;
}

const StudyProfileFieldStyle = {
  display: 'flex',
  justifyContent: 'center',
  margin: '24px 0 52px',
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

export default function StudyProfileField({ imageUrl, disabled, onSelect, onReset }: Props) {
  const [open, openBottomSheet, closeBottomSheet] = useBooleanState();
  const input = useRef<HTMLInputElement>(null);

  function selectProfileImage() {
    closeBottomSheet();
    input.current?.click();
  }

  function selectDefaultProfileImage() {
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
        disabled={disabled}
        onClick={openBottomSheet}
        css={ProfileButtonStyle}
      >
        <StudyAvatar imageUrl={imageUrl} alt="스터디 프로필" />
        <span css={CameraBadgeStyle}>
          <img src={camera} alt="" width={16} height={16} />
        </span>
      </button>
      <input
        ref={input}
        type="file"
        hidden
        aria-label="프로필 이미지 파일"
        accept={IMAGE_ACCEPT}
        disabled={disabled}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = '';
          if (file) onSelect(file);
        }}
      />
      <BottomSheet open={open} title="프로필 이미지" variant="menu" onClose={closeBottomSheet}>
        <BottomSheet.Action onClick={selectProfileImage}>프로필 이미지 선택</BottomSheet.Action>
        {imageUrl && (
          <BottomSheet.Action onClick={selectDefaultProfileImage}>
            기본 프로필 이미지 사용
          </BottomSheet.Action>
        )}
        <BottomSheet.Action onClick={closeBottomSheet}>취소</BottomSheet.Action>
      </BottomSheet>
    </div>
  );
}
