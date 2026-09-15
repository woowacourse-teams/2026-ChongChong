// Figma 화면별 측정값입니다. 전역 변수 컬렉션과의 대응은 app/docs/STUDY_HOME_UI.md에 기록합니다.
export const homeLayout = {
  bannerSize: 120,
  statusHeight: 106,
  rowHeight: 66,
  iconSize: 18,
  rowPaddingLeft: 23,
  rowPaddingRight: 19,
  rowContentGap: 15,
  headingGap: 6,
  memberSectionAdjustment: -2,
  statusListGap: 14,
  statusShadow: '0 1px 2px rgba(15, 23, 42, 0.06)',
  header: {
    rightPadding: 19,
    contentGap: 18,
    copyGap: 2,
    copyOffset: -0.5,
    menuSize: 32,
  },
} as const;

export const notificationLayout = {
  rowHeight: 95,
  bottomPadding: 11,
  iconContainerSize: 36,
  unreadDotSize: 6,
  unreadDotTop: 7,
} as const;
