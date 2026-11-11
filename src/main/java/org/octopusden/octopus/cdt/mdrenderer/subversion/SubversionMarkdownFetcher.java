package org.octopusden.octopus.cdt.mdrenderer.subversion;

import org.octopusden.octopus.cdt.mdrenderer.renderingengine.IMarkdownFetcher;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class SubversionMarkdownFetcher implements IMarkdownFetcher {
    private final String basicUrl;
    private final String basicAuth;
    public SubversionMarkdownFetcher(SourceProperties sourceProperties, String svnCountry, String clientCode, String svnBranchPath, String path) throws NullPointerException, UnsupportedEncodingException {
        basicUrl = sourceProperties.getSourceUrl() + ((sourceProperties.getSourceUrl().endsWith("/")) ? "" : "/") +
                URLEncoder.encode(svnCountry, "UTF-8") + 
                "/" +
                URLEncoder.encode(clientCode, "UTF-8") +
                (svnBranchPath.startsWith("/") ? "" : "/") + 
                svnBranchPath +
                (path.startsWith("/") ? "" : "/") +
                path;
        
        basicAuth = "Basic " + printBase64Binary((sourceProperties.getSourceUsername() + ":" + sourceProperties.getSourcePassword()).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String fetch() {
        String markdown;
        try {
            URL url = new URL(basicUrl);
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", basicAuth);

            InputStream inputStream = urlConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int size;
            while ((size = bufferedReader.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, size));
            }
            bufferedReader.close();
            markdown = stringBuilder.toString();
        } catch (MalformedURLException e) {
            markdown = "Exception[" + e + "] during URL creation.";
        } catch (IOException e) {
            markdown = "Exception[" + e + "] during connection opening.";
        } catch (IndexOutOfBoundsException e) {
            markdown = "Exception[" + e + "] due to Resource too large.";
        }
        
        return markdown;
    }
}
