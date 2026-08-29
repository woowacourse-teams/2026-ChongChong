import { useRef } from 'react';

export default function useDialogControl() {
  const dialogRef = useRef<HTMLDialogElement>(null);

  const open = () => {
    dialogRef.current?.showModal();
  };

  const close = () => {
    dialogRef.current?.close();
  };

  return { dialogRef, open, close };
}
