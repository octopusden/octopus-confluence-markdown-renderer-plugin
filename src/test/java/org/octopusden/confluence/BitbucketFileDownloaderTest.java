package org.octopusden.confluence;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import org.octopusden.confluence.BitbucketFileDownloader;
import lombok.val;
import org.junit.*;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class BitbucketFileDownloaderTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());
    private static final Gson gson = new Gson();

    @BeforeClass
    public static void configureSSL() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }

        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    @After
    public void clearMapping() {
        wireMockRule.resetRequests();
        wireMockRule.resetMappings();
    }

    protected void testPlugin(
            String projectKey,
            String repository,
            String path,
            String revision,
            String realBaseUrl,
            String realPath,
            String realRevision
    ) throws Exception {
        String mockUrl = "https://localhost:" + wireMockRule.httpsPort();

        val plugin = new BitbucketFileDownloader(
                mockUrl,
                projectKey,
                repository,
                "user",
                "password"
        );

        val typeResponseMap = new HashMap<>();
        typeResponseMap.put("type", "FILE");
        val typeResponse = gson.toJson(typeResponseMap);
        val fileResponse = new byte[10];

        Random random = new Random();
        random.nextBytes(fileResponse);

        stubFor(get(urlPathMatching(realBaseUrl + "/browse" + realPath))
                .withQueryParam("at", equalTo(realRevision))
                .withQueryParam("type", equalTo("true"))
                .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNzd29yZA=="))
                .willReturn(aResponse().withBody(typeResponse).withStatus(200)));

        stubFor(get(urlPathMatching(realBaseUrl + "/raw" + realPath))
                .withQueryParam("at", equalTo(realRevision))
                .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNzd29yZA=="))
                .willReturn(aResponse().withBody(fileResponse).withStatus(200)));

        val result = plugin.get(path, revision);

        verify(1, getRequestedFor(urlPathMatching(realBaseUrl + "/browse" + realPath))
                .withQueryParam("at", equalTo(realRevision))
                .withQueryParam("type", equalTo("true"))
                .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNzd29yZA=="))
        );

        verify(1, getRequestedFor(urlPathMatching(realBaseUrl + "/raw" + realPath))
                .withQueryParam("at", equalTo(realRevision))
                .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNzd29yZA=="))
        );

        Assert.assertArrayEquals(fileResponse, result);
    }
}
