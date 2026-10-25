# 알림 부하 테스트 sender

`SimulatedNotificationSender`는 외부 Push 서비스에 요청하지 않고 설정한 시간만큼 기다린 뒤 `SENT`를 반환한다.
알림 생성, Delivery 저장, DB claim, worker, 결과 저장 및 기존 알림 메트릭 경로는 그대로 사용한다.
고정 지연과 성공 경로의 처리량을 비교하기 위한 sender이며 실패율, 재시도 응답, 네트워크 timeout을 주입하지 않는다.

## 활성 조건

다음 조건을 모두 만족해야 애플리케이션이 시작된다.

- `web-push.sender.mode=simulated`
- 활성 Spring 프로필에 `load-test` 포함
- `app.environment=dev`
- 활성 Spring 프로필에 `prod` 미포함

`load-test` 프로필에서 모드를 생략하거나 `real`을 지정해도 시작을 거부한다. 설정 오타나 음수 지연 시간도 시작을 실패시킨다.
모드와 `load-test` 프로필을 모두 생략하면 기존 실제 sender를 사용한다.
시뮬레이터 모드에서는 실제 `WebPushClient`를 생성하지 않으며, 시작 로그에 모드와 지연 시간을 경고 수준으로 남긴다.

## 설정과 실행

기존 dev 실행 환경의 DB, 인증 및 Web Push 설정에 아래 환경 변수를 추가한다.

```dotenv
CLOUD_WATCH_APP_ENV=dev
SPRING_PROFILES_ACTIVE=load-test
WEB_PUSH_SENDER_MODE=simulated
WEB_PUSH_SENDER_SIMULATED_DELAY_MS=500
```

이미 필요한 프로필이 있다면 `SPRING_PROFILES_ACTIVE`에 `load-test`를 추가한다. 프로필 이름은 환경 표시를 대신하지 않는다.
공개키 조회 API가 VAPID 설정을 사용하므로 기존 dev VAPID 설정도 유지한다. 시뮬레이터가 해당 키로 외부 요청을 보내지는 않는다.

환경 변수를 적용한 터미널에서 기존 실행 명령 또는 다음 명령을 사용한다.

```sh
./gradlew bootRun
```

배포 Compose는 `/opt/chongchong/.env`에서 위 환경 변수를 읽어 backend 컨테이너에 전달한다.
컨테이너 실행 시에는 dev EC2의 해당 파일에 값을 설정하고 재배포한다. 별도 Compose override는 필요하지 않다.
설정을 생략하면 활성 프로필 미지정, `real` 모드와 `500ms` 지연 기본값을 사용한다.
기존 dev 또는 prod의 실제 전송을 유지할 때는 새 변수를 추가하지 않아도 된다.
자세한 배포 절차는 [배포 가이드](../../deploy/README.md#dev-알림-부하-테스트-sender-설정)를 따른다.

컨테이너 환경 변수 변경은 backend 컨테이너를 재생성한 뒤 적용된다. 단순 컨테이너 재시작으로는 변경된 환경 변수가 적용되지 않는다.
로컬 실행은 변경된 환경 변수를 전달해 애플리케이션을 다시 시작한다. 부하를 시작하기 전에 시뮬레이터 시작 로그와 실제 컨테이너 설정을 확인한다.
환경 표시가 `dev`라는 사실만으로 실제 DB 대상이 보장되지는 않으므로, 유효한 DB 주소가 테스트 RDS를 가리키는지도 확인한다.

## 20건 예행시험

1. 다른 사용자 작업이 섞이지 않은 테스트 DB와 큐를 준비한다. 이 모드는 해당 인스턴스가 claim하는 모든 Delivery를 모의 성공 처리하므로 기존 실제 사용자 작업과 공유하지 않는다.
2. 테스트 사용자와 활성 합성 구독을 준비하고, 공지 생성 API로 예상 Delivery 20개를 만든다. 수신자별 활성 구독 수를 합해 개수를 계산한다.
3. 예상한 20개 Delivery가 `SENT`, `attempt_count=1`이고 `sent_at`이 기록됐는지 확인한다. 기존 worker가 DB 결과를 반영한다.
4. 해당 구간의 `notification_delivery_send_seconds` 성공 히스토그램 count 합계가 20인지 확인한다. 큐 Gauge는 10초 스냅샷과 60초 scrape 사이에 짧은 적체를 놓칠 수 있다.
5. 같은 데이터 조건으로 지연을 `0`과 `500`으로 각각 설정해 재시작한 뒤 비교한다. `500ms`는 실제 Push 서비스의 평균이나 p95를 뜻하지 않는 시험 입력이다.

합성 구독은 `https://load-test.invalid/subscriptions/<고유 식별자>`와 같은 테스트 endpoint를 사용하고 키 필드도 테스트 값으로 채운다.
이 레코드는 실제 Push 구독이 아니며 실제 sender에서 사용할 수 없다. 합성 데이터의 유효성 검사와 DB 제약은 지켜야 한다.

## 종료

시뮬레이터 상태에서 실행 중인 테스트 작업이 종료된 것을 확인하고 합성 구독 및 테스트 데이터를 정리한다.
남은 테스트 Delivery와 합성 구독을 실제 sender가 처리하게 두지 않는다.
그다음 `load-test` 프로필을 제거하고 `WEB_PUSH_SENDER_MODE=real`로 설정해 backend 컨테이너를 재생성한다.
모드를 생략해도 기본값인 실제 sender를 사용한다. 다른 Spring 프로필을 사용 중이라면 해당 프로필은 유지한다.
시뮬레이터의 `SENT`와 `outcome=success`는 모의 성공이며 실제 브라우저 수신을 증명하지 않는다.
