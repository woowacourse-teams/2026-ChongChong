import { useState } from 'react';

export function useSubmitSearch() {
  const [submittedValue, setSubmittedValue] = useState('');
  const [searchValue, updateSearchValue] = useState('');

  const handleSearchValue = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    updateSearchValue(value);
    if (!value) setSubmittedValue('');
  };

  function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmittedValue(searchValue.trim());
  }

  return { submittedValue, searchValue, handleSearchValue, handleSubmit };
}
