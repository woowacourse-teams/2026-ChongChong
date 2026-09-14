export const chongchongV2 = {
  color: {
    brand: { primary: '#00C471', subtle: '#E6F9F0' },
    border: { default: 'rgba(15,23,42,0.08)' },
    background: {
      muted: 'rgba(15,23,42,0.06)',
      default: '#FFFFFF',
      subtle: '#F9F9F9',
      icon: '#ECECEE',
    },
    neutral: { black: '#000000' },
    text: {
      placeholder: 'rgba(15,23,42,0.4)',
      secondary: 'rgba(15,23,42,0.7)',
      primary: '#172033',
      tertiary: 'rgba(15,23,42,0.55)',
      disabled: 'rgba(0,0,0,0.32)',
    },
    status: { error: '#DE5E56' },
    social: {
      'google-border': 'rgba(116,119,117,0.7)',
      'google-blue': '#1976D2',
      'google-green': '#4CAF50',
      'google-red': '#FF3D00',
      'google-yellow': '#FFC107',
      'kakao-background': '#FEE500',
    },
    board: { section: '#CEFFF3', role: '#FFCEE2', canvas: '#444444' },
    overlay: { scrim: 'rgba(0,0,0,0.42)' },
  },
  spacing: { 4: 4, 16: 16, 12: 12 },
} as const;

export const chongchongV2Typography = {
  font: {
    family: { pretendard: 'Pretendard' },
    size: { 12: 12, 13: 13, 14: 14, 16: 16, 18: 18, 24: 24, 36: 36, 48: 48 },
    'line-height': { 16: 16, 18: 18, 20: 20, 24: 24, 28: 28, 34: 34, 64: 64 },
  },
} as const;
