import { useLayoutEffect, useRef } from 'react';

function resizeTextArea(textArea: HTMLTextAreaElement | null) {
  if (!textArea) return;

  textArea.style.height = 'auto';
  textArea.style.height = `${textArea.scrollHeight}px`;
}

export default function useAutoResizeTextArea({ inputValue }: { inputValue: string }) {
  const targetRef = useRef<HTMLTextAreaElement>(null);

  useLayoutEffect(() => {
    resizeTextArea(targetRef.current);
  }, [inputValue]);

  return { targetRef };
}
