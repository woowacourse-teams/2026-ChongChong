import { useEffect, useRef } from 'react';
import AttachmentField from '../../../shared/widgets/AttachmentField';

// 임시 기술 소개: 제거할 때 이 파일과 App.tsx의 import / <UploadUpdateModal />만 삭제합니다.
// 안내 문구, 표시 정책, 스타일은 이 파일 안에서만 관리합니다.
const SEEN_KEY = 'chongchong:upload-update:v1';

export default function UploadUpdateModal() {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    let seen = false;
    try {
      seen = sessionStorage.getItem(SEEN_KEY) === 'seen';
    } catch {
      // 저장소를 사용할 수 없어도 안내를 열고 닫을 수 있습니다.
    }
    if (!seen && !dialog.open) dialog.showModal();
    return () => {
      if (dialog.open) dialog.close();
    };
  }, []);

  // 모달이 열려 있는 동안에만 배경 스크롤을 잠급니다. Escape도 같은 정리를 거칩니다.
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    let previousOverflow: string | undefined;
    const syncScroll = () => {
      if (dialog.open && previousOverflow === undefined) {
        previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
      } else if (!dialog.open && previousOverflow !== undefined) {
        document.body.style.overflow = previousOverflow;
        previousOverflow = undefined;
      }
    };
    const observer = new MutationObserver(syncScroll);
    observer.observe(dialog, { attributes: true, attributeFilter: ['open'] });
    syncScroll();
    return () => {
      observer.disconnect();
      if (previousOverflow !== undefined) document.body.style.overflow = previousOverflow;
    };
  }, []);

  const close = () => {
    try {
      sessionStorage.setItem(SEEN_KEY, 'seen');
    } catch {
      // 저장소 차단 여부와 관계없이 닫습니다.
    }
    dialogRef.current?.close();
  };

  return (
    <>
      <style>{STYLES}</style>
      <button
        ref={triggerRef}
        className="cc-upload-update-trigger"
        type="button"
        aria-haspopup="dialog"
        onClick={() => dialogRef.current?.showModal()}
      >
        <span>NEW</span> 이미지·파일 첨부가 생겼어요 <span aria-hidden="true">↗</span>
      </button>
      <dialog
        ref={dialogRef}
        className="cc-upload-update"
        aria-labelledby="cc-upload-update-title"
        aria-describedby="cc-upload-update-description"
        onCancel={(event) => {
          event.preventDefault();
          close();
        }}
        onClose={() => triggerRef.current?.focus({ preventScroll: true })}
        onClick={(event) => {
          if (event.target === event.currentTarget) close();
        }}
      >
        <button
          className="cc-upload-update-close"
          type="button"
          onClick={close}
          aria-label="새 기능 안내 닫기"
        >
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="m6 6 12 12M18 6 6 18"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
            />
          </svg>
        </button>
        <div className="cc-upload-update-panel">
          <header className="cc-upload-update-heading">
            <p className="cc-upload-update-eyebrow">
              <span>NEW</span> 총총 업데이트
            </p>
            <h2 id="cc-upload-update-title">
              말로 다 못한 내용은,
              <br />
              파일로 함께 나눠요.
            </h2>
            <p id="cc-upload-update-description">
              이제 공지, 과제, 제출물에 이미지와 파일을 첨부할 수 있어요.
              <br className="cc-upload-update-desktop-break" /> 스터디 자료부터 인증 사진까지, 총총
              한곳에 모아 보세요.
            </p>
          </header>

          <div className="cc-upload-update-preview" aria-label="이미지와 파일 첨부 예시">
            <p className="cc-upload-update-preview-label">실제 파일 첨부 UI · 미리보기</p>
            <div inert>
              <AttachmentField
                id="cc-upload-update-example-file"
                label="파일 업로드"
                items={[
                  {
                    id: 'example-file',
                    name: '이번 주 스터디 자료.pdf',
                    status: 'completed',
                    fileId: 1,
                  },
                ]}
                onSelect={() => undefined}
                onRemove={() => undefined}
              />
            </div>
            <div className="cc-upload-update-limits">
              <span>
                파일당 최대 <strong>5MB</strong>
              </span>
              <span>
                이미지·파일 각각 최대 <strong>10개</strong>
              </span>
            </div>
          </div>

          <section className="cc-upload-update-tech" aria-labelledby="cc-upload-update-tech-title">
            <div className="cc-upload-update-section-label">
              이 기능 뒤의 기술 <span>Amazon S3 · Presigned URL</span>
            </div>
            <h3 id="cc-upload-update-tech-title">파일은 바로 보내고, 서버는 확인에 집중해요.</h3>
            <p>
              파일 전송까지 서버가 맡으면 큰 자료나 업로드가 몰릴 때 부담이 커질 수 있어요. 총총은
              임시 업로드 주소를 발급해, 브라우저가 S3 저장소로 파일을 직접 보내도록 만들었어요.
            </p>
            <ol className="cc-upload-update-flow">
              <li>
                <span className="cc-upload-update-step">01</span>
                <strong>업로드 주소 받기</strong>
                <p>
                  서버가 파일 정보를 저장하고
                  <br />
                  유효기간이 있는 URL을 발급해요.
                </p>
                <code>브라우저 ↔ 서버</code>
              </li>
              <li>
                <span className="cc-upload-update-step">02</span>
                <strong>S3로 직접 전송</strong>
                <p>
                  파일 본문은 서버를 거치지 않고
                  <br />
                  저장소로 바로 이동해요.
                </p>
                <code>브라우저 → S3</code>
              </li>
              <li>
                <span className="cc-upload-update-step">03</span>
                <strong>확인 후 완료</strong>
                <p>
                  서버가 실제 파일의 존재와 크기를
                  <br />
                  확인한 뒤 완료로 바꿔요.
                </p>
                <code>PENDING → COMPLETED</code>
              </li>
            </ol>
            <div className="cc-upload-update-result">
              <span aria-hidden="true">↳</span>
              <p>
                <strong>전송 부하는 분리하고, 완료 처리는 확실하게.</strong>
                <br />
                파일 전송은 S3가 맡고, 서버는 파일 정보와 완료 상태를 관리해요.
              </p>
            </div>
            <details className="cc-upload-update-details">
              <summary>안전한 업로드를 위해 더한 장치들</summary>
              <ul>
                <li>
                  <strong>덮어쓰기 방지</strong> — 무작위 저장 경로와 조건부 업로드(If-None-Match)로
                  같은 경로의 파일이 덮어써지지 않게 해요.
                </li>
                <li>
                  <strong>서버에서 다시 검증</strong> — 완료 요청자의 소유권과 실제 파일 크기를
                  확인해, 전송 응답만으로 완료 처리하지 않아요.
                </li>
                <li>
                  <strong>짧은 DB 트랜잭션</strong> — S3 응답을 기다릴 때는 DB 트랜잭션을 유지하지
                  않고, 완료 저장 시 잠금과 재검증으로 상태를 보호해요.
                </li>
              </ul>
            </details>
          </section>
          <footer className="cc-upload-update-footer">
            <p>자료도, 기록도. 스터디에 필요한 만큼 함께.</p>
            <button type="button" onClick={close}>
              좋아요, 총총 둘러보기 <span aria-hidden="true">→</span>
            </button>
          </footer>
        </div>
      </dialog>
    </>
  );
}

