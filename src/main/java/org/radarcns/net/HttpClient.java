package org.radarcns.net;

import org.radarcns.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;

public class HttpClient {
    private final static Logger logger = LoggerFactory.getLogger(HttpClient.class);
    public final static String KAFKA_REST_ACCEPT_ENCODING = "application/vnd.kafka.v1+json, application/vnd.kafka+json, application/json";
    public final static String KAFKA_REST_AVRO_ENCODING = "application/vnd.kafka.avro.v1+json; charset=utf-8";

    public interface HttpOutputstreamWriter {
        /** Write any output without closing the stream. */
        void handleOutput(OutputStream out) throws IOException;
    }

    public static HttpResponse request(URL url, String method, HttpOutputstreamWriter writer, Map<String, String> requestProperties) throws IOException {
        String responseContent = null;
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        setProperties(urlConnection, requestProperties, true, true);
        urlConnection.setRequestMethod(method);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setChunkedStreamingMode(0);

        try {
            urlConnection.connect();
            try (OutputStream out = urlConnection.getOutputStream()) {
                writer.handleOutput(out);
            }
            responseContent = getInput(urlConnection);

            return new HttpResponse(urlConnection.getResponseCode(), urlConnection.getHeaderFields(), responseContent);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.warn("Failed HTTP {} request to {} (status code {}, response {}): {}", method, url, urlConnection.getResponseCode(), responseContent, ex.toString(), ex);
            throw ex;
        } finally {
            urlConnection.disconnect();
        }
    }

    private static void setProperties(URLConnection urlConnection, Map<String, String> requestProperties, boolean hasInput, boolean hasOutput) {
        if (hasInput) {
            urlConnection.setRequestProperty("Accept", KAFKA_REST_ACCEPT_ENCODING);
            urlConnection.setRequestProperty("Accept-Encoding", "identity");
        }
        if (hasOutput) {
            urlConnection.setRequestProperty("Content-Type", KAFKA_REST_AVRO_ENCODING);
        }
        if (requestProperties != null) {
            for (Map.Entry<String, String> requestProperty : requestProperties.entrySet()) {
                urlConnection.setRequestProperty(requestProperty.getKey(), requestProperty.getKey());
            }
        }
    }

    private static String getInput(HttpURLConnection urlConnection) throws IOException {
        if (urlConnection.getResponseCode() < 400) {
            try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                return IO.readInputStream(in);
            }
        } else {
            try (InputStream in = new BufferedInputStream(urlConnection.getErrorStream())) {
                return IO.readInputStream(in);
            }
        }
    }

    public static HttpResponse head(URL url, Map<String, String> requestProperties) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        setProperties(urlConnection, requestProperties, false, false);
        urlConnection.setRequestMethod("HEAD");

        urlConnection.setDoInput(false);
        urlConnection.setDoOutput(false);
        try {
            urlConnection.connect();
            return new HttpResponse(urlConnection.getResponseCode(), urlConnection.getHeaderFields(), null);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.warn("Failed HTTP HEAD request to {} (status code {}): {}", url, urlConnection.getResponseCode(), ex.toString(), ex);
            throw ex;
        } finally {
            urlConnection.disconnect();
        }
    }

    public static HttpResponse request(URL url, String method, String data, Map<String, String> requestProperties) throws IOException {
        if (Objects.equals(method, "HEAD")) {
            return head(url, requestProperties);
        }
        String responseContent = null;
        boolean hasOutput = data != null;

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        setProperties(urlConnection, requestProperties, true, hasOutput);
        urlConnection.setRequestMethod(method);

        urlConnection.setDoInput(true);

        byte[] bytes;

        if (hasOutput) {
            urlConnection.setDoOutput(true);
            bytes = data.getBytes("UTF-8");
            urlConnection.setFixedLengthStreamingMode(bytes.length);
        } else {
            urlConnection.setDoOutput(false);
            bytes = null;
        }

        try {
            urlConnection.connect();
            if (hasOutput) {
                try (OutputStream out = urlConnection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            responseContent = getInput(urlConnection);

            return new HttpResponse(urlConnection.getResponseCode(), urlConnection.getHeaderFields(), responseContent);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.warn("Failed HTTP {} request to {} (status code {}, response {}): {} {}", method, url, urlConnection.getResponseCode(), responseContent, ex.toString(), data, ex);
            throw ex;
        } finally {
            urlConnection.disconnect();
        }
    }
}
