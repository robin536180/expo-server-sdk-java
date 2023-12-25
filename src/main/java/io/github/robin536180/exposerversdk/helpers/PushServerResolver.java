package io.github.robin536180.exposerversdk.helpers;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public interface PushServerResolver {
    public CompletableFuture<String> postAsync(URL url, String json) throws CompletionException;
}
