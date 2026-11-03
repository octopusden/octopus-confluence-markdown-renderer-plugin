package org.octopusden.octopus.cdt.mdrenderer.git;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;
import org.octopusden.octopus.cdt.mdrenderer.renderingengine.IMarkdownFetcher;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class GitMarkdownFetcher implements IMarkdownFetcher {
    private static final String IMAGE_PATH_GROUP = "path";
    private static final Pattern REPLACE_PATTERN = Pattern.compile("!\\[.*]\\((?<" + IMAGE_PATH_GROUP + ">.+)\\)");
    private static final Gson GSON = new GsonBuilder().create();
    private final String basicUrl;
    private final String basicAuth;
    private final String path;
    private final String revision;
    private static class Error {
        String message;
    }
    private static class ErrorResponse {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        List<GitMarkdownFetcher.Error> errors;
    }
    private static class TypeResponse {
        String type;
    }
    public static class GitMarkdownFetcherException extends Exception {
        private static final long serialVersionUID = -6023227236242014753L;
        private GitMarkdownFetcherException(String message) {
            super(message);
        }
    }

    public GitMarkdownFetcher(SourceProperties sourceProperties, String projectKey, String repository, String path, String revision) throws NullPointerException, UnsupportedEncodingException {
        this.basicUrl = sourceProperties.getSourceUrl() + ((sourceProperties.getSourceUrl().endsWith("/")) ? "" : "/") +
                "rest/api/1.0/projects/" + URLEncoder.encode(projectKey, "UTF-8") +
                "/repos/" + URLEncoder.encode(repository, "UTF-8");

        this.basicAuth = "Basic " + printBase64Binary((sourceProperties.getSourceUsername() + ":" + sourceProperties.getSourcePassword()).getBytes());
        this.path = path;
        this.revision = revision;
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
                for (GitMarkdownFetcher.Error error : GSON.fromJson(bufferedReader, GitMarkdownFetcher.ErrorResponse.class).errors) {
                    text.append("* ").append(error.message).append('\n');
                }
                throw new GitMarkdownFetcherException(text.toString());
            } else {
                String type = GSON.fromJson(bufferedReader, GitMarkdownFetcher.TypeResponse.class).type;
                if (!"FILE".equalsIgnoreCase(type)) {
                    throw new GitMarkdownFetcherException("**Invalid URL**\nType: " + type);
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
                for (GitMarkdownFetcher.Error error : GSON.fromJson(bufferedReader, GitMarkdownFetcher.ErrorResponse.class).errors) {
                    text.append("* ").append(error.message).append('\n');
                }
                throw new GitMarkdownFetcherException(text.toString());
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
    
    @Override
    public String fetch() {
        String markdown;
        try {
            String fileText = new String(get(path, revision), StandardCharsets.UTF_8);

            String imageBasicPath = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            StringBuilder markdownBuilder = new StringBuilder();
            Matcher matcher = REPLACE_PATTERN.matcher(fileText);

            int cursor = 0;
            while (matcher.find(cursor)) {
                markdownBuilder.append(fileText, cursor, matcher.start(IMAGE_PATH_GROUP));
                String imageRelativePath = matcher.group(IMAGE_PATH_GROUP).replace('\\', '/');
                int imageRelativePathLength = imageRelativePath.length();
                if (imageRelativePathLength > 4 && ".png".equalsIgnoreCase(imageRelativePath.substring(imageRelativePathLength - 4))) {
                    String imagePath = imageBasicPath + (imageRelativePath.startsWith("/") ? imageRelativePath : "/" + imageRelativePath);
                    try {
                        String imageData = printBase64Binary(get(imagePath, revision));
                        markdownBuilder.append("data:image/png;base64,").append(imageData);
                    } catch (Exception e) {
                        markdownBuilder.append(matcher.group(IMAGE_PATH_GROUP));
                    }
                } else {
                    markdownBuilder.append(matcher.group(IMAGE_PATH_GROUP));
                }
                cursor = matcher.end(IMAGE_PATH_GROUP);
            }

            markdownBuilder.append(fileText.substring(cursor));
            markdown = markdownBuilder.toString();
        } catch (GitMarkdownFetcherException bfde) {
            markdown = bfde.getMessage();
        } catch (MalformedURLException mue) {
            markdown = "**Invalid URL.**\n" + mue.getMessage();
        } catch (ClassCastException cce) {
            markdown = "**Unsupported protocol. Use HTTPS.**";
        } catch (IndexOutOfBoundsException iaobe) {
            markdown = "**Resource is too large.**";
        } catch (Exception e) {
            markdown = "**Exception**\n" + e.getMessage() + "\n```\n" + getStackTrace(e) + "\n```";
        }
        
        return markdown;
    }

    private static String getStackTrace(Exception e) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (Exception e2) {
            return "getStackTrace exception: " + e2.getMessage();
        }
    }
}
