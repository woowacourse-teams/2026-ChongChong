export const invalidImageFileCases = [
  {
    title: '5MB를 초과하는 이미지',
    createFile: () =>
      new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' }),
    message: '파일당 최대 5MB까지 첨부할 수 있어요.',
  },
  {
    title: '허용하지 않는 확장자의 파일',
    createFile: () => new File(['gif-image'], 'profile.gif', { type: 'image/gif' }),
    message: 'JPEG, PNG, WebP 이미지만 첨부할 수 있어요.',
  },
];
