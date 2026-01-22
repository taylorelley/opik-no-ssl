package com.comet.opik.infrastructure.http;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class InsecureTlsConfig {

    private static final TrustManager[] TRUST_ALL_MANAGERS = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust all client certificates
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust all server certificates
                }
            }
    };

    private static final HostnameVerifier TRUST_ALL_HOSTNAMES = (hostname, session) -> true;

    public static void disableSslVerificationGlobally() {
        try {
            SSLContext sslContext = buildInsecureSslContext();
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TRUST_ALL_HOSTNAMES);
            log.warn("TLS verification disabled globally for outbound HTTPS connections.");
        } catch (Exception e) {
            log.error("Failed to disable TLS verification globally: {}", e.getMessage());
        }
    }

    public static HttpClient.Builder applyInsecureTls(HttpClient.Builder builder) {
        SSLContext sslContext = buildInsecureSslContext();
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("");
        return builder.sslContext(sslContext).sslParameters(sslParameters);
    }

    public static JdkHttpClientBuilder insecureJdkHttpClientBuilder() {
        HttpClient.Builder httpClientBuilder = applyInsecureTls(HttpClient.newBuilder());
        return JdkHttpClient.builder().httpClientBuilder(httpClientBuilder);
    }

    private static SSLContext buildInsecureSslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_MANAGERS, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize insecure SSL context", e);
        }
    }
}
