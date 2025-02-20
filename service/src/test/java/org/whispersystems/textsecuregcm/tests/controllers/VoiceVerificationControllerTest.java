/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.tests.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import io.dropwizard.auth.PolymorphicAuthValueFactoryProvider;
import io.dropwizard.testing.FixtureHelpers;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.util.Arrays;
import java.util.HashSet;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.whispersystems.textsecuregcm.auth.DisabledPermittedAccount;
import org.whispersystems.textsecuregcm.controllers.VoiceVerificationController;
import org.whispersystems.textsecuregcm.mappers.RateLimitExceededExceptionMapper;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.tests.util.AuthHelper;
import org.whispersystems.textsecuregcm.util.SystemMapper;

@ExtendWith(DropwizardExtensionsSupport.class)
class VoiceVerificationControllerTest {

  private static final ResourceExtension resources = ResourceExtension.builder()
                                                            .addProvider(AuthHelper.getAuthFilter())
                                                            .addProvider(new PolymorphicAuthValueFactoryProvider.Binder<>(ImmutableSet.of(Account.class, DisabledPermittedAccount.class)))
                                                            .addProvider(new RateLimitExceededExceptionMapper())
                                                            .setMapper(SystemMapper.getMapper())
                                                            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                                                            .addResource(new VoiceVerificationController("https://foo.com/bar",
                                                                                                         new HashSet<>(Arrays.asList("pt-BR", "ru"))))
                                                            .build();

  @Test
  void testTwimlLocale() {
    Response response =
        resources.getJerseyTest()
                 .target("/v1/voice/description/123456")
                 .queryParam("l", "pt-BR")
                 .request()
                 .post(null);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isXmlEqualTo(FixtureHelpers.fixture("fixtures/voice_verification_pt_br.xml"));
  }

  @Test
  void testTwimlSplitLocale() {
    Response response =
        resources.getJerseyTest()
                 .target("/v1/voice/description/123456")
                 .queryParam("l", "ru-RU")
                 .request()
                 .post(null);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isXmlEqualTo(FixtureHelpers.fixture("fixtures/voice_verification_ru.xml"));
  }

  @Test
  void testTwimlUnsupportedLocale() {
    Response response =
        resources.getJerseyTest()
                 .target("/v1/voice/description/123456")
                 .queryParam("l", "es-MX")
                 .request()
                 .post(null);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isXmlEqualTo(FixtureHelpers.fixture("fixtures/voice_verification_en_us.xml"));
  }

  @Test
  void testTwimlMultipleLocales() {
    Response response =
        resources.getJerseyTest()
            .target("/v1/voice/description/123456")
            .queryParam("l", "es-MX")
            .queryParam("l", "ru-RU")
            .request()
            .post(null);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isXmlEqualTo(FixtureHelpers.fixture("fixtures/voice_verification_ru.xml"));
  }

  @Test
  void testTwimlMissingLocale() {
    Response response =
        resources.getJerseyTest()
                 .target("/v1/voice/description/123456")
                 .request()
                 .post(null);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isXmlEqualTo(FixtureHelpers.fixture("fixtures/voice_verification_en_us.xml"));
  }


  @Test
  void testTwimlMalformedCode() {
    Response response =
        resources.getJerseyTest()
                 .target("/v1/voice/description/1234...56")
                 .request()
                 .post(null);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isXmlEqualTo(FixtureHelpers.fixture("fixtures/voice_verification_en_us.xml"));
  }

  @Test
  void testTwimlBadCodeLength() {
    Response response =
        resources.getJerseyTest()
                 .target("/v1/voice/description/1234567")
                 .request()
                 .post(null);

    assertThat(response.getStatus()).isEqualTo(400);

  }

  @Test
  void testTwimlMalformedLocale() {
    Response response =
        resources.getJerseyTest()
            .target("/v1/voice/description/123456")
            .queryParam("l", "it IT ,")
            .request()
            .post(null);

    assertThat(response.getStatus()).isEqualTo(400);
  }
}
