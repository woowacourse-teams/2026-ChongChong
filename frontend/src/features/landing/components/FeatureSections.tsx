import type { CSSProperties } from 'react';
import chongchongLogo from '../assets/chongchong-logo.png';
import studyMascot from '../assets/mascots/header-icon.webp';
import laptopMascot from '../assets/mascots/laptop-icon.webp';
import assignDone from '../assets/parts/assign-done.webp';
import assignStatus from '../assets/parts/assign-status.webp';
import calendar from '../assets/parts/calendar.webp';
import calendarDue from '../assets/parts/calendar-due.webp';
import homeRows from '../assets/parts/home-rows.webp';
import invite from '../assets/parts/invite.webp';
import noticeRead from '../assets/parts/notice-read.webp';
import noticeStatus from '../assets/parts/notice-status.webp';
import studyThree from '../assets/parts/study-three.webp';
import homeMemberScreen from '../assets/screens/home-member.webp';
import wordmark from '../assets/wordmark.webp';
import InstallAppButton from './InstallAppButton';
import Phone from './Phone';
import RemindFlow from './RemindFlow';
import WebStartLink from './WebStartLink';

/** 앱 설정 화면(AccountMenuSection)과 같은 개인정보처리방침·지원 메일이에요. */
const PRIVACY_POLICY_URL =
  'https://glittery-cobweb-ac6.notion.site/3b9842e631298049903cec2633a4fab7?pvs=74';
const SUPPORT_EMAIL = 'chongwithoutc@gmail.com';

const delay = (seconds: number) => ({ '--d': `${seconds}s` }) as CSSProperties;
const noop = () => {};

/** 화면에서 잘라낸 UI 조각. 원본은 780px(2배) 폭이에요. */
function UiPiece({
  src,
  alt,
  height,
  className = '',
}: {
  src: string;
  alt: string;
  height: number;
  className?: string;
}) {
  return (
    <div className={('ui ' + className).trim()}>
      <img src={src} alt={alt} width={780} height={height} />
    </div>
  );
}

export function OverviewSection() {
  return (
    <section className="bento-sec" id="dashboard" data-section>
      <div className="container">
        <div className="sec-head" data-reveal>
          <p className="eyebrow">한눈에 보는 현황</p>
          <h2 className="title">세지 말고, 보세요</h2>
          <p className="desc">공지 확인도, 과제 제출도 숫자와 이름으로 바로 보여줘요.</p>
        </div>
        <div className="bento">
          <article className="tile full" data-reveal>
            <div className="tile-copy">
              <h3>
                누가 읽고, 누가 냈는지
                <br />
                바로 보여요
              </h3>
              <p>
                공지는 끝까지 읽은 사람과 아직 안 본 사람을, 과제는 제출·지각 제출·미제출을 이름으로
                나눠서 보여줘요.
              </p>
            </div>
            <div className="pair">
              <div className="ui-stack">
                <UiPiece
                  className="main"
                  src={noticeRead}
                  alt="확인 2명 목록: 이름과 확인 시각"
                  height={428}
                />
                <UiPiece
                  className="badge"
                  src={noticeStatus}
                  alt="공지 확인 현황: 4명 중 2명 확인"
                  height={212}
                />
              </div>
              <div className="ui-stack">
                <UiPiece
                  className="main"
                  src={assignDone}
                  alt="제출 2명, 지각 1명 목록"
                  height={436}
                />
                <UiPiece
                  className="badge"
                  src={assignStatus}
                  alt="과제 제출 현황: 4명 중 2명 제출"
                  height={212}
                />
              </div>
            </div>
          </article>
          <article className="tile half" data-reveal>
            <div className="tile-copy">
              <h3>
                일정은
                <br />
                캘린더로 한눈에
              </h3>
              <p>
                공지와 과제가 올라온 날, 과제 마감일을 캘린더에 표시해요. 날짜를 누르면 그날 일정이
                보여요.
              </p>
            </div>
            <div className="cal-stack">
              <UiPiece
                src={calendar}
                alt="스터디 캘린더: 공지·과제 생성일은 초록 점, 과제 마감일은 빨간 점"
                height={856}
              />
              <UiPiece
                className="due"
                src={calendarDue}
                alt="10월 28일 일정: 알고리즘 과제 제출 마감 23:59"
                height={148}
              />
            </div>
          </article>
          <article className="tile half" data-reveal style={delay(0.08)}>
            <div className="tile-copy">
              <h3>
                여러 스터디도
                <br />한 번에
              </h3>
              <p>리드하는 스터디도, 스터디원으로 참여 중인 스터디도 한 곳에 모아 보여줘요.</p>
            </div>
            <UiPiece
              src={studyThree}
              alt="내 스터디 목록: 스터디 리드인 프론트엔드 CS 스터디·알고리즘 스터디, 스터디원으로 참여 중인 영어 회화 스터디"
              height={960}
            />
          </article>
        </div>
      </div>
    </section>
  );
}

const notifications = [
  { time: '지금', title: '[알고리즘 스터디] 새 제출물', text: '바니 스터디원이 과제를 제출했어요' },
  { time: '3시간 전', title: '[CS 스터디] 새 과제', text: '이번 주 CS 과제' },
  { time: '3시간 전', title: '[알고리즘 스터디] 새 공지', text: '8월 스터디 운영 방식이 바뀝니다' },
];

