import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { useNavigate } from 'react-router';
import { tokens, typography } from '../../../styles/global';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import { useToast } from '../../../shared/providers/ToastProvider';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { deleteAssignment } from '../api';
import assignmentQueries from '../queries';

const containerStyle = { position: 'relative' } satisfies CSSProperties;

const moreButtonStyle = {
  display: 'grid',
  placeItems: 'center',
  width: 40,
  height: 40,
  padding: 0,
  border: 0,
  background: 'transparent',
  color: tokens.text.primary,
  cursor: 'pointer',
} satisfies CSSProperties;

const menuStyle = {
  position: 'absolute',
  top: 28,
  right: 0,
  zIndex: 1,
  width: 100,
  overflow: 'hidden',
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
  boxShadow: tokens.shadow[2],
} satisfies CSSProperties;

const menuButtonStyle = {
  ...typography.body,
  display: 'block',
  width: '100%',
  minHeight: 40,
  padding: `${tokens.spacing[2]} 0`,
  border: 0,
  background: 'transparent',
  color: tokens.text.primary,
  textAlign: 'center',
  cursor: 'pointer',
} satisfies CSSProperties;

interface Props {
  studyId: number;
  assignmentId: number;
}

export default function AssignmentHeaderActions({ studyId, assignmentId }: Props) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmOpen, openDialog, closeDialog] = useBooleanState();

  useEffect(() => {
    if (!menuOpen) return;
    const closeOutside = (event: PointerEvent) => {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) {
        setMenuOpen(false);
      }
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuOpen(false);
    };
    document.addEventListener('pointerdown', closeOutside);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOutside);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [menuOpen]);

  const { mutate, isPending } = useMutation({
    mutationFn: () => deleteAssignment(studyId, assignmentId),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: assignmentQueries.detail(studyId, assignmentId).queryKey,
      });
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(studyId) });
      navigate(`/studies/${studyId}/assignments`);
    },
    onError: (error) => {
      closeDialog();
      toast.open(<StatusToast status="Error" message={error.message} />);
    },
  });

  return (
    <div ref={menuRef} css={containerStyle}>
      <button
        type="button"
        css={moreButtonStyle}
        aria-label="과제 더보기"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        onClick={() => setMenuOpen((open) => !open)}
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <circle cx="4" cy="12" r="2" />
          <circle cx="12" cy="12" r="2" />
          <circle cx="20" cy="12" r="2" />
        </svg>
      </button>
      {menuOpen && (
        <div role="menu" aria-label="과제 관리" css={menuStyle}>
          <button
            type="button"
            role="menuitem"
            css={menuButtonStyle}
            onClick={() => {
              setMenuOpen(false);
              navigate(`/studies/${studyId}/assignments/${assignmentId}/edit`);
            }}
          >
            과제 수정
          </button>
          <button
            type="button"
            role="menuitem"
            css={{
              ...menuButtonStyle,
              borderTop: tokens.border.neutral,
              color: tokens.text.critical,
            }}
            onClick={() => {
              setMenuOpen(false);
              openDialog();
            }}
          >
            과제 삭제
          </button>
        </div>
      )}
      {confirmOpen && (
        <ConfirmDialog
          title="과제를 삭제할까요?"
          description={'삭제한 과제는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
          onClose={closeDialog}
          closeButton={
            <ConfirmDialog.CloseButton onClick={closeDialog}>취소</ConfirmDialog.CloseButton>
          }
          confirmButton={
            <ConfirmDialog.ConfirmButton disabled={isPending} onClick={() => mutate()}>
              {isPending ? '삭제 중...' : '삭제'}
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </div>
  );
}
