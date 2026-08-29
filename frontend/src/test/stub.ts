// webpack의 `type: 'asset'`은 이미지 import를 URL 문자열로 바꿔주지만
// jest는 그 규칙을 모르므로, 테스트에서는 이 stub으로 대체합니다.
export default 'test-file-stub';
