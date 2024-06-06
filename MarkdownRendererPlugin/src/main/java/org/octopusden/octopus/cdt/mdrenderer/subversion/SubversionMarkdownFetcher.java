package org.octopusden.octopus.cdt.mdrenderer.subversion;

import org.octopusden.octopus.cdt.mdrenderer.renderingengine.IMarkdownFetcher;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;

import java.io.*;
import java.net.*;
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
        
        basicAuth = "Basic " + printBase64Binary((sourceProperties.getSourceUsername() + ":" + sourceProperties.getSourcePassword()).getBytes());
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
