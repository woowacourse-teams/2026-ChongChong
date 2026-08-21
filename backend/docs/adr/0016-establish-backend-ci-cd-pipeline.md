# 0016. AWS 관리형 서비스로 백엔드 CI/CD 파이프라인을 구성한다

- 날짜: 2026-08-21
- 관련 이슈: [#83](https://github.com/woowacourse-teams/2026-ChongChong/issues/83)
- 후속 ADR: [0017. 개인 AWS 계정에 임시 개발 배포 환경을 구성한다](0017-use-personal-aws-account-for-temporary-development-deployment.md)

## 배경

백엔드는 PostgreSQL RDS와 같은 VPC에 있는 단일 Ubuntu EC2 `t4g.micro`에서 운영할 예정이다. EC2는 외부로
통신할 수 있지만 SSH 22번 포트는 특정 IP에서만 접근할 수 있으므로 GitHub-hosted runner가 EC2에 직접 SSH하여
배포할 수 없다. EC2에는 아직 Docker, Nginx와 배포 Agent가 설치되어 있지 않으며 최초 한 번은 허용된 위치에서
SSH로 접속해 준비할 수 있다.

개발 기간에는 서버 한 대를 유지하고 배포 중 짧은 서비스 중단을 허용한다. 운영 단계에서 요구할 무중단 배포를 지금
단일 서버에 억지로 구성하면 메모리 여유가 작은 `t4g.micro`에서 두 애플리케이션 컨테이너를 동시에 실행해야 하고,
배포 복잡도도 먼저 증가한다.

프론트엔드와 백엔드는 같은 저장소와 브랜치를 사용한다. 프론트엔드 변경만으로 백엔드를 다시 배포하지 않되,
백엔드 실행 파일과 Nginx, Docker Compose 또는 배포 절차가 달라지면 `dev` 병합 후 자동으로 EC2에 반영해야 한다.

## 결정

### CI와 CD의 책임을 분리한다

- GitHub Actions는 백엔드 변경의 CI를 담당한다.
- CI는 `./gradlew test bootJar`를 실행하여 Gradle 테스트와 실행 가능한 JAR 생성 여부를 함께 확인한다.
- CI는 Docker 이미지를 만들거나 AWS와 Docker Hub 배포 자격 증명을 사용하지 않는다.
- AWS CodePipeline은 `dev` 브랜치 push를 배포 시작점으로 사용한다.
- CodePipeline은 GitHub Connection으로 변경을 감지하고 CodeBuild와 CodeDeploy를 순서대로 실행한다.
- CodeBuild는 `dev`에 확정된 커밋으로 테스트와 JAR 빌드를 다시 수행하고 Docker 이미지를 만든다.
- 팀 AWS의 `t4g.micro`에서는 `linux/arm64` 이미지를 사용한다. 개인 AWS의 임시 x86_64 환경에서는
  ADR-0017에 따라 `linux/amd64` 이미지를 사용하며, 빌드 platform은 배포 대상 EC2 아키텍처와 일치시킨다.
- 배포 이미지는 변경 불가능한 Git commit SHA와 추적하기 쉬운 `dev` 태그를 Docker Hub에 push한다.
- CodeDeploy는 단일 EC2에서 기존 컨테이너를 새 이미지로 교체하는 in-place 배포를 수행한다.

CI와 CodeBuild에서 JAR을 각각 만드는 중복을 허용한다. PR에서 검증한 임시 결과가 아니라 `dev`에 실제로 병합된
커밋으로 배포 이미지를 재현하기 위한 중복이다.

### 배포 감지 범위를 백엔드 운영 파일로 제한한다

다음 경로 중 하나가 변경된 `dev` push에서 배포 파이프라인을 실행한다.

- `backend/**`
- `deploy/**`
- `buildspec.yml`
- `appspec.yml`

현재 선택은 구현 단순성을 우선하여 위 파일 중 하나만 변경되어도 JAR과 Docker 이미지를 모두 다시 만든다.
프론트엔드 파일만 바뀐 경우에는 백엔드 파이프라인을 실행하지 않는다.

### Docker Hub를 이미지 Registry로 사용한다

- CodeBuild는 쓰기 권한이 있는 Docker Hub Access Token으로 이미지를 push한다.
- EC2는 pull에 필요한 최소 권한의 Docker Hub Access Token을 사용한다.
- `latest`만으로 배포하지 않고 CodeBuild가 제공하는 Git commit SHA를 이미지 태그와 배포 변수로 전달한다.
- Registry 자격 증명은 Git에 저장하지 않는다.
- CodeBuild용 Docker Hub username과 push Token은 Systems Manager Parameter Store의 표준 파라미터로 저장한다.
- username은 `String`, Token은 `SecureString`으로 저장하고 CodeBuild Service Role에는 해당 파라미터를 읽는
  `ssm:GetParameters` 권한만 부여한다.

Parameter Store 조회는 CodeBuild가 AWS API로 수행하므로 EC2를 Systems Manager 관리형 노드로 등록하거나 SSM
Agent를 설치할 필요가 없다. EC2의 런타임 환경 변수와 pull 자격 증명은 `/opt/chongchong/.env`에 두고 파일 권한을
제한한다.

### CodeDeploy Agent가 배포 명령을 실행한다

- EC2에는 Docker Engine, Docker Compose Plugin과 CodeDeploy Agent를 최초 한 번 설치한다.
- EC2에 CodeDeploy용 Instance Profile을 연결하고 필요한 S3 읽기 권한만 부여한다.
- CodeDeploy Agent가 AWS 서비스로 outbound HTTPS 통신하므로 GitHub나 AWS 빌드 서버를 위해 22번 포트를 열지 않는다.
- 배포 스크립트는 Docker Hub 로그인, 이미지 pull, Compose 교체와 HTTPS health check를 수행한다.
- health check는 Nginx의 TLS 종료를 거친 `/actuator/health`가 2xx로 응답하는 경우에만 성공 처리한다.
- health check가 실패하면 배포를 실패 처리하고 직전에 실행하던 이미지 태그로 복구를 시도한다.

### Nginx와 인증서의 책임을 분리한다

- Nginx는 Docker Compose로 실행하여 설정과 버전을 저장소에서 관리한다.
- Nginx는 80번 요청을 HTTPS로 전환하고 443번 요청을 백엔드 컨테이너로 전달한다.
- 실제 도메인을 준비하기 전에는 EC2 공개 IP에 대응하는 `nip.io` 호스트 이름을 사용한다.
- Certbot은 Ubuntu Host에 설치하고 인증서와 ACME webroot를 Nginx 컨테이너에 읽기 전용으로 마운트한다.
- 최초 인증서 발급과 자동 갱신 설정은 EC2 bootstrap 절차에 포함한다.

### 애플리케이션 비밀은 EC2 파일에서 주입한다

- DB, JWT, 프론트엔드 URL과 Docker Hub pull 자격 증명을 `/opt/chongchong/.env`에 저장한다.
- `.env`는 저장소에 커밋하지 않고 EC2에서 소유자만 읽을 수 있게 관리한다.
- RDS의 5432번 inbound source가 EC2에 연결된 Security Group인지 배포 전에 확인한다.

## 선택 이유

CodePipeline, CodeBuild와 CodeDeploy를 사용하면 GitHub-hosted runner의 변하는 IP를 EC2 SSH 규칙에 추가하지 않고도
병합부터 배포까지 자동화할 수 있다. 배포 Agent는 EC2에서 AWS로 나가는 통신을 사용하므로 현재의 제한된 SSH 정책을
유지할 수 있다. GitHub 연결과 배포 상태도 AWS에서 함께 추적할 수 있다.

CI에는 비밀이 필요 없는 Gradle 테스트와 JAR 빌드를 남겨 PR 피드백을 제공한다. 배포 이미지는 CodeBuild에서 테스트와
JAR 빌드를 다시 수행해 `dev` 커밋과 이미지 SHA의 대응을 보장한다. 팀 AWS에서는 ARM64 이미지를 만들면
`t4g.micro`가 에뮬레이션 없이 실행할 수 있고, 개인 AWS의 임시 환경에서는 AMD64 이미지를 만들어 x86_64 EC2와
호환한다.

Docker Hub는 별도의 AWS Registry 권한을 추가하지 않아도 되고 현재 팀이 사용할 수 있는 외부 Registry다. 이미지와
소스가 서로 다른 서비스에 존재하고 토큰을 관리해야 하는 비용은 감수한다.

## 검토한 대안

### GitHub Actions에서 이미지까지 빌드하고 직접 배포

GitHub Actions 무료 실행 시간을 활용할 수 있지만 AWS를 호출할 OIDC Role이나 장기 Access Key가 필요하다. 현재 팀이
IAM을 직접 제어하기 어렵고 장기 키의 만료와 권한 변경에 의존하지 않기 위해 선택하지 않았다.

### GitHub Actions에서 EC2로 SSH

구성이 단순하지만 GitHub-hosted runner의 출발지 IP를 현재 SSH Security Group에 허용할 수 없다. 22번 포트를 넓게
개방하는 것은 보안상 허용하지 않는다.

### AWS Systems Manager Run Command

외부 SSH 없이 명령을 실행할 수 있지만 현재 EC2는 Systems Manager 관리형 노드가 아니며 필요한 역할과 관리 상태를
확보하지 못했다. 사용할 수 있다고 확정된 CodeDeploy 경로를 선택했다.

### CodeBuild에서 EC2 사설 IP로 SSH

공개 SSH를 피할 수 있지만 CodeBuild의 VPC 연결, Security Group, SSH 키와 인터넷 접근을 위한 NAT 구성이 필요하다.
CodeDeploy Agent보다 네트워크와 비밀 관리가 복잡해 선택하지 않았다.

### ECR 또는 GHCR 사용

ECR은 AWS 권한과 수명 짧은 인증을 활용할 수 있지만 현재 사용할 수 있다고 확인된 서비스 범위에 포함되지 않는다.
GHCR은 소스와 이미지를 GitHub에서 함께 관리할 수 있지만 GitHub PAT의 수명과 권한 변경이 배포에 미치는 영향을
피하고자 선택하지 않았다.

### Docker Hub Token을 Secrets Manager에 저장

Secret 수명 주기와 교체 관리 기능이 더 풍부하지만 개발 환경의 Docker Hub Token 하나를 위해 Secret 저장 비용과
별도 권한 구성을 추가해야 한다. 현재는 자동 교체가 필요하지 않고 비용을 낮추는 것이 더 중요하므로 표준
Parameter Store `SecureString`을 사용한다.

### 지금 무중단 배포 구성

Blue/Green 배포나 두 컨테이너 전환은 중단을 줄이지만 단일 `t4g.micro`의 자원을 더 사용하고 초기 운영 복잡도를
높인다. 개발 기간에는 짧은 중단을 수용하고 운영 전 별도 ADR로 결정한다.

### Host에 Nginx 설치

구성 파일이 서버에만 남으면 재현과 변경 검토가 어렵다. Nginx 자체는 컨테이너로 관리하고 Host에는 인증서 발급과
갱신에 필요한 Certbot만 설치한다.

## 영향

### 긍정적 영향

- `dev`의 배포 대상 변경만 자동으로 EC2에 반영한다.
- GitHub와 EC2 사이에 공개 SSH 경로를 만들지 않는다.
- 커밋 SHA로 실행 중인 이미지와 원본 소스를 추적할 수 있다.
- Nginx, Compose와 배포 절차가 코드 리뷰와 버전 관리 대상이 된다.
- CI와 CD의 책임 및 자격 증명 경계가 분리된다.

### 부정적 영향과 위험

- CodePipeline, CodeBuild와 CodeDeploy용 Service Role 및 EC2 Instance Profile을 인프라 담당자가 준비해야 한다.
- GitHub Actions와 CodeBuild가 JAR을 각각 만들어 빌드 시간이 중복된다.
- 단일 EC2 in-place 배포 중에는 짧은 서비스 중단이 발생한다.
- Docker Hub 장애, pull 제한과 Access Token 변경이 배포 성공에 영향을 준다.
- 고정되지 않은 EC2 공개 IP가 바뀌면 `nip.io` 호스트 이름과 인증서를 다시 구성해야 한다.
- CodeDeploy가 성공해도 데이터베이스 스키마 변경의 하위 호환성을 자동으로 보장하지 않는다.

## 미확정 사항

- 인프라 담당자가 제공할 CodePipeline, CodeBuild, CodeDeploy Service Role과 EC2 Instance Profile의 정확한 이름
- EC2의 Docker Hub pull Token을 개인 계정과 조직 계정 중 어디에서 발급할지
- EC2 공개 IP와 이에 대응하는 `nip.io` 서버 이름
- RDS Security Group의 5432번 source가 EC2 Security Group으로 제한되어 있는지
- 운영 전 EC2 크기 조정, ELB·ACM 도입과 무중단 Blue/Green 배포 방식

## 후속 작업

- 백엔드 테스트와 `bootJar`를 실행하는 GitHub Actions workflow를 추가한다.
- 대상 EC2 아키텍처에 맞는 백엔드 Docker 이미지와 Nginx Docker 구성을 추가한다.
- 개발 서버용 Docker Compose와 EC2 bootstrap·배포 스크립트를 추가한다.
- CodeBuild `buildspec.yml`과 CodeDeploy `appspec.yml`을 추가한다.
- CodePipeline의 GitHub Connection, `dev` 브랜치와 경로 필터를 AWS에서 구성한다.
- 인프라 담당자와 Service Role, Instance Profile 및 Docker Hub 비밀 주입 방식을 확인한다.
