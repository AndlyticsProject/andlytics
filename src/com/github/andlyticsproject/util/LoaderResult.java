package com.github.andlyticsproject.util;


public class LoaderResult<T> {

    private T data;
    private Exception error;

    private LoaderResult(T data, Exception error) {
        this.data = data;
        this.error = error;
    }

    public static <T> LoaderResult<T> create(T data) {
        return new LoaderResult<T>(data, null);
    }

    public static <T> LoaderResult<T> createFailed(Exception error) {
        return new LoaderResult<T>(null, error);
    }

    public T getData() {
        return data;
    }

    public Exception getError() {
        return error;
    }

    public boolean isSuccessful() {
        return error == null;
    }

    public boolean isFailed() {
        return !isSuccessful();
    }
}
