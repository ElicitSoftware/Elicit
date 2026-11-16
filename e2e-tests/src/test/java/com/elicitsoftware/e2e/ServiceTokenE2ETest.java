package com.elicitsoftware.e2e;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The service token on the running stack (umbrella C-019, UC-023): the paths the modules call on
 * each other answer {@code 401} to a caller without the site's token, and the paths meant for
 * people and probes stay open. No browser; plain HTTP against the published ports.
 *
 * <p>The journeys in the other classes prove the positive half: a definition apply (Admin to
 * Survey), the Family History reports (Survey to FHHS, FHHS to Pedigree) and the preview reset
 * (Author to its Survey) only work when every caller sends the token.</p>
 */
class ServiceTokenE2ETest {

    private static final String SURVEY = System.getProperty("survey.baseUrl", "http://localhost:8080");
    private static final String FHHS = System.getProperty("fhhs.baseUrl", "http://localhost:8082");
    private static final String PEDIGREE = System.getProperty("pedigree.baseUrl", "http://localhost:8083");
    private static final String PREVIEW = System.getProperty("preview.baseUrl", "http://localhost:8085");

    // HTTP/1.1 only: Pedigree's server (httpuv) drops a request that offers the HTTP/2 upgrade.
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void surveyRefusesServiceCallsWithoutTheToken() throws Exception {
        assertEquals(401, post(SURVEY + "/api/etl/build", null));
        assertEquals(401, post(SURVEY + "/api/etl/build", "Bearer not-the-token-of-this-site-at-all"));
        assertEquals(401, post(SURVEY + "/api/reports/respondent-summary", null));
    }

    @Test
    void reportServicesRefuseCallsWithoutTheToken() throws Exception {
        assertEquals(401, post(FHHS + "/proband/report", null));
        assertEquals(401, post(FHHS + "/familyhistory/generate", null));
        assertEquals(401, post(PEDIGREE + "/svg", null));
    }

    @Test
    void previewSurveyRefusesTheSchemaDropWithoutTheToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(PREVIEW + "/api/etl/schema/00000000-0000-0000-0000-000000000000"))
                .DELETE().build();
        assertEquals(401, http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @Test
    void probesAndTheRespondentDownloadStayOpen() throws Exception {
        assertEquals(200, get(SURVEY + "/q/health/ready"));
        assertEquals(200, get(PEDIGREE + "/healthz"));
        // Keyed by the respondent's session, not by the service token: whatever it answers to a
        // caller without a session, it is not the service token's refusal.
        assertNotEquals(401, get(SURVEY + "/api/pdf/download"));
    }

    private int post(String url, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
