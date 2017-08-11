package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.testsupport.WireMatchers;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.testsupport.NoFileSource.noFileSource;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class HandlebarsJsonHelperTest extends HandlebarsHelperTestBase {

    private HandlebarsJsonHelper helper;
    private ResponseTemplateTransformer transformer;

    @Before
    public void init() {
        helper = new HandlebarsJsonHelper();
        transformer = new ResponseTemplateTransformer(true);

        LocalNotifier.set(new ConsoleNotifier(true));
    }

    @Test
    public void mergesASimpleValueFromRequestIntoResponseBody() {
        final ResponseDefinition responseDefinition = this.transformer.transform(
                mockRequest()
                    .url("/json").
                    body("{\"a\": {\"test\": \"success\"}}"),
                aResponse()
                    .withBody("{\"test\": \"{{jsonPath request.body '$.a.test'}}\"}").build(),
                noFileSource(),
                Parameters.empty());

        assertThat(responseDefinition.getBody(), is("{\"test\": \"success\"}"));
    }

    @Test
    public void incluesAnErrorInTheResponseBodyWhenTheJsonPathExpressionReturnsNothing() {
        final ResponseDefinition responseDefinition = this.transformer.transform(
                mockRequest()
                    .url("/json")
                    .body("{\"a\": {\"test\": \"success\"}}"),
                aResponse()
                    .withBody("{\"test\": \"{{jsonPath request.body '$.b.test'}}\"}").build(),
                noFileSource(),
                Parameters.empty());

        assertThat(responseDefinition.getBody(), startsWith("{\"test\": \"" + HandlebarsHelper.ERROR_PREFIX));
    }

    @Test
    public void extractsASimpleValueFromTheInputJson() throws IOException {
        testHelper(helper,"{\"test\":\"success\"}", "$.test", "success");
    }

    @Test
    public void extractsAJsonObjectFromTheInputJson() throws IOException {
        testHelper(helper,
            "{                          \n" +
                "    \"outer\": {               \n" +
                "        \"inner\": \"Sanctum\" \n" +
                "    }                          \n" +
                "}",
            "$.outer",
            equalToJson("{                         \n" +
                "        \"inner\": \"Sanctum\" \n" +
                "    }"));
    }

    @Test
    public void extractsAJsonArrayFromTheInputJson() throws IOException {
        testHelper(helper,
            "{\n" +
                "    \"things\": [1, 2, 3]\n" +
                "}",
            "$.things",
            equalToJson("[1, 2, 3]"));
    }

    @Test
    public void rendersAMeaningfulErrorWhenInputJsonIsInvalid() {
        testHelperError(helper, "{\"test\":\"success}", "$.test", is("[ERROR: {\"test\":\"success} is not valid JSON]"));
    }

    @Test
    public void rendersAMeaningfulErrorWhenJsonPathIsInvalid() {
        testHelperError(helper, "{\"test\":\"success\"}", "$.\\test", is("[ERROR: $.\\test is not a valid JSONPath expression]"));
    }

    @Test
    public void rendersAnEmptyStringWhenJsonIsNull() {
        testHelperError(helper, null, "$.test", is(""));
    }

    @Test
    public void rendersAMeaningfulErrorWhenJsonPathIsNull() {
        testHelperError(helper, "{\"test\":\"success}", null, is("[ERROR: The JSONPath cannot be empty]"));
    }
}
