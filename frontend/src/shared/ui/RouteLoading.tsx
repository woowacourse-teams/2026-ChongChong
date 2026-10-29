import Loading from './Loading';
import Main from './Main';
import Page from './Page';
import TopHeader from './TopHeader';

export default function RouteLoading() {
  return (
    <Page>
      <TopHeader />
      <Main>
        <Loading />
      </Main>
    </Page>
  );
}