const STYLES = `
  .cc-upload-update-trigger { margin-left: auto; display: inline-flex; align-items: center; gap: 9px; border: 1px solid var(--line); border-radius: 999px; background: var(--surface-card); padding: 10px 15px; color: var(--ink); font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
  .cc-upload-update-trigger > span:first-child, .cc-upload-update-eyebrow > span { color: var(--brand-deep); font-size: 10px; font-weight: 800; letter-spacing: .06em; }
  .cc-upload-update-trigger:hover { background: var(--surface-soft); }
  .cc-upload-update { width: min(760px, calc(100% - 32px)); max-width: none; max-height: calc(100dvh - 48px); margin: auto; padding: 8px; border: 1px solid var(--line); border-radius: 28px; background: var(--surface-card); color: var(--ink); box-shadow: var(--shadow-dialog); font: inherit; text-align: left; overflow: hidden; }
  .cc-upload-update::backdrop { background: rgba(16, 32, 25, .48); backdrop-filter: blur(6px); }
  .cc-upload-update-panel { max-height: calc(100dvh - 66px); padding: 30px; overflow-y: auto; overscroll-behavior: contain; scrollbar-gutter: stable; scrollbar-width: thin; scrollbar-color: var(--line) transparent; }
  .cc-upload-update-close, .cc-upload-update-footer button { font: inherit; cursor: pointer; }
  .cc-upload-update-close { position: absolute; z-index: 1; top: 18px; right: 18px; display: grid; place-items: center; width: 34px; height: 34px; padding: 7px; border: 0; border-radius: 50%; background: var(--surface-control-hover); color: var(--muted); }
  .cc-upload-update-close svg { width: 20px; height: 20px; }
  .cc-upload-update-eyebrow { display: flex; align-items: center; gap: 9px; margin: 0 0 16px; color: var(--muted); font-size: 12px; font-weight: 600; }
  .cc-upload-update-eyebrow > span { border-radius: 6px; padding: 4px 7px; background: var(--surface-soft); }
  .cc-upload-update-heading h2 { margin: 0 0 14px; color: var(--ink); font-size: clamp(26px, 4vw, 34px); font-weight: 750; letter-spacing: -.05em; line-height: 1.3; }
  .cc-upload-update-heading > p:last-child, .cc-upload-update-tech > p { margin: 0; color: var(--muted); font-size: 14px; line-height: 1.8; word-break: keep-all; overflow-wrap: anywhere; }
  .cc-upload-update-preview { display: grid; gap: 14px; margin: 24px 0 30px; padding: 18px; border: 1px solid var(--line); border-radius: 16px; background: var(--surface-card); }
  .cc-upload-update-preview-label { margin: 0; color: var(--muted); font-size: 11px; }
  .cc-upload-update-limits { display: flex; flex-wrap: wrap; gap: 4px 12px; color: var(--muted); font-size: 11px; }
  .cc-upload-update-limits strong { color: var(--brand-deep); font-weight: 750; }
  .cc-upload-update-section-label { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; color: var(--brand-deep); font-size: 12px; font-weight: 700; }
  .cc-upload-update-section-label > span { color: var(--muted); font-size: 11px; font-weight: 500; }
  .cc-upload-update-tech h3 { margin: 10px 0; font-size: 20px; line-height: 1.5; letter-spacing: -.04em; word-break: keep-all; }
  .cc-upload-update-flow { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; list-style: none; padding: 0; margin: 20px 0 16px; }
  .cc-upload-update-flow li { padding: 16px 12px; border: 1px solid var(--line); border-radius: 14px; }
  .cc-upload-update-step { display: block; margin-bottom: 12px; color: var(--brand-deep); font-size: 11px; font-weight: 800; letter-spacing: .08em; }
  .cc-upload-update-flow strong { display: block; font-size: 13px; font-weight: 700; }
  .cc-upload-update-flow p { margin: 8px 0 14px; color: var(--muted); font-size: 11px; line-height: 1.8; }
  .cc-upload-update-flow code { color: var(--brand-deep); font-family: inherit; font-size: 10px; font-weight: 600; }
  .cc-upload-update-result { display: flex; align-items: flex-start; gap: 10px; padding: 15px; border-radius: 12px; background: var(--surface-soft); color: var(--brand-deep); }
  .cc-upload-update-result > span { font-size: 20px; line-height: 1.4; }
  .cc-upload-update-result p { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.8; word-break: keep-all; }
  .cc-upload-update-result strong { color: var(--brand-deep); font-weight: 700; }
  .cc-upload-update-details { margin-top: 16px; color: var(--muted); font-size: 12px; line-height: 1.8; }
  .cc-upload-update-details summary { width: fit-content; color: var(--ink); cursor: pointer; padding: 4px 0; }
  .cc-upload-update-details ul { margin: 12px 0 0; padding-left: 18px; }
  .cc-upload-update-details li + li { margin-top: 9px; }
  .cc-upload-update-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 26px; padding-top: 20px; border-top: 1px solid var(--line); }
  .cc-upload-update-footer p { margin: 0; color: var(--muted); font-size: 11px; }
  .cc-upload-update-footer button { display: flex; justify-content: center; align-items: center; gap: 20px; flex-shrink: 0; padding: 12px 17px; border: 0; border-radius: 12px; background: var(--brand-deep); color: #fff; font-size: 13px; font-weight: 650; }
  .cc-upload-update-footer button:hover { background: var(--brand-dark); }
  .cc-upload-update :is(button, summary):focus-visible, .cc-upload-update-trigger:focus-visible { outline: 3px solid var(--brand); outline-offset: 3px; }
  @media (max-width: 600px) {
    .cc-upload-update { width: calc(100% - 24px); max-height: calc(100dvh - 24px); border-radius: 20px; }
    .cc-upload-update-panel { max-height: calc(100dvh - 42px); padding: 22px 14px 14px; }
    .cc-upload-update-close { top: 12px; right: 12px; }
    .cc-upload-update-trigger { gap: 6px; padding: 8px 10px; font-size: 10px; }
    .cc-upload-update-preview { grid-template-columns: 1fr; gap: 14px; padding: 15px; }
    .cc-upload-update-limits { display: flex; flex-wrap: wrap; gap: 4px 12px; }
    .cc-upload-update-tech h3 { font-size: 18px; }
    .cc-upload-update-flow { grid-template-columns: 1fr; }
    .cc-upload-update-flow li { display: grid; grid-template-columns: 25px 1fr; gap: 4px 8px; padding: 13px; }
    .cc-upload-update-step { grid-row: span 3; margin: 2px 0 0; }
    .cc-upload-update-flow p { margin: 0; font-size: 12px; }
    .cc-upload-update-flow br, .cc-upload-update-desktop-break { display: none; }
    .cc-upload-update-footer { flex-direction: column-reverse; gap: 12px; }
    .cc-upload-update-footer button { width: 100%; }
  }
`;
