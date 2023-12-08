package org.octopusden.confluence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class BitbucketFileDownloader {
    private final String basicUrl;
    private final String basicAuth;

    public BitbucketFileDownloader(String bitbucketUrl, String projectKey, String repository, String user, String password) throws NullPointerException, UnsupportedEncodingException {
        basicUrl = bitbucketUrl + ((bitbucketUrl.endsWith("/")) ? "" : "/") +
                "rest/api/1.0/projects/" + URLEncoder.encode(projectKey, "UTF-8") +
                "/repos/" + URLEncoder.encode(repository, "UTF-8");
        basicAuth = "Basic " + printBase64Binary((user + ":" + password).getBytes());
    }

    private void checkType(String urlPath) throws Exception {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(basicUrl + "/browse" + urlPath + "&type=true").openConnection();
        httpsURLConnection.setRequestProperty("Authorization", basicAuth);
        boolean isError = httpsURLConnection.getResponseCode() / 100 != 2;
        try (InputStream inputStream = (isError) ? httpsURLConnection.getErrorStream() : httpsURLConnection.getInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            if (isError) {
                StringBuilder text = new StringBuilder("**Errors**\n");
                for (Error error : gson.fromJson(bufferedReader, ErrorResponse.class).errors) {
                    text.append("* ").append(error.message).append('\n');
                }
                throw new BitbucketFileDownloaderException(text.toString());
            } else {
                String type = gson.fromJson(bufferedReader, TypeResponse.class).type;
                if (!"FILE".equalsIgnoreCase(type)) {
                    throw new BitbucketFileDownloaderException("**Invalid URL**\nType: " + type);
                }
            }
        }
    }

    private byte[] download(String urlPath) throws Exception {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(basicUrl + "/raw" + urlPath).openConnection();
        httpsURLConnection.setRequestProperty("Authorization", basicAuth);
        boolean isError = httpsURLConnection.getResponseCode() / 100 != 2;
        if (isError) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder text = new StringBuilder("**Errors**\n");
                for (Error error : gson.fromJson(bufferedReader, ErrorResponse.class).errors) {
                    text.append("* ").append(error.message).append('\n');
                }
                throw new BitbucketFileDownloaderException(text.toString());
            }
        } else {
            try (InputStream inputStream = httpsURLConnection.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int count;
                byte[] data = new byte[8192];
                while ((count = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, count);
                }
                buffer.flush();
                return buffer.toByteArray();
            }
        }
    }

    public byte[] get(String path, String revision) throws Exception {
        if(!path.startsWith("/")) {
            path = "/" + path;
        }

        String asciiPath = new URI(null, null, path, null).toASCIIString()
                + "?at="
                + URLEncoder.encode(revision, "UTF-8");

        checkType(asciiPath);

        return download(asciiPath);
    }

    private static final Gson gson = new GsonBuilder().create();

    private static class Error {
        String message;
    }

    private static class ErrorResponse {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        List<Error> errors;
    }

    private static class TypeResponse {
        String type;
    }

    public static class BitbucketFileDownloaderException extends Exception {
        private static final long serialVersionUID = -6023227236242014753L;

        private BitbucketFileDownloaderException(String message) {
            super(message);
        }
    }
}
