import { Fragment, useRef, useState } from 'react';
import { clamp01, trackProgress, useScrollFrame } from '../useLandingMotion';

/** 한 줄씩 나눈 문장. highlight 줄은 불이 켜지면 초록색이에요. */
const lines: { text: string; highlight?: boolean }[][] = [
  [{ text: '과제 냈어요? 공지 읽으셨어요?' }],
  [{ text: '매주 단톡방을 거슬러 올라가며' }],
  [{ text: '한 명씩 세던 일,' }, { text: '이제 그만해도 돼요.', highlight: true }],
];

const words = lines.map((line) =>
  line.flatMap((part) =>
    part.text.split(' ').map((word) => ({ word, highlight: !!part.highlight })),
  ),
);
const totalWords = words.reduce((sum, line) => sum + line.length, 0);

export default function Statement() {
  const trackRef = useRef<HTMLElement>(null);
  const [lit, setLit] = useState(0);

  useScrollFrame(
    () => (trackRef.current ? trackProgress(trackRef.current) : 0),
    (progress) => setLit(Math.round(clamp01((progress - 0.1) / 0.7) * totalWords)),
  );

  let index = 0;
  return (
    <section ref={trackRef} className="statement" aria-label="스터디 리드의 고민">
      <div className="statement-sticky">
        <div className="container">
          <p>
            {words.map((line, lineIndex) => (
              <Fragment key={lineIndex}>
                {lineIndex > 0 && <br />}
                {line.map(({ word, highlight }, wordIndex) => {
                  const on = index++ < lit;
                  return (
                    <Fragment key={wordIndex}>
                      {wordIndex > 0 && ' '}
                      <span className={'w' + (on ? ' on' : '') + (highlight ? ' hl' : '')}>
                        {word}
                      </span>
                    </Fragment>
                  );
                })}
              </Fragment>
            ))}
          </p>
        </div>
      </div>
    </section>
  );
}
