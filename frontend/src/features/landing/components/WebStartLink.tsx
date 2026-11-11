import { usePostHog } from '@posthog/react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';

/** 웹 서비스로 들어가는 링크. 누를 때마다 위치를 함께 PostHog에 기록해요. */
export default function WebStartLink({
  className,
  position,
  children = '웹으로 시작하기',
}: {
  className: string;
  position: 'header' | 'hero' | 'final' | 'footer';
  children?: ReactNode;
}) {
  const posthog = usePostHog();

  return (
    <Link
      className={className}
      {...URL_POLICY.toStudies()}
      onClick={() => posthog?.capture('web_button_clicked', { location: 'landing_page', position })}
    >
      {children}
    </Link>
  );
}
