import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { Suspense } from 'react';
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
import Spinner from '../../../shared/ui/Spinner';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';

export default function NotificationListPage() {
  return (
    <Page>
      <TopHeader
        left={
          <>
            <PrevButton />
            <TopHeader.Title>알림</TopHeader.Title>
          </>
        }
      />
      <Main css={{ paddingTop: tokens.spacing[5] }}>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}

function Content() {
  const { data, fetchNextPage, hasNextPage, isFetching, isFetchingNextPage, isFetchNextPageError } =
    useSuspenseInfiniteQuery(notificationQueries.list());
  const notifications = data.pages.flatMap((page) => page.notifications);
  const loadMoreRef = useInfiniteScroll({
    hasNextPage: hasNextPage && !isFetchNextPageError,
    isFetchingNextPage: isFetching,
    fetchNextPage,
  });
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
    <>
      <List aria-label="알림 목록" css={{ gap: tokens.spacing[2] }}>
        {notifications.map((notification) => (
          <List.Item key={notification.id}>
            <Link
              {...URL_POLICY.toDeepLink(notification.deepLink)}
              onClick={() => handleNotificationClick(notification)}
            >
              <NotificationItem key={notification.id} notification={notification} />
            </Link>
          </List.Item>
        ))}
      </List>
      <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
      {isFetchingNextPage && <Spinner ariaLabel="알림을 더 불러오는 중" />}
    </>
  );
}
