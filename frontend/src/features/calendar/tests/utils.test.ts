import { getKoreanToday } from '../utils';

test.each([
  ['2026-11-05T14:59:59Z', 2026, 11, 5],
  ['2026-11-05T15:00:00Z', 2026, 11, 6],
  ['2026-12-31T15:00:00Z', 2027, 1, 1],
])('한국 기준 날짜 경계를 반영한다 (%s)', (now, year, month, day) => {
  const today = getKoreanToday(new Date(now));
  expect([today.getFullYear(), today.getMonth() + 1, today.getDate()]).toEqual([year, month, day]);
  expect([today.getHours(), today.getMinutes(), today.getSeconds()]).toEqual([0, 0, 0]);
});
