import { useState, ChangeEventHandler, useCallback } from 'react';

type InputLikeElement = HTMLInputElement | HTMLTextAreaElement;

export function useInputState(initialValue = '', transformValue: (value: string) => string = echo) {
  const [value, setValue] = useState(initialValue);

  const handleValueChange: ChangeEventHandler<InputLikeElement> = useCallback(
    ({ target: { value } }) => {
      setValue(transformValue(value));
    },
    [transformValue],
  );

  return [value, handleValueChange] as const;
}

function echo(v: string) {
  return v;
}
