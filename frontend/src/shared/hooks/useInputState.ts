import { useState, ChangeEventHandler, useCallback } from 'react';

type InputLikeElement = HTMLInputElement | HTMLTextAreaElement;

export function useInputState(
  initialValue: string | (() => string) = '',
  transformValue: (value: string, prevValue: string) => string = echo,
) {
  const [value, setValue] = useState(initialValue);

  const handleValueChange: ChangeEventHandler<InputLikeElement> = useCallback(
    ({ target: { value } }) => {
      setValue((prevValue) => transformValue(value, prevValue));
    },
    [transformValue],
  );

  return [value, handleValueChange] as const;
}

function echo(v: string) {
  return v;
}
