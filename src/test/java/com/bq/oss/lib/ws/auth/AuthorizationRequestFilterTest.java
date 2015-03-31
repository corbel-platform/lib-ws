/*
 * Copyright (C) 2014 StarTIC
 */
package com.bq.oss.lib.ws.auth;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import com.bq.oss.lib.token.TokenInfo;
import com.bq.oss.lib.token.reader.TokenReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.inject.Injectable;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.oauth.OAuthProvider;

/**
 * @author Alexander De Leon
 *
 */
public class AuthorizationRequestFilterTest {

	private static final String TEST_PATH = "/test";

	private static final String TEST_USER = "user";
	private static final String TEST_NOT_SECURIZED_PATH = "/not_securize_path";

	private OAuthProvider<AuthorizationInfo> providerMock;
	private CookieOAuthProvider<AuthorizationInfo> cookieProviderMock;
	private ContainerRequest requestMock;
	private AuthorizationInfo authorizationInfoMock;
	private final JsonParser jsonParser = new JsonParser();

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		providerMock = mock(OAuthProvider.class);
		cookieProviderMock = mock(CookieOAuthProvider.class);
		requestMock = mock(ContainerRequest.class);
		authorizationInfoMock = mock(AuthorizationInfo.class);
		AbstractHttpContextInjectable<AuthorizationInfo> injectableMock = mock(AbstractHttpContextInjectable.class);
		when(injectableMock.getValue(Mockito.any(HttpContext.class))).thenReturn(authorizationInfoMock);
		when(
				providerMock.getInjectable(Mockito.any(ComponentContext.class), Mockito.any(Auth.class),
						Mockito.any(Parameter.class))).thenReturn((Injectable) injectableMock);

	}

	@Test(expected = WebApplicationException.class)
	public void testPatternMatches() {
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock,
				TEST_NOT_SECURIZED_PATH);
		stubRequest(TEST_PATH, HttpMethod.GET);
		filter.filter(requestMock);
	}

	@Test
	public void testPatternNoMatches() {
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock, TEST_PATH);
		stubRequest(TEST_PATH, HttpMethod.GET);
		assertThat(filter.filter(requestMock)).isSameAs(requestMock);
	}

	@Test
	public void testCORSIsSkiped() {
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock, ".*");
		stubRequest(TEST_PATH, HttpMethod.OPTIONS);
		assertThat(filter.filter(requestMock)).isSameAs(requestMock);
	}

	@Test
	public void testHttpAccessRule() {
		TokenReader tokenReader = mock(TokenReader.class);
		when(authorizationInfoMock.getTokenReader()).thenReturn(tokenReader);
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock, ".*");
		stubRequest(TEST_PATH, HttpMethod.GET);
		when(requestMock.getAcceptableMediaTypes()).thenReturn(Arrays.asList(MediaType.APPLICATION_JSON_TYPE));
		stubRules(jsonParser.parse(
				"{\"type\":\"http_access\", \"mediaTypes\":[ \"application/json\"], \"methods\":[\"GET\"], \"uri\": \""
						+ TEST_PATH + "\"}").getAsJsonObject());
		assertThat(filter.filter(requestMock)).isSameAs(requestMock);
	}

	@Test
	public void testHttpAccessRuleWithGenericMediaType() {
		TokenReader tokenReader = mock(TokenReader.class);
		when(authorizationInfoMock.getTokenReader()).thenReturn(tokenReader);
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock, ".*");
		stubRequest(TEST_PATH, HttpMethod.GET);
		when(requestMock.getAcceptableMediaTypes()).thenReturn(
				Arrays.asList(MediaType.TEXT_HTML_TYPE, MediaType.APPLICATION_JSON_TYPE));
		stubRules(jsonParser
				.parse("{\"type\":\"http_access\", \"mediaTypes\":[ \"music/mp3\",\"text/plain\",\"application/*\"], \"methods\":[\"GET\"], \"uri\": \""
						+ TEST_PATH + "\"}").getAsJsonObject());
		assertThat(filter.filter(requestMock)).isSameAs(requestMock);
	}

	@Test(expected = WebApplicationException.class)
	public void testNoRules() {
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock,
				TEST_NOT_SECURIZED_PATH);
		stubRequest(TEST_PATH, HttpMethod.GET);
		when(requestMock.getAcceptableMediaTypes()).thenReturn(Arrays.asList(MediaType.APPLICATION_JSON_TYPE));
		stubRules(); // no rules
		assertThat(filter.filter(requestMock)).isSameAs(requestMock);
	}

	@Test
	public void testTokenTypeRule() {
		TokenReader tokenReader = mock(TokenReader.class);
		TokenInfo tokenMock = mock(TokenInfo.class);
		when(tokenReader.getInfo()).thenReturn(tokenMock);
		when(tokenMock.getUserId()).thenReturn(TEST_USER);
		when(authorizationInfoMock.getTokenReader()).thenReturn(tokenReader);
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock, ".*");
		when(requestMock.getAcceptableMediaTypes()).thenReturn(Arrays.asList(MediaType.APPLICATION_JSON_TYPE));
		stubRequest(TEST_PATH, HttpMethod.GET);
		stubRules(jsonParser.parse(
				"{\"type\":\"http_access\", \"mediaTypes\":[ \"application/json\"], \"methods\":[\"GET\"], \"uri\": \""
						+ TEST_PATH + "\", \"tokenType\":\"user\"}").getAsJsonObject());
		assertThat(filter.filter(requestMock)).isSameAs(requestMock);
	}

	@Test(expected = WebApplicationException.class)
	public void testTokenTypeRuleAccessDenied() {
		TokenInfo tokenMock = mock(TokenInfo.class);
		TokenReader tokenReader = mock(TokenReader.class);
		when(tokenReader.getInfo()).thenReturn(tokenMock);
		when(tokenMock.getUserId()).thenReturn(null);
		when(authorizationInfoMock.getTokenReader()).thenReturn(tokenReader);
		AuthorizationRequestFilter filter = new AuthorizationRequestFilter(providerMock, cookieProviderMock,
				TEST_NOT_SECURIZED_PATH);
		when(requestMock.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
		stubRequest(TEST_PATH, HttpMethod.GET);
		stubRules(jsonParser.parse(
				"{\"type\":\"http_access\", \"mediaTypes\":[ \"application/json\"], \"methods\":[\"GET\"], \"uri\": \""
						+ TEST_PATH + "\", \"tokenType\":\"user\"}").getAsJsonObject());
		filter.filter(requestMock);
	}

	private void stubRules(JsonObject... rules) {
		when(authorizationInfoMock.getAccessRules()).thenReturn(Sets.newHashSet(rules));
	}

	private void stubRequest(String path, String method) {
		when(requestMock.getPath()).thenReturn(path);
		when(requestMock.getMethod()).thenReturn(method);
	}
}