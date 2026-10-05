import type { ReactNode } from 'react';
import Button from '../../../../shared/ui/Button';
import { tokens, typography } from '../../../../styles/global';
import type { GuidelinePageProps } from '../../types';
import GuidelineArtwork from '../GuidelineArtwork';
import GuidelinePageLayout from '../GuidelinePageLayout';

const emphasisStyle = {
  color: '#007b48',
  fontWeight: tokens.fontWeight.semibold,
};

const steps: { id: string; content: ReactNode }[] = [
  { id: 'copy-link', content: '전달받은 초대 링크의 주소 전체를 복사해 주세요.' },
  {
    id: 'open-join',
    content: (
      <>
        <strong css={emphasisStyle}>스터디 참여하기</strong>를 눌러 주세요.
      </>
    ),
  },
  {
    id: 'paste-link',
    content: (
      <>
        <strong css={emphasisStyle}>초대 링크</strong> 입력란에 붙여넣고 참여해 주세요.
      </>
    ),
  },
];

export default function InvitationPage({ onJoinStudy }: GuidelinePageProps) {
  return (
    <GuidelinePageLayout
      artwork={<GuidelineArtwork theme="invitation" />}
      title="초대 링크로 함께 시작해요"
      description="스터디장에게 받은 초대 링크만 있으면 참여할 수 있어요."
      action={
        <Button variant="brandSolid" size="large" onClick={onJoinStudy}>
          초대 링크 입력하기
        </Button>
      }
    >
      <ol
        css={{
          ...typography.body,
          display: 'grid',
          gap: tokens.spacing[3],
          margin: 0,
          padding: `${tokens.spacing[4]} ${tokens.spacing[3]} ${tokens.spacing[4]} ${tokens.spacing[8]}`,
          background: tokens.bg.subtle,
          borderRadius: tokens.radius.md,
          '& li::marker': { color: tokens.text.brand, fontWeight: tokens.fontWeight.semibold },
        }}
      >
        {steps.map((step) => (
          <li key={step.id}>{step.content}</li>
        ))}
      </ol>
      <p css={{ ...typography.caption, margin: 0, color: tokens.text.muted, textAlign: 'center' }}>
        아직 초대 링크가 없어도 괜찮아요. 나중에 스터디 목록에서 참여할 수 있어요.
      </p>
    </GuidelinePageLayout>
  );
}
