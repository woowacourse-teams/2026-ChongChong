"""Docker Hub 조회 전용. DELETE 요청과 자동 삭제 기능은 제공하지 않는다."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import urlparse
from urllib.request import Request, build_opener, HTTPRedirectHandler


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("API redirect refused")


def request(url, token="", credentials=None):
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None
    if credentials is not None:
        data = json.dumps(credentials).encode()
        headers["Content-Type"] = "application/json"
    with build_opener(NoRedirect).open(Request(url, data=data, headers=headers), timeout=30) as response:
        return json.load(response)


def fetch_tags(repository, token, get=request):
    if not re.fullmatch(r"[a-z0-9_-]+/[a-z0-9._-]+", repository):
        raise ValueError("namespace/repository 형식 필요")
    namespace, name = repository.split("/")
    path = "/v2/namespaces/" + namespace + "/repositories/" + name + "/tags"
    url = "https://hub.docker.com" + path + "?page_size=100"
    seen, tags = set(), []
    while url:
        parsed = urlparse(url)
        if parsed.scheme != "https" or parsed.netloc != "hub.docker.com" or parsed.path.rstrip("/") != path:
            raise ValueError("API pagination 경로 오류")
        if url in seen or len(seen) >= 10000:
            raise ValueError("API pagination 반복/한도 초과")
        seen.add(url)
        page = get(url, token)
        if not isinstance(page.get("results"), list):
            raise ValueError("태그 응답 오류")
        tags.extend(page["results"])
        url = page.get("next")
    return tags


def candidates(tags, required, now=None):
    now = now or datetime.now(timezone.utc)
    if not required:
        raise ValueError("보호 태그를 지정해야 합니다")
    parsed = {}
    for tag in tags:
        name = tag["name"]
        if not re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}", name) or name in parsed:
            raise ValueError("태그 중복/형식 오류")
        digests = {i["digest"] for i in tag.get("images", []) if i.get("digest")}
        if tag.get("digest"):
            digests.add(tag["digest"])
        if not digests or any(not re.fullmatch(r"sha256:[a-f0-9]{64}", d) for d in digests):
            raise ValueError("digest 누락/오류; 후보 조회 중단")
        stamp = datetime.fromisoformat(tag["tag_last_pushed"].replace("Z", "+00:00"))
        if stamp.tzinfo is None or stamp > now:
            raise ValueError("업로드 시각 오류")
        key = tag.get("digest") or tuple(sorted(digests))
        parsed[name] = (digests, stamp, key)
    if not set(required).issubset(parsed):
        raise ValueError("필수 보호 태그 누락; 후보 조회 중단")
    protected = set()
    groups = {}
    for name, (digests, stamp, key) in parsed.items():
        # 이 저장소가 빌드한 커밋 SHA 태그만 삭제 후보로 다룬다.
        if name in required or not re.fullmatch(r"[a-f0-9]{40}", name):
            protected.update(digests)
        else:
            groups[key] = max(stamp, groups.get(key, stamp))
    recent = set(sorted(groups, key=lambda k: (groups[k], str(k)), reverse=True)[:10])
    for digests, stamp, key in parsed.values():
        if key in recent or stamp >= now - timedelta(days=90):
            protected.update(digests)
    # 같은 manifest/하위 이미지가 다른 태그로 연결된 경우에도 모두 보호한다.
    changed = True
    while changed:
        before = len(protected)
        for digests, _, _ in parsed.values():
            if digests & protected:
                protected.update(digests)
        changed = len(protected) != before
    return [{"tag": name, "digests": sorted(digests), "pushed_at": stamp.isoformat()}
            for name, (digests, stamp, _) in sorted(parsed.items()) if not digests & protected]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--protected-tags", required=True, help="쉼표로 구분한 환경별 current,rollback 태그")
    parser.add_argument("--input", type=Path, help="오프라인 검증용 태그 배열 JSON")
    args = parser.parse_args()
    if args.input:
        tags = json.loads(args.input.read_text())
    else:
        credentials = {"identifier": os.environ["DOCKERHUB_READ_USER"], "secret": os.environ["DOCKERHUB_READ_TOKEN"]}
        token = request("https://hub.docker.com/v2/auth/token", credentials=credentials)["access_token"]
        tags = fetch_tags(args.repository, token)
    rows = candidates(tags, [v.strip() for v in args.protected_tags.split(",") if v.strip()])
    print(json.dumps({"repository": args.repository, "read_only": True, "candidates": rows}, indent=2))
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a") as output:
            output.write("## 원격 이미지 정리 후보 (조회 전용)\n\n")
            output.write("삭제 전 배포·동기화·롤백 중지 여부와 최신 보호 태그를 다시 확인하세요.\n\n")
            output.write("후보 " + str(len(rows)) + "개. 실행 로그의 digest와 태그를 확인하세요. 자동 삭제는 없습니다.\n")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # 인증 오류 응답이나 환경변수 원문을 로그에 노출하지 않는다.
        print("원격 후보 조회 실패: 인증·보호 태그·응답 형식을 확인하세요. 삭제하지 않았습니다.", file=sys.stderr)
        sys.exit(1)
