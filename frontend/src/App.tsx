import { css } from '@emotion/react';
import { useState } from 'react';
import { QueryClient, QueryClientProvider, useQuery, queryOptions } from '@tanstack/react-query';
import { typography } from './styles/global';
import api from './client';

const queryClient = new QueryClient();

interface Repo {
  name: string;
  description: string;
  subscribers_count: number;
  stargazers_count: number;
  forks_count: number;
}

const exampleOptions = () =>
  queryOptions({
    queryKey: ['example'],
    queryFn: () => api.get('repos/TanStack/query').json<Repo>(),
  });

export default function App() {
  const [count, setCount] = useState(0);
  return (
    <QueryClientProvider client={queryClient}>
      <div
        css={[
          typography.headline,
          css`
            color: var(--text-brand);
            font-size: 64px;
          `,
        ]}
      >
        총총 총총 총총 씨없는팀
        <button type="button" onClick={() => setCount((c) => c + 1)}>
          {count}
        </button>
      </div>
      <TanstackExample />
    </QueryClientProvider>
  );
}

function TanstackExample() {
  const { isPending, error, data } = useQuery(exampleOptions());

  if (isPending) return 'Loading...';

  if (error) return 'An error has occurred: ' + error.message;

  return (
    <div>
      <h1>{data.name}</h1>
      <p>{data.description}</p>
      <strong>👀 {data.subscribers_count}</strong> <strong>✨ {data.stargazers_count}</strong>{' '}
      <strong>🍴 {data.forks_count}</strong>
    </div>
  );
}
