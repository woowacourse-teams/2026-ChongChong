export function formatDeadline(value: string) {
  const date = new Date(value);

  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ${String(
    date.getHours(),
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

export function formatReminder(value: string) {
  const now = new Date();
  const remindDate = new Date(value);

  const diffMs = remindDate.getTime() - now.getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));

  if (diffMinutes < 60) {
    return `${diffMinutes}분 뒤`;
  }

  const diffHours = Math.floor(diffMinutes / 60);

  if (diffHours < 24) {
    return `${diffHours}시간 뒤`;
  }

  const diffDays = Math.floor(diffHours / 24);

  return `${diffDays}일 뒤`;
}

export function formatSubmittedAt(value: string) {
  const date = new Date(value.replace(' ', 'T'));

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${hours}:${minutes} 제출`;
}
