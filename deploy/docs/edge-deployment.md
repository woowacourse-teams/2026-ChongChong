# A/B 배포하기

서버 설치가 끝나고 edge를 통해 활성 백엔드의 health 응답을 받을 수 있는 상태에서 시작한다. 처음 구성하는 서버는 [서버 준비](server-setup.md)를 먼저 진행한다.

## 1. 파이프라인을 연결한다

**실행 위치: AWS 콘솔. 이미 구성된 환경은 2번으로 이동한다.**

### 1-1. CodeBuild 프로젝트를 설정한다

**CodeBuild → 빌드 프로젝트 → 대상 프로젝트 → 편집**에서 아래 항목을 확인한다. 새 환경은 기존 정상 빌드 프로젝트의 환경 설정을 기준으로 만든다.

| 설정 화면 | 값 |
| --- | --- |
| 소스 | CodePipeline |
| 환경 → 이미지 | Corretto 25와 ARM64 이미지 빌드를 지원하는 환경. 기존 프로젝트의 이미지·아키텍처 설정 확인 |
| 환경 → 추가 구성 | Docker 빌드용 privileged mode 활성화 |
| Buildspec | 저장소의 buildspec.yml |
| 아티팩트 | CodePipeline |

**환경 → 환경변수**에 아래 값을 입력한다.

| 이름 | 유형 | 값 |
| --- | --- | --- |
| DEPLOYMENT_MODE | 일반 텍스트 | edge |
| DOCKERHUB_REPOSITORY | 일반 텍스트 | 실제 이미지 저장소. 예: docker.io/팀/백엔드 |
| DOCKERHUB_USERNAME | Parameter Store | 사용자명이 저장된 파라미터 경로 |
| DOCKERHUB_TOKEN | Parameter Store | push 토큰이 저장된 파라미터 경로 |

Parameter Store 값은 **Systems Manager → Parameter Store → 파라미터 이름**에서 확인한다. CodeBuild의 서비스 역할에는 이 경로를 읽는 ssm:GetParameters 권한이 필요하다. SecureString에 고객 관리 KMS 키를 썼다면 kms:Decrypt 권한도 확인한다.

> [!IMPORTANT]
> DEPLOYMENT_MODE가 비어 있으면 legacy 배포가 선택된다. A/B 구성에서는 저장 후 값이 **edge**인지 다시 확인한다.

### 1-2. CodeDeploy가 edge를 선택하게 한다

1. **EC2 → edge 인스턴스 → 태그 관리**에서 환경과 역할을 구분할 태그를 확인한다.
2. **CodeDeploy → 애플리케이션**에서 EC2/온프레미스 유형의 애플리케이션을 연다. 없으면 같은 유형으로 만든다.
3. **배포 그룹 생성**에서 환경별 이름을 입력하고 CodeDeploy 서비스 역할을 선택한다.
4. 배포 유형은 **In-place**, 환경 구성은 **Amazon EC2 인스턴스**를 선택한다.
5. 태그 조건을 edge의 태그와 맞추고, 일치하는 인스턴스가 **edge 한 대**인지 확인한다.
6. 배포 구성은 CodeDeployDefault.AllAtOnce를 선택한다. 트래픽 전환은 edge의 Nginx가 담당하므로 이 구성에 별도 로드 밸런서를 연결하지 않는다.

