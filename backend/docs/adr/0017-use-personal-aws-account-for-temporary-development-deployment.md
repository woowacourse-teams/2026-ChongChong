# 0017. 개인 AWS 계정에 임시 개발 배포 환경을 구성한다

- 날짜: 2026-08-21
- 관련 이슈: [#83](https://github.com/woowacourse-teams/2026-ChongChong/issues/83)
- 관련 ADR: [0016. AWS 관리형 서비스로 백엔드 CI/CD 파이프라인을 구성한다](0016-establish-backend-ci-cd-pipeline.md)

## 배경

ADR-0016은 팀에서 제공한 EC2에 최초 한 번 SSH로 접속하여 Docker와 CodeDeploy Agent를 설치할 수 있다는 전제로
작성했다. 실제로는 22번 포트가 사전에 등록된 네트워크에서만 열려 있고, 현재 그 네트워크를 사용할 수 없으며 새로운
접속 IP도 Security Group에 추가할 수 없다. EC2에는 IAM Instance Profile도 연결되어 있지 않아 Systems Manager나
CodeDeploy를 최초 설정 통로로 사용할 수 없다.

CodeDeploy Agent가 설치되지 않은 EC2에는 CodeDeploy가 명령을 전달할 수 없으므로 CodePipeline이나 CodeBuild만으로
현재 인스턴스를 준비할 수 없다. 팀 계정의 권한이나 접속 조건이 바뀔 때까지 기다리면 프론트엔드가 사용할 HTTPS
백엔드 개발 환경과 자동 배포 검증이 함께 지연된다.

초기에는 `dev` push 이후 CodePipeline 실행이 생성되지 않아 GitHub push 트리거 연결 문제를 의심했다. 그러나 개인
AWS 계정에서 구성한 CodeConnections 기반 트리거는 정상적으로 자동 배포를 시작했다. 팀 환경에서 파이프라인을
구성하고 검증하지 못한 원인은 저장소 설정이 아니라 CodeConnections, CodePipeline과 관련 IAM Role을 생성·수정할
AWS 권한이 없었던 것이다. 따라서 저장소에서 별도의 자동 배포 트리거를 추가하지 않고, 팀 계정 권한이 준비되면 개인
계정에서 검증한 구성을 같은 조건으로 재생성한다.

## 결정

- 팀 AWS 계정에서 필요한 접근 권한을 확보할 때까지 개인 AWS 계정에 임시 백엔드 개발 배포 환경을 구성한다.
- 리전은 기존 설계와 같은 서울 `ap-northeast-2`를 사용한다.
- ADR-0016에서 결정한 `GitHub Actions CI -> CodePipeline -> CodeBuild -> Docker Hub -> CodeDeploy -> EC2` 구조와
  저장소의 배포 파일을 그대로 사용한다.
- 개인 계정에서는 CodeDeploy Service Role, CodeBuild Service Role과 CodePipeline Service Role을 최소 권한으로 직접
  구성한다.
- EC2 Instance Profile은 첫 배포부터 AWS 관리형 `AmazonS3ReadOnlyAccess`를 사용하지 않는다.
- CodePipeline artifact 버킷과 CodeDeploy가 실제로 읽는 서울 리전 S3 리소스만 허용하는 사용자 지정 읽기 정책을
  연결한다.
- CodeBuild의 Docker Hub username은 표준 `String` 파라미터 `/chongchong/dev/dockerhub/username`, push Token은 표준
  `SecureString` 파라미터 `/chongchong/dev/dockerhub/token`으로 저장한다.
- 개인 계정에 PostgreSQL RDS를 만들고 EC2와 같은 VPC에서 사용한다.
- RDS는 공개 접근을 비활성화하고 5432번 inbound source를 백엔드 EC2의 Security Group으로 제한한다.
- 개인 계정의 EC2는 Ubuntu 26.04 LTS `x86_64` 환경을 사용한다.
- 개인 환경의 CodeBuild는 `linux/amd64` Docker 이미지를 만들고 x86_64 EC2에 배포한다.
- CodeDeploy Agent는 Ruby 의존성이 없고 Ubuntu 26.04를 지원하는 v2를 사용한다.
- 팀 AWS 환경에 접근할 수 있게 되면 ADR-0016의 `t4g.micro/ARM64` 결정을 다시 적용하고 이미지 빌드 대상을
  `linux/arm64`로 전환한다.
- 개인 계정의 리소스는 개발 검증 전용으로 사용하며 실제 운영 데이터나 실제 사용자 비밀을 저장하지 않는다.
- 모든 리소스에는 프로젝트와 환경을 식별할 수 있는 `Project=ChongChong`, `Environment=dev` 태그를 부여한다.
- 팀 계정에서 배포 환경을 사용할 수 있게 되면 같은 저장소 설정으로 환경을 다시 만들고, 검증과 필요한 데이터 이전을
  마친 뒤 개인 계정 리소스를 제거한다.

개인 계정으로 배포 대상을 바꾸는 것은 CI/CD 도구 선택을 바꾸는 결정이 아니다. 계정 ID, 인스턴스 ID, S3 버킷과
IAM Role 이름은 저장소에 고정하지 않고 AWS 리소스 설정으로 주입한다.

개인 환경의 AMD64 선택도 팀 환경의 ARM64 결정을 폐기하지 않는다. 현재 생성되어 접근 가능한 개인 EC2를 사용해
파이프라인 검증을 먼저 완료하기 위한 임시 예외이며, 팀 환경 이전 시 CodeBuild 이미지와 Docker platform을 함께
ARM64로 되돌린다.

EC2의 S3 읽기 권한은 배포 아티팩트 수신에 필요한 경로로 제한한다. 버킷이나 객체 경로가 바뀌면 사용자 지정 정책도
함께 갱신하고, 배포와 무관한 S3 리소스는 허용하지 않는다.

Parameter Store 표준 파라미터는 개발 환경에서 추가 저장 비용 없이 CodeBuild에 비밀을 주입할 수 있다. CodeBuild가
직접 Parameter Store API를 호출하므로 개인 EC2를 Systems Manager 관리형 노드로 등록하지 않는다.

## 선택 이유

현재 배포 구현은 특정 AWS 계정에 종속된 값을 포함하지 않으므로 배포 대상 계정만 변경해 재사용할 수 있다. 개인
계정에서는 EC2 최초 설정과 IAM Role 연결을 직접 제어할 수 있어, 공유 계정에서 막힌 초기 접근 문제를 해결하면서도
이미 구현하고 검증한 CodeBuild와 CodeDeploy 절차를 유지할 수 있다.

EC2와 PostgreSQL RDS를 같은 개인 계정과 VPC에 두면 계정 간 VPC Peering, 양쪽 Route Table과 공유 계정 Security
Group 변경이 필요하지 않다. 개인 계정 안에서 RDS 접근 경로와 권한을 독립적으로 구성할 수 있다.

임시 환경에서도 운영과 같은 배포 흐름을 사용하면 Docker 이미지 생성, 배포 스크립트, Nginx와 HTTPS 구성을 실제
환경에서 검증할 수 있다. 이후 팀 계정으로 이동할 때 애플리케이션 배포 방식을 다시 변경하지 않고 AWS 리소스만
재구성할 수 있다. CPU 아키텍처는 환경별로 다르므로 팀 환경 이전 시 빌드 platform도 함께 변경한다.

## 검토한 대안

### 팀 계정의 접근 권한이 준비될 때까지 배포하지 않는다

추가 비용이나 임시 환경 이전 작업은 발생하지 않는다. 그러나 권한 변경 일정을 제어할 수 없고 백엔드 통합 및 자동 배포
검증이 멈추므로 선택하지 않았다.

### 개인 계정 EC2에 GitHub Actions가 직접 SSH한다

구성이 단순하지만 GitHub-hosted runner의 출발지 IP 범위를 SSH에 허용하거나 별도의 접속 중계 수단을 운영해야 한다.
이미 선택한 Agent 기반 배포와 달리 공개 SSH 경로 및 배포 키를 관리해야 하므로 선택하지 않았다.

### 팀 계정의 기존 PostgreSQL RDS를 사용한다

새 RDS 비용과 데이터 분리를 피할 수 있지만 개인 EC2와 사설 RDS 사이에 계정 간 VPC Peering이 필요하다. 공유 계정
관리자가 Peering 요청 승인, Route Table과 Security Group을 변경해야 하므로 현재의 인프라 제약을 다시 만들게 된다.
임시 환경을 개인 계정 안에서 독립적으로 운영하기 위해 선택하지 않았다.

### 현재 팀 EC2의 루트 볼륨을 분리하여 오프라인으로 수정한다

네트워크 접속 없이 파일을 넣을 수 있지만 인스턴스 중지, EBS 분리와 재연결 과정에서 부팅 장애가 발생할 수 있다.
아직 애플리케이션이 없는 개발 서버를 복구하기 위해 감수할 복잡도와 위험이 크므로 선택하지 않았다.

### 개인 EC2를 t4g.micro ARM64로 다시 생성한다

ADR-0016과 같은 아키텍처를 유지하고 팀 환경 이전 시 변경을 줄일 수 있다. 그러나 이미 준비한 개인 x86_64 EC2를
다시 만들고 네트워크, 환경 변수와 인증서를 재설정해야 한다. 개인 환경은 임시 검증 대상이므로 현재 EC2를 유지하고,
팀 환경에서 ARM64 결정을 다시 적용하기로 했다.

### Ubuntu 26.04에서 CodeDeploy Agent v1을 위해 Ruby 3.2를 별도 설치한다

구형 Agent를 계속 사용할 수 있지만 운영체제 기본 Ruby와 별도 버전을 관리해야 한다. CodeDeploy Agent v2가
Ubuntu 26.04와 x86_64를 지원하고 Ruby 의존성을 제거했으므로 v1을 우회하지 않고 v2를 사용한다.

## 영향

### 긍정적 영향

- 공유 계정의 SSH와 IAM 제약을 기다리지 않고 CI/CD 전체 흐름을 검증할 수 있다.
- 기존 배포 코드와 운영 방식을 바꾸지 않고 대상 계정만 이전할 수 있다.
- 개인 계정에서 최소 권한 IAM Role과 네트워크 규칙을 직접 검증할 수 있다.
- 팀 계정으로 옮길 때 필요한 AWS 리소스와 권한을 구체적으로 문서화할 수 있다.
- 공유 계정의 네트워크 변경 없이 EC2와 RDS 연결을 구성할 수 있다.

### 부정적 영향과 위험

- EC2, RDS, 공개 IPv4, CodePipeline, CodeBuild와 S3 사용 비용을 개인 계정 소유자가 부담한다.
- 개인 계정에 장애가 발생하거나 소유자가 접근할 수 없으면 팀 개발 환경도 영향을 받는다.
- 팀원이 AWS 배포 상태나 로그를 직접 확인하려면 개인 계정에 별도의 제한된 접근 권한이 필요하다.
- 팀 계정으로 이전할 때 DNS, 인증서, 비밀값과 데이터베이스 연결 정보를 다시 설정해야 한다.
- 임시 개발 환경을 위해 별도 RDS 비용이 발생하고 팀 계정 데이터와 자동으로 동기화되지 않는다.
- 임시 환경이 운영 환경처럼 장기간 유지되지 않도록 종료 조건과 리소스 제거 시점을 관리해야 한다.
- 개인 환경과 팀 환경의 CPU 아키텍처가 달라 이전 시 Docker platform과 CodeBuild 이미지를 함께 변경해야 한다.
- 사용자 지정 S3 정책의 버킷이나 객체 경로가 실제 배포 경로와 다르면 CodeDeploy가 아티팩트를 받지 못한다.

## 미확정 사항

- EC2에 Elastic IP를 연결할지, 변경 가능한 공개 IP와 `nip.io` 주소를 사용할지
- 개인 계정 리소스의 월간 비용 한도와 비용 알림 기준
- 팀 계정으로 이전할 수 있다고 판단할 IAM, 네트워크와 운영 권한의 완료 조건

## 후속 작업

- 개인 계정에서 EC2, Security Group과 필요한 IAM Role을 생성한다.
- 개인 계정에서 PostgreSQL RDS를 생성하고 EC2 Security Group만 5432번 source로 허용한다.
- 비밀 저장 방식을 결정하고 배포 환경 변수의 실제 값을 준비한다.
- Docker Hub Repository와 push/pull Token을 준비한다.
- Parameter Store에 Docker Hub username과 push Token을 생성하고 CodeBuild Role의 조회 범위를 두 파라미터로
  제한한다.
- EC2를 bootstrap하고 CodeBuild, CodeDeploy와 CodePipeline을 순서대로 연결한다.
- 최초 `dev` 배포와 HTTPS health check를 확인한다.
- EC2의 사용자 지정 S3 읽기 정책이 실제 배포 버킷과 객체 경로로 제한되는지 정기적으로 확인한다.
- 개인 계정에서 발생하는 비용과 팀 계정 이전 조건을 운영 문서에 기록한다.
