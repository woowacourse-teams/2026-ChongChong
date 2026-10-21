# edge와 백엔드 A/B 무중단 배포

## 범위와 실행 흐름

CodeDeploy가 환경별 edge 한 대에서 `edge-deploy.sh`를 실행한다. edge는 현재 라우팅을
확인하고 비활성 A/B에 설정과 배포 파일을 SSH로 전달한다. 새 백엔드 readiness가 UP이면
Nginx 설정을 검사하고 reload한 다음 HTTPS 응답의 실제 대상까지 검증한다.

기본 배포는 기존 단일 서버 방식이다. CodeBuild에 `DEPLOYMENT_MODE=edge`를 지정해야
`appspec.edge.yml`이 배포 artifact의 `appspec.yml`로 선택된다. 빌드·이미지 push는 그대로 유지한다.
이 코드를 반영하는 것만으로 AWS 배포 그룹, 서버 설정, EIP가 변경되지는 않는다.
이번 범위는 dev 검증까지이며 prod 적용과 EC2 자동 시작·중지는 포함하지 않는다.

## 설정의 원본과 서버 사본

팀원은 edge에 개인 SSH 키로 접속해 다음 파일을 `sudoedit`으로 수정한다.

| 파일 | 용도 |
| --- | --- |
| `/opt/chongchong/config/backend.env` | 같은 환경의 A/B가 공유할 앱 설정 원본 |
| `/opt/chongchong/config/edge.env` | A/B 사설 IP, 배포 SSH 키 경로, HTTPS 도메인 |

두 파일은 root:root 600, config 디렉터리는 700으로 관리한다. `edge.env.example`을
참고하되 dev와 prod의 파일·키·도메인은 분리한다. backend.env는 Nginx와 수집기 컨테이너에
마운트하지 않는다. 설정은 Bash에서도 읽으므로 셸 문법에 맞춰 작성하고 신뢰하는 관리자만 수정한다.

배포는 원본을 `edge-state/deployment.*`에 스냅샷으로 남긴 뒤 대상 백엔드의
`releases/release.*`에 전달한다. 같은 release의 이미지 태그·Compose·설정을 사용한다.
파일을 원본에서 지웠거나 전송에 실패하면 새 배포를 중단한다. 실행 중인 앱은 원본 파일을
실시간으로 읽지 않으므로 수정만으로 동작이 바뀌지는 않는다.

새 모드에서 백엔드가 읽는 파일은 release의 `backend.env`다. 기존 `/opt/chongchong/.env`는
새 모드의 기준 파일이 아니다. `current-backend-release` 심볼릭 링크로 현재 release를 찾을 수 있다.
실패한 후보 release와 이전 release를 자동 삭제하지 않는다. 비밀값이 포함되므로 디렉터리의
root 전용 권한을 유지하고, 롤백 대상이 아닌 오래된 사본을 운영자가 보존 정책에 따라 정리한다.

## 최초 전환 전에 준비할 것

1. dev edge → A/B의 사설 IP 8080 연결, RDS 연결, 양쪽의 awslogs 권한을 확인한다.
   백엔드 8080은 edge 보안 그룹에서만 허용한다. 새 Compose는 각 백엔드 사설 IP에만 포트를 게시한다.
2. edge와 A/B에 Docker·Compose, Bash·tar·flock이 필요하다. edge에는 curl, Python 3,
   OpenSSH 클라이언트와 GNU timeout도 필요하다. CodeDeploy 에이전트는 edge에 설치한다.
3. 배포 전용 SSH 키와 known_hosts를 edge에 설치한다. A/B의 호스트 키 지문을 별도로
   확인한 뒤 등록한다. StrictHostKeyChecking을 끄거나 개인 개발자 키를 복사하지 않는다.
   현재 구현은 배포 계정의 비대화형 sudo가 필요해 배포 키가 backend root 권한을 갖는다.
   A/B의 SSH 접근을 edge로 제한하고 키를 root 전용으로 보관한다. 이 접근 권한 설정은 별도 승인 후 진행한다.
4. 기존 edge의 Nginx·인증서·수집기 설정을 백업한다. 인증서 갱신과 실제 화면·로그인 동작을
   확인하고 EIP 이전 및 기존 dev의 B 전환을 별도 작업으로 수행한다.
