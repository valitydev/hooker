package dev.vality.hooker.service;

import dev.vality.hooker.logging.HttpLoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PostSender {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String SIGNATURE_HEADER = "Content-Signature";
    public static final long RESPONSE_MAX_LENGTH = 4096L;
    private final OkHttpClient httpClient;

    public PostSender(int connectionPoolSize, int timeout) {
        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();

        if (log.isDebugEnabled()) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(message -> log.debug(message));
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpBuilder.addInterceptor(httpLoggingInterceptor);
        }

        ConnectionPool connectionPool = new ConnectionPool(2 * connectionPoolSize, 5, TimeUnit.MINUTES);
        this.httpClient = httpBuilder
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(false)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    public int doPost(String url, long messageId, String paramsAsString, String signature) throws IOException {
        log.info("Sending message with id {}, {} to hook: {} ", messageId, paramsAsString, url);
        RequestBody body = RequestBody.create(JSON, paramsAsString);
        final Request request = new Request.Builder()
                .url(url)
                .addHeader(SIGNATURE_HEADER, "alg=RS256; digest=" + signature)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            log.info("Response from hook: messageId: {}, code: {}; body: {}", messageId, response.code(),
                    response.body() != null ? response.peekBody(RESPONSE_MAX_LENGTH).string() : "<empty>");
            return response.code();
        }
    }
}
