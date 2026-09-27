import { useSuspenseQuery } from '@tanstack/react-query';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { Suspense } from 'react';
import { Link } from 'react-router';
import Page from '../../../shared/ui/Page';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import ErrorContent from '../../../shared/ui/ErrorContent';
import Loading from '../../../shared/ui/Loading';
import EmptyContent from '../../../shared/ui/EmptyContent';
import List from '../../../shared/ui/List';
import { tokens } from '../../../styles/global';
import notificationQueries from '../queries';
import NotificationItem from '../components/NotificationItem';
import useMarkNotificationAsRead from '../hooks/useMarkNotificationAsRead';
import { usePostHog } from '@posthog/react';
import { Notification } from '../types';

export default function NotificationListPage() {
  return (
    <Page>
      <TopHeader
        left={
          <>
            <PrevButton to="/studies" />
            <TopHeader.Title>알림</TopHeader.Title>
          </>
        }
      />
      <Main css={{ paddingTop: tokens.spacing[5] }}>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <NotificationListPage.Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}

NotificationListPage.Content = function Content() {
  const { data: notifications } = useSuspenseQuery(notificationQueries.list());
  const { markAsRead } = useMarkNotificationAsRead();
  const posthog = usePostHog();

  function handleNotificationClick(notification: Notification) {
    if (!notification.isRead) {
      posthog.capture('unread_notification_clicked', {
        location: 'notification_page',
      });
    }
    markAsRead(notification);
  }

  return notifications.length === 0 ? (
    <EmptyContent message="아직 알림이 없어요" />
  ) : (
    <List aria-label="알림 목록" css={{ gap: tokens.spacing[2] }}>
      {notifications.map((notification) => (
        <List.Item key={notification.id}>
          <Link to={notification.deepLink} onClick={() => handleNotificationClick(notification)}>
            <NotificationItem key={notification.id} notification={notification} />
          </Link>
        </List.Item>
      ))}
    </List>
  );
};
