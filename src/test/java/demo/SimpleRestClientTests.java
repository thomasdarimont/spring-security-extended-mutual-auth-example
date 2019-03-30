package demo;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(
        classes = ExtendedMutualAuthExampleApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SimpleRestClientTests {

    @LocalServerPort
    private int port;

    @Value("${http.client.ssl.key-alias}")
    private String CLIENT_CERT;

    @Value("${http.client.ssl.key-store}")
    private String keyStorePath;

    @Value("${http.client.ssl.trust-store}")
    private String trustStorePath;

    @Value("${http.client.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${http.client.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${http.client.ssl.client-key-password}")
    private String clientKeyPassword;

    private PrivateKeyStrategy defaultAliasStrategy = (Map<String, PrivateKeyDetails> map, Socket socket) -> CLIENT_CERT;

    @Test
    public void givenValidCertificates_whenUsingHttpClient_thenCorrect()
            throws Exception {

        CloseableHttpClient client = createHttpClientWithSsl(defaultAliasStrategy);

        HttpGet httpGet = new HttpGet(getApiUrl());
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encodeBase64(
                "tester:test".getBytes(StandardCharsets.ISO_8859_1))));

        HttpResponse response = client.execute(httpGet);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }


    @Test
    public void givenValidCertificate_whenUsingRestTemplateWithoutClientCertWithoutBasicAuth_thenFail() {

        assertTrue(
                assertThrows(ResourceAccessException.class, () -> {
                    PrivateKeyStrategy aliasStrategy = (map, socket) -> "WRONG_CERT";

                    RestTemplate restTemplate = createRestTemplate(aliasStrategy);

                    restTemplate.exchange(getApiUrl(), HttpMethod.GET, null, String.class);
                }).getMessage().contains("bad_certificate"));
    }

    @Test
    public void givenValidCertificate_whenUsingRestTemplateWithoutBasicAuth_thenFail() {

        assertTrue(
                assertThrows(HttpClientErrorException.class, () -> {
                    CloseableHttpClient httpClient = createHttpClientWithSsl(defaultAliasStrategy);

                    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
                    requestFactory.setHttpClient(httpClient);

                    new RestTemplate(requestFactory).exchange(
                            getApiUrl(), HttpMethod.GET, null, String.class);
                }).getMessage().contains("401"));
    }

    @Test
    public void givenValidCertificateAndBasicAuth_whenUsingRestTemplate_thenCorrect()
            throws Exception {

        RestTemplate restTemplate = createRestTemplate(defaultAliasStrategy);
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor("tester", "test"));

        ResponseEntity<String> response = restTemplate.exchange(
                getApiUrl(), HttpMethod.GET, null, String.class);
        System.out.println(response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    public void givenValidCertificateAndWrongBasicAuth_whenUsingRestTemplate_thenFail() {

        assertTrue(
                assertThrows(HttpClientErrorException.class, () -> {

                    RestTemplate restTemplate = createRestTemplate(defaultAliasStrategy);
                    restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor("tester", "wrong"));

                    restTemplate.exchange(getApiUrl(), HttpMethod.GET, null, String.class);
                }).getMessage().contains("403"));
    }

    private RestTemplate createRestTemplate(PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {

        CloseableHttpClient httpClient = createHttpClientWithSsl(aliasStrategy);

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

    private CloseableHttpClient createHttpClientWithSsl(PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {

        SSLContext sslContext = SSLContextBuilder
                .create()
                .loadKeyMaterial(ResourceUtils.getFile(keyStorePath),
                        keyStorePassword.toCharArray(),
                        clientKeyPassword.toCharArray(),
                        aliasStrategy)
                .loadTrustMaterial(ResourceUtils.getFile(trustStorePath), trustStorePassword.toCharArray())
                .build();

        return HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build();
    }

    private String getApiUrl() {
        return "https://apps.tdlabs.local:" + port + "/api/hello";
    }
}
