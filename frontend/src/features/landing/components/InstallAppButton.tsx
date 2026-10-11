import { useEffect, useRef, useState } from 'react';
import { usePostHog } from '@posthog/react';
import { useNavigate } from 'react-router';
import chongchongLogo from '../assets/chongchong-logo.png';
import { usePwaInstall } from '../usePwaInstall';

function getInstallSteps() {
  const userAgent = navigator.userAgent;
  const isIos =
    /iPad|iPhone|iPod/i.test(userAgent) ||
    (/Macintosh/i.test(userAgent) && navigator.maxTouchPoints > 1);

  if (isIos) {
    return [
      'Safari에서 현재 페이지를 열어 주세요.',
      '공유 버튼을 누르고 ‘홈 화면에 추가’를 선택해 주세요.',
      '‘웹 앱으로 열기’가 보이면 켠 뒤 ‘추가’를 눌러 주세요.',
    ];
  }
  if (/Android/i.test(userAgent)) {
    return [
      'Chrome에서 현재 페이지를 열어 주세요.',
      '오른쪽 위 메뉴(⋮)에서 ‘홈 화면에 추가’ 또는 ‘앱 설치’를 선택해 주세요.',
      '화면에 표시되는 안내에 따라 추가해 주세요.',
    ];
  }
  return [
    'Chrome 또는 Edge에서 현재 페이지를 열어 주세요.',
    '주소창의 설치 아이콘 또는 브라우저 메뉴의 앱 설치 항목을 선택해 주세요.',
    '설치 항목이 없다면 웹사이트로 바로 이용할 수 있어요.',
  ];
}

export default function InstallAppButton({
  onActiveChange,
}: {
  onActiveChange: (active: boolean) => void;
}) {
  const { install, isPrompting } = usePwaInstall();
  const [guideOpen, setGuideOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const navigate = useNavigate();
  const posthog = usePostHog();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (guideOpen && dialog && !dialog.open) dialog.showModal();
  }, [guideOpen]);

  async function startApp() {
    posthog?.capture('app_button_clicked', { location: 'landing_page' });
    onActiveChange(true);
    const result = await install();
    if (result === 'busy') return;
    if (result === 'unavailable') {
      setGuideOpen(true);
    } else {
      onActiveChange(false);
      if (result === 'installed') navigate('/studies');
    }
  }

  const closeGuide = () => dialogRef.current?.close();

  return (
    <>
      <button
        ref={buttonRef}
        className="cc-destination cc-app-button"
        type="button"
        aria-haspopup="dialog"
        aria-busy={isPrompting}
        disabled={isPrompting}
        onClick={startApp}
      >
        <svg className="cc-destination-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <rect x="6" y="2" width="12" height="20" rx="3" stroke="currentColor" strokeWidth="1.8" />
          <path d="M10 5h4M11 19h2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
        <span className="cc-destination-copy">
          <span className="cc-destination-name">앱으로 시작하기</span>
        </span>
      </button>

      <dialog
        ref={dialogRef}
        className="cc-install-dialog"
        aria-labelledby="cc-install-dialog-title"
        aria-describedby="cc-install-dialog-description"
        onClose={() => {
          setGuideOpen(false);
          onActiveChange(false);
          buttonRef.current?.focus();
        }}
        onClick={(event) => {
          if (event.target === event.currentTarget) closeGuide();
        }}
      >
        <div className="cc-install-dialog-panel">
          <button
            className="cc-install-dialog-close"
            type="button"
            aria-label="앱 설치 안내 닫기"
            onClick={closeGuide}
          >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="m7 7 10 10M17 7 7 17"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
          </button>
          <span className="cc-install-dialog-bunny" aria-hidden="true">
            <img src={chongchongLogo} alt="" width={232} height={256} />
          </span>
          <p className="cc-install-dialog-label">총총 앱</p>
          <h2 id="cc-install-dialog-title">총총을 앱으로 만나요</h2>
          <p className="cc-install-dialog-description" id="cc-install-dialog-description">
            브라우저에서 직접 추가할 수 있어요.
            <br />
            이미 추가했다면 총총 앱 아이콘으로 시작해 주세요.
          </p>
          <ol className="cc-install-steps">
            {getInstallSteps().map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
          <button className="cc-install-dialog-confirm" type="button" onClick={closeGuide}>
            알겠어요
          </button>
        </div>
      </dialog>
    </>
  );
}
