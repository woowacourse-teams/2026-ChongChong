import { css } from '@emotion/react';
import { useState } from 'react';
import { QueryClient, QueryClientProvider, useQuery, queryOptions } from '@tanstack/react-query';

const queryClient = new QueryClient();

const exampleOptions = () =>
  queryOptions({
    queryKey: ['example'],
    queryFn: () => fetch('https://api.github.com/repos/TanStack/query').then((res) => res.json()),
  });

export default function App() {
  const [count, setCount] = useState(0);
  return (
    <QueryClientProvider client={queryClient}>
      <div
        css={css`
          color: green;
        `}
      >
        chong chong
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
