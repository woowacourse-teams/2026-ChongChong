import type { QueryClient, QueryKey } from '@tanstack/react-query';
import { waitFor } from '@testing-library/react';

export async function waitForQuerySuccess(queryClient: QueryClient, queryKey: QueryKey) {
  await waitFor(() => {
    expect(queryClient.getQueryState(queryKey)?.status).toBe('success');
  });
}
