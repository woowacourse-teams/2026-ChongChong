import { fireEvent } from '@testing-library/react';

export function createTextWithLength(length: number) {
  return '가'.repeat(length - 1) + '끝';
}

export function insertTextInMiddle(input: HTMLElement, value: string, text = '추') {
  const middle = Math.floor(value.length / 2);
  const insertedValue = value.slice(0, middle) + text + value.slice(middle);

  fireEvent.change(input, { target: { value: insertedValue } });

  return insertedValue;
}
