import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { useNavigate } from 'react-router';
import { tokens, typography } from '../../styles/global';
import useBooleanState from '../hooks/useBooleanState';
import { useToast } from '../providers/ToastProvider';
import ConfirmDialog from '../ui/dialogs/ConfirmDialog';
import StatusToast from '../ui/toasts/StatusToast';
import { deleteNotice } from '../../features/notice/api';
import { deleteAssignment } from '../../features/assignment/api';
import noticeQueries from '../../features/notice/queries';
import assignmentQueries from '../../features/assignment/queries';

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
  id: number;
  content: 'notice' | 'assignment';
}

export default function ContentActionMenu({ studyId, id, content }: Props) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmOpen, openDialog, closeDialog] = useBooleanState();

  const deleteMutation = content === 'notice' ? deleteNotice : deleteAssignment;
  const queries = content === 'notice' ? noticeQueries : assignmentQueries;
  const domain = content === 'notice' ? '공지' : '과제';

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
    mutationFn: () => deleteMutation(studyId, id),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: queries.detail(studyId, id).queryKey,
      });
      if (content === 'notice') {
        queryClient.removeQueries({
          queryKey: noticeQueries.readStatus(studyId, id).queryKey,
        });
      }
      queryClient.invalidateQueries({ queryKey: queries.lists(studyId) });
      navigate(`/studies/${studyId}/${content}s`);
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
        aria-label={`${domain} 더보기`}
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
        <div role="menu" aria-label={`${domain} 관리`} css={menuStyle}>
          <button
            type="button"
            role="menuitem"
            css={menuButtonStyle}
            onClick={() => {
              setMenuOpen(false);
              navigate(`/studies/${studyId}/${content}s/${id}/edit`);
            }}
          >
            {domain} 수정
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
            {domain} 삭제
          </button>
        </div>
      )}
      {confirmOpen && (
        <ConfirmDialog
          title={`${domain}를 삭제할까요?`}
          description={`삭제한 ${domain}는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?`}
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