5. `/opt/chongchong/edge-runtime`에 아래 파일을 설치한다. artifact는 `deploy/`에 복사되므로
   이후 앱 배포가 Nginx·수집기의 실행 설정을 덮어쓰지 않는다.

   | 저장소 파일 | edge의 파일 |
   | --- | --- |
   | `nginx/edge.conf.template` | `edge-runtime/edge.conf.template` |
   | `nginx/nginx.edge.conf` | `edge-runtime/nginx.conf` |
   | `otel-collector.edge.yaml` | `edge-runtime/otel-collector.yaml` |

6. root 소유 `/opt/chongchong/edge-routing`을 만들고 현재 요청을 처리하는 백엔드 IP를
   `upstream.conf`에 입력한다. 아래는 A가 `10.0.0.189`일 때의 예다.

   ```nginx
   upstream chongchong_backend { server 10.0.0.189:8080; }
   ```

   `edge-state/`에는 잠금·설정 스냅샷이 들어간다. **Nginx에는 edge-routing 디렉터리만** 마운트한다.
   파일 단위 bind mount는 rename으로 바뀐 inode를 반영하지 못하므로 사용하지 않는다.
7. 새 `docker-compose.edge.yml`로 Nginx와 수집기를 구성한다. 최초 Nginx 구성 교체는
   컨테이너 재생성이 필요할 수 있으므로 일상적인 무중단 앱 배포와 구분해 수행한다.
   기존 컨테이너를 관리하던 Compose 프로젝트도 확인하고 중복 이름을 정리해야 한다.

   ```bash
   sudo docker compose --env-file /opt/chongchong/config/edge.env \
     -f /opt/chongchong/deploy/docker-compose.edge.yml --profile metrics config --quiet
   # 최초 구성 적용은 접속 및 복구 경로를 준비한 후 실행한다.
   sudo docker compose --env-file /opt/chongchong/config/edge.env \
     -f /opt/chongchong/deploy/docker-compose.edge.yml --profile metrics up -d
   ```

   수집기는 A/B를 서로 다른 instance로 수집한다. 백엔드 배포 스크립트는 Nginx·수집기를
   재생성하지 않는다. 전체 `down`이나 `--remove-orphans`로 기존 서비스를 지우지 않는다.
8. `/actuator/health` 응답의 `X-Deploy-Backend`가 현재 IP:8080인지 확인한다.
   스크립트는 이 값과 upstream 파일이 다르면 두 백엔드 모두 변경하지 않고 중단한다.
   이후에만 새 배포 파이프라인을 활성화한다.

## CodePipeline·CodeDeploy 설정

- 기존 서버에 붙어 있는 배포 그룹을 그대로 쓰지 않고, dev edge 한 대만 선택하는 배포 그룹을 준비한다.
  A/B를 같은 AllAtOnce 그룹에 넣지 않는다. 새 AppSpec은 edge용이며 A/B에서 직접 실행하면 안 된다.
- CodeBuild 프로젝트에 `DEPLOYMENT_MODE=edge`를 지정하고 Deploy 액션을 새 그룹에 연결한다.
- 동일 환경은 CodePipeline QUEUED 모드를 사용한다. 수동 배포와 CodeDeploy 배포를 겹치지 않는다.
  flock은 전환 스크립트의 동시 실행을 막지만 CodeDeploy의 파일 복사 단계 전체를 잠그지는 않는다.
- 이 AppSpec은 ApplicationStart 안에서 배포·전환·검증·실패 복구를 끝낸다. 기존 ValidateService의
  Nginx 로컬 네트워크 검사나 이미지 prune은 실행하지 않는다.
- CodeDeploy 자동 롤백은 이전 artifact로 스크립트를 다시 실행하는 추가 배포다. 초기 dev에서는
  비활성화하고 수동 라우팅 복구부터 검증한다. 설정 원본도 변경됐다면 이전 artifact만으로 이전
  앱 설정이 복원되지는 않는다. 즉시 복구에는 아래 `edge-rollback.sh`를 사용한다.

## 배포·설정 적용·롤백

CodeDeploy 또는 edge에서 다음을 실행한다. 수동 실행은 `deploy/image.env`에 기록된 이미지를 사용한다.

```bash
sudo bash /opt/chongchong/deploy/scripts/edge-deploy.sh
```

한 번 실행하면 비활성 슬롯 하나에만 적용하고 전환한다. 설정을 양쪽에 적용하려면 첫 배포를
검증하고 기존 요청이 끝난 뒤 다시 실행한다. 양쪽 설정을 동시에 바꾸거나 컨테이너를 동시에 끄지 않는다.

