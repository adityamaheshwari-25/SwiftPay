// src/hooks/useApi.js
import { useState } from "react";

export function useApi(apiFunc, options = {}) {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const callApi = async (...args) => {
    setIsLoading(true);
    setError(null);

    try {
      const res = await apiFunc(...args);
      setData(res);
      if (options.onSuccess) options.onSuccess(res);
      return res;
    } catch (err) {
      setError(err);
      if (options.onError) options.onError(err);
      // console.error(err);
      return null;
    } finally {
      setIsLoading(false);
    }
  };

  return { data, isLoading, error, callApi };
}
