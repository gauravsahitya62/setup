import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";

import { env } from "@/lib/env";
import type { ApiError } from "@/types";

export class ApiClientError extends Error {
  status?: number;
  code?: string;
  details?: Record<string, string[]>;
  fieldErrors?: Record<string, string>;

  constructor(error: ApiError & { fieldErrors?: Record<string, string> }) {
    super(error.message);
    this.name = "ApiClientError";
    this.status = error.status;
    this.code = error.code;
    this.details = error.details;
    this.fieldErrors = error.fieldErrors;
  }
}

type TokenGetter = () => string | null;
type TokenSetter = (token: string | null) => void;
type RefreshHandler = () => Promise<string | null>;

let getAccessToken: TokenGetter = () => null;
let setAccessToken: TokenSetter = () => undefined;
let refreshAccessToken: RefreshHandler = async () => null;
let refreshPromise: Promise<string | null> | null = null;

export function bindAuthTokenHandlers(handlers: {
  getAccessToken: TokenGetter;
  setAccessToken: TokenSetter;
  refreshAccessToken: RefreshHandler;
}) {
  getAccessToken = handlers.getAccessToken;
  setAccessToken = handlers.setAccessToken;
  refreshAccessToken = handlers.refreshAccessToken;
}

export const axiosClient: AxiosInstance = axios.create({
  baseURL: env.NEXT_PUBLIC_API_BASE_URL.replace(/\/$/, ""),
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
  withCredentials: true,
});

axiosClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError & { fieldErrors?: Record<string, string> }>) => {
    const original = error.config as (AxiosRequestConfig & { _retry?: boolean }) | undefined;
    const status = error.response?.status;
    const url = original?.url ?? "";

    const isAuthPath =
      url.includes("/auth/login") ||
      url.includes("/auth/register") ||
      url.includes("/auth/refresh") ||
      url.includes("/auth/forgot-password") ||
      url.includes("/auth/reset-password");

    if (status === 401 && original && !original._retry && !isAuthPath) {
      original._retry = true;
      try {
        if (!refreshPromise) {
          refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null;
          });
        }
        const nextToken = await refreshPromise;
        if (nextToken) {
          original.headers = {
            ...original.headers,
            Authorization: `Bearer ${nextToken}`,
          };
          return axiosClient(original);
        }
      } catch {
        setAccessToken(null);
      }
    }

    const data = error.response?.data;
    throw new ApiClientError({
      message: data?.message || error.message || "Request failed",
      status: status ?? error.status,
      code: data?.code,
      details: data?.details,
      fieldErrors: data?.fieldErrors,
    });
  }
);

type RequestOptions = {
  params?: Record<string, string | number | boolean | undefined>;
  headers?: Record<string, string>;
  signal?: AbortSignal;
};

async function request<T>(
  method: "get" | "post" | "put" | "patch" | "delete",
  path: string,
  body?: unknown,
  options: RequestOptions = {}
): Promise<T> {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const response = await axiosClient.request<T>({
    method,
    url: normalizedPath,
    data: body,
    params: options.params,
    headers: options.headers,
    signal: options.signal,
  });
  return response.data;
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>("get", path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>("post", path, body, options),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>("put", path, body, options),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>("patch", path, body, options),
  delete: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>("delete", path, body, options),
};
