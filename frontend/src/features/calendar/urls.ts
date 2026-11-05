export const CALENDAR_URLS = {
  monthly: {
    path: (studyId: number) => `/studies/${studyId}/calendar`,
    mock: '/studies/:studyId/calendar',
  },
};