export function NotifySection() {
  return (
    <section className="dark-sec" id="notify" data-section>
      <div className="container split">
        <div className="copy" data-reveal>
          <p className="eyebrow">제출 알림</p>
          <h2 className="title">
            과제를 제출하면
            <br />
            바로 알려드려요
          </h2>
          <p className="desc">
            스터디원이 과제를 내는 순간 리드에게 알림이 가요.
            <br />새 공지와 새 과제도 스터디원에게 바로 알려요.
          </p>
        </div>
        <div
          className="push-stage"
          data-reveal
          style={delay(0.1)}
          role="img"
          aria-label="총총 알림 3개: 새 제출물, 새 과제, 새 공지"
        >
          <div className="glow" />
          {notifications.map((item, index) => (
            <div key={item.title} className="push2" style={{ '--i': index } as CSSProperties}>
              <img src={chongchongLogo} alt="" />
              <div>
                <div className="push2-head">
                  <b>총총</b>
                  <span>{item.time}</span>
                </div>
                <p className="push2-title">{item.title}</p>
                <p className="push2-text">{item.text}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export function RemindSection() {
  return (
    <section className="split-sec" id="remind" data-section>
      <div className="container split">
        <div className="copy" data-reveal>
          <p className="eyebrow">예약 리마인드</p>
          <h2 className="title">
            안 낸 사람에게만,
            <br />
            정해둔 시간에 다시
          </h2>
          <p className="desc">
            과제를 올릴 때 리마인드 시각만 정해두세요.
            <br />
            아직 내지 않은 스터디원에게 총총이 대신 알려요.
          </p>
          <ul className="facts">
            <li className="fact">리마인드 시각은 여러 번 등록할 수 있어요</li>
            <li className="fact">아직 제출하지 않은 스터디원에게만 알림이 가요</li>
          </ul>
        </div>
        <RemindFlow />
      </div>
    </section>
  );
}

export function MemberSection() {
  return (
    <section className="split-sec grey" id="member" data-section>
      <div className="container split reverse">
        <div className="copy" data-reveal>
          <p className="eyebrow">스터디원 화면</p>
          <h2 className="title">
            스터디원에게는
            <br />할 일만 보여요
          </h2>
          <p className="desc">
            읽지 않은 공지와 제출하지 않은 과제만 홈에 모아줘요.
            <br />
            챙길 게 줄면, 리드가 챙길 일도 줄어요.
          </p>
        </div>
        <div className="visual" data-reveal style={delay(0.1)}>
          <Phone
            src={homeMemberScreen}
            label="스터디원 홈 화면: 할 일 2건, 읽지 않은 공지 1개, 제출하지 않은 과제 1개"
          />
        </div>
      </div>
    </section>
  );
}

export function StartSection() {
  return (
    <section className="steps-sec" id="start" data-section>
      <div className="container">
        <div className="steps-head" data-reveal>
          <p className="eyebrow">시작하기</p>
          <h2 className="title">시작은 3단계면 충분해요</h2>
        </div>
        <ol className="steps">
          <li className="step" data-reveal>
            <div className="step-copy">
              <p className="step-num">1</p>
              <h3>스터디 만들기</h3>
              <p>이름과 소개만 적으면 스터디가 만들어져요.</p>
            </div>
            <div className="step-art">
              <img className="mascot-art" src={studyMascot} alt="" width={480} height={420} />
            </div>
          </li>
          <li className="step" data-reveal style={delay(0.1)}>
            <div className="step-copy">
              <p className="step-num">2</p>
              <h3>링크로 초대하기</h3>
              <p>초대 링크 하나만 공유하면 스터디원이 모여요.</p>
            </div>
            <div className="step-art">
              <UiPiece
                src={invite}
                alt="초대 링크: https://chongchong.kr/studies/join"
                height={200}
              />
            </div>
          </li>
          <li className="step" data-reveal style={delay(0.2)}>
            <div className="step-copy">
              <p className="step-num">3</p>
              <h3>공지·과제 올리기</h3>
              <p>올리고 나면 확인과 독촉은 총총이 챙겨요.</p>
            </div>
            <div className="step-art">
              <UiPiece src={homeRows} alt="홈의 공지·과제 목록: 2/4 읽음, 1/4 제출" height={488} />
            </div>
          </li>
        </ol>
      </div>
    </section>
  );
}

export function FinalSection() {
  return (
    <section className="final" aria-labelledby="cc-final-title">
      <div className="container" data-reveal>
        <img className="mascot" src={laptopMascot} alt="" width={480} height={300} />
        <h2 id="cc-final-title" className="title">
          스터디 리드의 수고,
          <br />
          오늘부터 덜어드릴게요
        </h2>
        <div className="hero-actions">
          <WebStartLink className="btn btn-primary" position="final" />
          <InstallAppButton onActiveChange={noop} />
        </div>
        <p className="final-note">
          카카오 계정으로 바로 시작할 수 있어요 · 홈 화면에 추가하면 앱처럼 쓸 수 있어요
        </p>
      </div>
    </section>
  );
}

export function LandingFooter() {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-top">
          <span className="brand">
            <img src={wordmark} alt="총총" width={200} height={100} />
          </span>
          <div className="footer-links">
            <WebStartLink className="" position="footer">
              웹 서비스
            </WebStartLink>
            <a href={PRIVACY_POLICY_URL} target="_blank" rel="noreferrer">
              개인정보처리방침
            </a>
            <a href={`mailto:${SUPPORT_EMAIL}`}>문의하기</a>
          </div>
        </div>
        <p className="footer-info">
          안톨리니 · 디움 · 바니 · 이든 · 피즈
          <br />© 2026 총총
        </p>
      </div>
    </footer>
  );
}
