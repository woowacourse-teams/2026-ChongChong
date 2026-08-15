import Button from './shared/ui/Button';
import { useRef } from 'react';
import AlertDialog from './shared/ui/dialogs/AlertDialog';
import ConfirmDialog from './shared/ui/dialogs/ConfirmDialog';

export default function App() {
  const alertDialogRef = useRef<HTMLDialogElement>(null);
  const confirmDialogRef = useRef<HTMLDialogElement>(null);

  const handleOpenAlertDialog = () => {
    alertDialogRef.current?.showModal();
  };

  const handleCloseAlertDialog = () => {
    alertDialogRef.current?.close();
  };

  const handleOpenConfirmDialog = () => {
    confirmDialogRef.current?.showModal();
  };

  const handleCloseConfirmDialog = () => {
    confirmDialogRef.current?.close();
  };

  return (
    <div>
      <Button onClick={handleOpenAlertDialog} variant="BrandSolid" size="Large">
        Alert Button
      </Button>
      <Button onClick={handleOpenConfirmDialog} variant="BrandSolid" size="Large">
        Confirm Button
      </Button>
      <p css={{ height: '2000px' }}>스크롤 만들어내기</p>
      <AlertDialog
        ref={alertDialogRef}
        title={<AlertDialog.Title>회원 탈퇴가 불가능해요</AlertDialog.Title>}
        description={
          <AlertDialog.Description>
            {'스터디 리드는 다른 멤버에게\n리드 권한을 넘긴 후 탈퇴할 수 있어요'}
          </AlertDialog.Description>
        }
        closeButton={
          <AlertDialog.CloseButton onClick={handleCloseAlertDialog}>확인</AlertDialog.CloseButton>
        }
      ></AlertDialog>
      <ConfirmDialog
        ref={confirmDialogRef}
        title={<ConfirmDialog.Title>공지를 삭제할까요?</ConfirmDialog.Title>}
        description={
          <ConfirmDialog.Description>
            {'삭제한 공지는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
          </ConfirmDialog.Description>
        }
        closeButton={
          <ConfirmDialog.CloseButton onClick={handleCloseConfirmDialog}>
            취소
          </ConfirmDialog.CloseButton>
        }
        confirmButton={
          <ConfirmDialog.ConfirmButton onClick={handleCloseConfirmDialog}>
            삭제
          </ConfirmDialog.ConfirmButton>
        }
      ></ConfirmDialog>
    </div>
  );
}
