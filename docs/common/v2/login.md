# 로그인

[← v2 개요](README.md) · [v1 대비 변경](changes.md)

## 디자인에 있는 기능

- 앱 시작 시 스플래시를 표시한다.
- 카카오·Google·Apple 계정으로 로그인·회원가입하는 진입 버튼을 제공한다.
- 로그인 화면에 서비스 이용약관·개인정보처리방침 동의 안내를 표시한다.
- 푸시 알림 권한 안내에서 ‘나중에’ 또는 ‘알림 허용’을 선택한다.

## v1에서 달라지는 점

- v1의 카카오 로그인에 Google·Apple 로그인 진입과 연동을 추가한다.
- 스플래시와 앱 알림 권한 안내를 추가한다.
- 서버에 소셜 제공자 열거형이 있는 것과 실제 로그인 제공자 연동 완료는 구분한다.

> [!NOTE]
> 계정 연결·동일 이메일 병합, 로그인 취소·실패 처리, 권한 거부 후 재안내 시점은 화면만으로 확정할 수 없다.
> 권한 안내의 정확한 노출 순서도 구현 전에 확인한다.

## Figma 근거

- [00-01-01 스플래시](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19897)
- [00-02-01 로그인 및 회원가입](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19876)
- [00-03-01 알림 권한 안내](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19902)