CodeDeploy 서비스 역할은 배포를 관리하는 역할이고, EC2 IAM 역할은 서버가 artifact를 내려받는 역할이다. CodeDeploy 서비스 역할이 없으면 IAM의 CodeDeploy 사용 사례로 생성하고 AWSCodeDeployRole 정책을 연결한다. [AWS 역할 생성 안내](https://docs.aws.amazon.com/codedeploy/latest/userguide/getting-started-create-service-role.html).

자동 롤백을 켜면 실패 시 과거 artifact로 다시 배포한다. 아래 5번의 수동 롤백은 이전 서버로 라우팅을 되돌리는 명령이다.

### 1-3. CodePipeline을 연결한다

**CodePipeline → 파이프라인 생성 또는 기존 파이프라인 편집**에서 다음 세 단계를 연결한다.

| 단계 | 선택할 값 |
| --- | --- |
| Source | GitHub 연결, woowacourse-teams/2026-ChongChong, 대상 환경 브랜치 |
| Build | 1-1의 CodeBuild 프로젝트. Source 출력을 입력으로 사용 |
| Deploy | 1-2의 CodeDeploy 애플리케이션·배포 그룹. Build 출력을 입력으로 사용 |

GitHub 연결이 없으면 Source 단계의 연결 생성으로 저장소 접근을 연결한다. 파이프라인 서비스 역할에는 선택한 연결·CodeBuild·CodeDeploy·artifact S3 접근 권한이 필요하다.

파이프라인 유형은 **V2**, 실행 모드는 **QUEUED**로 설정한다. 생성 화면의 서비스 역할은 새 역할 생성 또는 기존 역할 선택으로 연결한다. [AWS 파이프라인 생성 안내](https://docs.aws.amazon.com/codepipeline/latest/userguide/pipelines-create.html).

Source의 push 트리거는 대상 환경 브랜치와 아래 경로를 포함한다.

~~~text
backend/**
deploy/**
buildspec.yml
appspec.yml
appspec.edge.yml
~~~

**여기까지의 확인 결과:** Source → Build → Deploy가 표시되고, Build는 DEPLOYMENT_MODE=edge인 프로젝트, Deploy는 edge 한 대를 선택한 배포 그룹에 연결돼 있어야 한다.

> [!NOTE]
> 새 파이프라인은 생성 직후 실행될 수 있다. [서버 준비](server-setup.md)의 HTTPS health 확인까지 마친 뒤 생성한다.
> 기존 파이프라인에 편집 내용을 저장하는 경우에도 다음 실행이 새 설정을 사용한다.

## 2. 배포 전 현재 대상을 확인한다

**실행 위치: edge의 SSM 터미널.** 아래 명령은 root 셸에서 실행한다.

~~~bash
sudo -i
source /opt/chongchong/config/edge.env
cat /opt/chongchong/edge-routing/upstream.conf
curl -fsS --resolve "$SERVER_NAME:443:127.0.0.1" -D - \
  "https://$SERVER_NAME/actuator/health"
~~~

본문의 status가 UP이고 X-Deploy-Backend가 upstream의 주소와 같아야 한다. A 주소가 나오면 이번 배포 대상은 B, B 주소가 나오면 대상은 A다.

응답이 없으면 docker logs --tail 100 chongchong-nginx를 확인한다. 앱 오류라면 응답을 담당하는 백엔드에서 docker logs --tail 100 chongchong-backend를 확인한다.

## 3. 배포를 시작한다

**일상 배포:** CodePipeline에서 파이프라인을 열고 **변경 사항 릴리스(Release change)**로 실행한다. 실행 상세의 Source commit이 배포할 commit과 같은지 확인한다. Source → Build → Deploy가 차례로 Succeeded가 되면 완료다.

Build가 실패하면 해당 단계의 **세부 정보 → 빌드 로그**, Deploy가 실패하면 **세부 정보 → 배포 ID → 인스턴스 → 이벤트**에서 실패한 hook 로그를 연다.

**현재 이미지로 수동 재배포:** 파이프라인이 실행 중이지 않을 때 edge에서 실행한다.

~~~bash
cat /opt/chongchong/deploy/image.env
bash /opt/chongchong/deploy/scripts/edge/edge-deploy.sh
~~~

수동 명령은 image.env에 적힌 이미지를 사용한다. 새 코드를 빌드하려면 파이프라인을 실행한다.

A가 활성인 경우 스크립트는 다음 순서로 동작한다.

~~~mermaid
sequenceDiagram
    participant E as edge
    participant A as backend A
    participant B as backend B
    participant N as Nginx
    E->>N: 현재 A 응답 확인
    E->>B: 설정·이미지 정보·Compose 전달
    B->>B: 이미지 pull 후 앱 교체
    E->>B: readiness 확인
    alt UP
        E->>N: B로 upstream 변경 후 reload
        E->>N: HTTPS 응답과 B 주소 확인
        alt 확인 실패
            E->>N: A 라우팅 복원
        end
    else 기동 실패
        Note over E,A: A로 요청을 계속 전달
    end
~~~

성공하면 Deployment verified on backend b가 출력된다. 이전 A 앱은 기존 요청 처리와 롤백을 위해 계속 실행된다.

## 4. 전환 결과를 확인한다

**실행 위치: edge.** 2번의 health 명령을 다시 실행한다. 이번에는 새 백엔드 주소가 나와야 한다.

다음은 **로컬 PC 터미널**에서 실행한다. 실제 사용자가 접근하는 HTTPS 주소를 입력한다.

~~~bash
read -r -p '서비스 URL (예: https://api.example.com): ' SERVICE_URL
curl -fsS --connect-timeout 3 --max-time 10 -D - "$SERVICE_URL/actuator/health"
~~~

HTTP 200과 status UP을 확인한다. CloudFront를 쓰는 경우 이 요청은 CloudFront를 포함한 사용자 경로를 확인한다. 마지막으로 서비스에서 로그인과 주요 조회 API를 직접 실행해 본다.

CloudWatch에서는 A/B별 요청량이 이동했는지, 전체 오류율·응답 시간이 변했는지 확인한다. 지표를 찾는 순서는 [모니터링 확인](monitoring.md)에 있다.

## 5. 실패했을 때 복구한다

| 출력·상황 | 확인할 곳과 조치 |
| --- | --- |
| Candidate not ready | 대상 백엔드의 앱 로그에서 DB·설정·기동 오류 확인. 기존 활성 서버로 요청은 계속 전달된다. |
| Previous Nginx workers are still draining | edge에서 docker top chongchong-nginx -eo pid,args 확인. 이전 연결을 처리하는 worker가 종료된 뒤 재시도한다. |
| Interrupted switch detected | 아래 롤백 명령으로 중단된 전환부터 복구한다. |
| 전환 후 HTTPS 검증 실패 | 스크립트가 이전 경로 복원을 시도한다. 2번 명령으로 현재 응답을 확인한다. |

직전 서버로 돌아가려면 edge에서 실행한다.

~~~bash
bash /opt/chongchong/deploy/scripts/edge/edge-rollback.sh
~~~

이 명령은 이전 서버의 readiness를 확인한 뒤 라우팅을 되돌린다. 복구 후 2번과 4번으로 내부·외부 응답을 확인한다.

복구할 주소는 edge-state/previous-upstream.conf에 있다. 전환이 중단된 경우에는 pending-upstream.conf를 우선 사용한다. 복구 검증에 실패하면 pending 파일이 남고 다음 배포가 차단된다. 이때는 앱·Nginx 오류를 해결한 뒤 롤백 명령을 다시 실행한다.

다음 배포에서 대기 서버를 새 버전으로 덮기 시작하면 그 서버의 이전 버전 롤백 기록은 해제된다. CodeDeploy 자동 롤백은 과거 artifact로 다시 배포하는 방식이며, 앱 설정은 현재 backend.env를 읽는다.

## 앱 설정을 바꿀 때

**실행 위치: edge.**

~~~bash
sudoedit /opt/chongchong/config/backend.env
bash /opt/chongchong/deploy/scripts/edge/edge-deploy.sh
~~~

한 번 배포하면 대기 서버 하나에 새 설정이 적용된다. A/B 모두 반영하려면 첫 전환을 확인하고 이전 연결이 끝난 뒤 한 번 더 배포한다. JWT·DB 설정처럼 두 버전이 함께 사용하는 값은 전환 중에도 양쪽 앱에서 동작해야 한다.

## 파일은 어디에 남는가

| 경로 | 내용 |
| --- | --- |
| edge의 /opt/chongchong/config/edge.env | 주소·도메인·SSH 설정 |
| edge의 /opt/chongchong/config/backend.env | A/B에 전달할 앱 설정 원본 |
| edge의 /opt/chongchong/deploy/image.env | 이번 이미지와 태그 |
| edge의 /opt/chongchong/edge-runtime/ | 실행 중인 Nginx·ADOT 설정 |
| edge의 /opt/chongchong/edge-routing/upstream.conf | 활성 백엔드 주소 |
| edge의 /opt/chongchong/edge-state/ | 배포별 설정 사본과 복구 주소 |
| A/B의 /opt/chongchong/releases/release.*/ | 전달받은 설정·이미지 정보·Compose |
| A/B의 /opt/chongchong/current-backend-release | 앱 시작에 사용한 release |

앱 배포는 Nginx·ADOT를 재생성하지 않는다. 수집기 설정 변경은 [모니터링 문서](monitoring.md)의 적용 명령을 사용한다. Nginx의 연결 한도나 마운트 변경은 컨테이너 재생성이 필요하므로 서비스 전환과 별도로 작업 시간을 잡는다.

edge의 배포 사본은 기본 3개를 보관한다. EDGE_DEPLOYMENT_RETENTION_COUNT로 개수를 바꿀 수 있다. 마지막 성공 사본과 복구 중인 자료는 보존한다. 백엔드의 과거 성공 release와 rollback 디렉터리는 자동 정리하지 않는다.
