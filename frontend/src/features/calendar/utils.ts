export function getKoreanToday(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  }).formatToParts(now);
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  const date = new Date(0);
  date.setFullYear(Number(values.year), Number(values.month) - 1, Number(values.day));
  date.setHours(0, 0, 0, 0);
  return date;
}
