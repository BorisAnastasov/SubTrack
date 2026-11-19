const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/+$/, "") || "";
export const AUTH_STORAGE_KEY = "subtrack-auth";

let refreshPromise;

export async function fetchApi(path, options) {
  try {
    return await fetch(`${API_BASE_URL}${path}`, options);
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error("Unable to reach the API. Check that the backend is running and that VITE_API_BASE_URL is configured correctly.");
    }
    throw error;
  }
}

function withAccessToken(options, accessToken) {
  return {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${accessToken}`,
    },
  };
}

async function refreshAuth() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const currentAuth = readStoredAuth();
      if (!currentAuth?.refreshToken) {
        return null;
      }

      const response = await fetchApi("/api/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: currentAuth.refreshToken }),
      });

      if (!response.ok) {
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
        window.dispatchEvent(new Event("subtrack-session-expired"));
        throw new Error("Your session expired. Please sign in again.");
      }

      const nextAuth = { ...currentAuth, ...(await response.json()) };
      sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextAuth));
      window.dispatchEvent(new CustomEvent("subtrack-auth-refreshed", { detail: nextAuth }));
      return nextAuth;
    })().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

async function fetchAuthenticated(path, accessToken, options) {
  const storedAuth = readStoredAuth();
  const currentToken = storedAuth?.accessToken || accessToken;
  const response = await fetchApi(path, withAccessToken(options, currentToken));
  if (response.status !== 401 && response.status !== 403) {
    return response;
  }

  const latestAuth = readStoredAuth();
  if (latestAuth?.accessToken && latestAuth.accessToken !== currentToken) {
    return fetchApi(path, withAccessToken(options, latestAuth.accessToken));
  }

  const refreshedAuth = await refreshAuth();
  return refreshedAuth
    ? fetchApi(path, withAccessToken(options, refreshedAuth.accessToken))
    : response;
}

async function readResponse(response) {
  const responseText = await response.text();
  try {
    return responseText ? JSON.parse(responseText) : null;
  } catch {
    return responseText;
  }
}

function throwResponseError(response, responseBody) {
  const message =
    responseBody?.message ||
    responseBody?.detail ||
    (typeof responseBody === "string" && responseBody) ||
    `Request failed (${response.status})`;
  throw new Error(message);
}

export function readStoredAuth() {
  try {
    const auth = JSON.parse(sessionStorage.getItem(AUTH_STORAGE_KEY));
    return auth?.accessToken && auth?.refreshToken ? auth : null;
  } catch {
    return null;
  }
}

export async function postAuth(path, payload, accessToken) {
  const options = {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    ...(payload ? { body: JSON.stringify(payload) } : {}),
  };
  const response = accessToken
    ? await fetchAuthenticated(`/api/auth/${path}`, accessToken, options)
    : await fetchApi(`/api/auth/${path}`, options);

  const responseBody = await readResponse(response);
  if (!response.ok) {
    throwResponseError(response, responseBody);
  }
  return responseBody;
}

export async function subscriptionRequest(path, accessToken, options = {}) {
  const requestOptions = {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  };
  const response = await fetchAuthenticated(
    `/api/subscriptions${path}`,
    accessToken,
    requestOptions,
  );
  const responseBody = await readResponse(response);
  if (!response.ok) {
    throwResponseError(response, responseBody);
  }
  return responseBody;
}

export async function apiRequest(path, accessToken, options = {}) {
  const { responseType, ...fetchOptions } = options;
  const requestOptions = {
    ...fetchOptions,
    headers: {
      ...(fetchOptions.body ? { "Content-Type": "application/json" } : {}),
      ...fetchOptions.headers,
    },
  };
  const response = await fetchAuthenticated(`/api${path}`, accessToken, requestOptions);
  if (responseType === "blob" && response.ok) {
    return response.blob();
  }

  const responseBody = await readResponse(response);
  if (!response.ok) {
    throwResponseError(response, responseBody);
  }
  return responseBody;
}

export function categoryRequest(path, accessToken, options) {
  return apiRequest(`/categories${path}`, accessToken, options);
}

export function currencyRequest(path, accessToken, options) {
  return apiRequest(`/currencies${path}`, accessToken, options);
}