실행 순서는 다음과 같다.

1. edge 잠금, 현재 라우팅·헬스, 이전 Nginx worker의 연결 종료 여부를 확인한다.
2. 설정·이미지·Compose를 스냅샷으로 만들고 SSH 표준 입력으로 비활성 슬롯에 전달한다.
3. 대상에서 Compose 검사·이미지 pull을 끝낸 뒤 기존 비활성 backend를 graceful stop하고
   새 backend만 실행한다. backend 호스트에도 중복 실행 방지 잠금을 둔다.
4. readiness를 최대 30회, 요청당 최대 3초와 재시도 간격 2초로 확인한다. 실패하면 라우팅은 유지한다.
5. 전환 전 상태를 pending 파일에 기록한다. upstream을 원자적으로 교체하고 `nginx -t` 후 reload한다.
   문법 검사나 reload 명령이 실패하면 교체 전 upstream 파일을 복원하고 실패를 반환한다.
6. 실제 HTTPS 헬스와 upstream 주소를 최대 10회 확인한다. 실패하면 이전 upstream으로 복구하고
   복구 응답도 검증한다. 실패 결과는 CodeDeploy에 실패로 전달한다.
7. 성공해도 이전 backend는 끄지 않는다. 기존 요청 처리와 즉시 롤백을 위해 다음 배포 전까지 유지한다.
   다음 배포에서 대기 슬롯을 교체하기 직전에 이전 롤백 대상을 무효화한다. 그 배포가 실패하면
   기존 활성 backend는 유지되지만, 덮어쓴 대기 슬롯을 이전 버전이라고 간주해 롤백하지 않는다.
   이전 Nginx worker가 연결 종료를 기다리는 동안 다음 배포는 거절한다. 강제 drain 시간은 두지 않는다.

직전 backend로 트래픽만 되돌리려면:

```bash
sudo bash /opt/chongchong/deploy/scripts/edge-rollback.sh
```

이 명령은 기존 backend의 readiness를 확인한 뒤 라우팅만 복구한다. 이미지를 다시 내려받거나
설정 원본을 다시 읽어 앱을 재생성하지 않는다. 배포 중 프로세스가 종료돼 pending 파일이 남으면
새 배포를 막고 이 명령으로 복구하도록 한다. SIGKILL·EC2 장애 순간의 자동 복구까지 보장하지 않는다.
복구 후에도 검증에 실패하면 pending 상태를 유지하고 수동 점검을 요구한다.

## 제약과 검증

구·신버전은 같은 DB를 동시에 사용한다. 스키마 변경은 구버전 호환성을 유지해야 하며,
파괴적 마이그레이션은 이미지 롤백만으로 복구되지 않는다. 알림·스케줄러는 HTTP 라우팅과 무관하게
양쪽에서 실행될 수 있으므로 다중 인스턴스 중복 처리 여부를 dev에서 확인한다.

```bash
python3 -m unittest discover -s deploy/tests -v
bash deploy/tests/nginx-smoke.sh
bash deploy/tests/edge-nginx-smoke.sh
```

단위 테스트는 설정 전송·pull·readiness 실패, 잘못된 활성 상태, 중복 실행, 전환 실패,
중단된 배포 및 수동 롤백을 확인한다. Docker 테스트는 실제 Nginx A → B → A 전환 중
연속 HTTP 150건과 4초 요청의 완료를 확인한다. 이는 실제 Spring·RDS·AWS에서의 검증을 대신하지 않는다.

dev 배포 전 체크:

- [ ] edge 인증서 자동 갱신, 외부 로그인·화면 조회 확인
- [ ] EIP 이전 및 기존 dev의 B 전환, SSH 별칭·외부 통신 경로 확인
- [ ] 팀원 접근 권한, 배포 키·known_hosts, 원본 파일 권한 확인
- [ ] 새 CodeDeploy 대상·QUEUED 실행·AppSpec 선택 확인
- [ ] 연속 요청 중 정상 배포, readiness 실패, 전환 실패, 수동 롤백 검증
- [ ] 긴 HTTP 요청 및 알림 작업의 종료·중복 처리 검증
- [ ] CloudWatch에서 A/B 메트릭 및 각 로그 스트림 확인

prod는 위 dev 검증이 완료된 뒤 별도 적용한다.
