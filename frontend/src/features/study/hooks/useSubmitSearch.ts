import { useState } from 'react';
import { useSearchParams } from 'react-router';

const SEARCH_QUERY_KEY = 'keyword';

export function useSubmitSearch() {
  const [searchQuery, setSearchQuery] = useSearchParams();
  const [searchValue, updateSearchValue] = useState(() => searchQuery.get(SEARCH_QUERY_KEY) ?? '');

  const handleSearchValue = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    updateSearchValue(value);
    if (!value) {
      setSearchQuery((prev) => {
        const next = new URLSearchParams(prev);
        next.delete(SEARCH_QUERY_KEY);
        return next;
      });
    }
  };

  function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearchQuery((prev) => {
      const next = new URLSearchParams(prev);
      next.set(SEARCH_QUERY_KEY, searchValue.trim());
      return next;
    });
  }

  return {
    searchQuery: searchQuery.get(SEARCH_QUERY_KEY) || undefined,
    searchValue,
    handleSearchValue,
    handleSubmit,
  };
}
