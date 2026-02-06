/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis.fhir.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.odoo.openelis.Constants;
import com.ozonehis.eip.odoo.openelis.DateUtils;
import com.ozonehis.eip.odoo.openelis.SyncUtils;
import com.ozonehis.eip.odoo.openelis.TestConfig;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TestConfig.class)
@EnableAutoConfiguration
@ComponentScan
@TestExecutionListeners({
    DirtiesContextBeforeModesTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
})
@TestPropertySource(properties = "EIP_ODOO_FHIR_URL=")
@TestPropertySource(properties = "EIP_ODOO_FHIR_USERNAME=")
@TestPropertySource(properties = "EIP_ODOO_FHIR_PASSWORD=")
public class FhirControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected static String TZ_OFFSET = ZonedDateTime.now().getOffset().toString();

    @Autowired
    private OdooFhirClient mockOdooClient;

    @Autowired
    private WebApplicationContext wac;

    protected MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @AfterEach
    public void tearDown() {
        SyncUtils.clearLastUpdatedTimestamps();
        Mockito.reset(mockOdooClient);
    }

    @Test
    public void createOrUpdate_shouldCreateOrUpdateTheResourceInOdoo() throws Exception {
        final String id = "12345";
        final String resType = "Patient";
        final LocalDateTime lastUpdated =
                ZonedDateTime.parse("2025-02-05T19:45:00.000" + TZ_OFFSET).toLocalDateTime();
        final Map<?, ?> data = Map.of("meta", Map.of("lastUpdated", DateUtils.serialize(lastUpdated)));
        final String body = MAPPER.writeValueAsString(data);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.put("/fhir/" + resType + "/" + id);
        builder.contentType(Constants.MEDIA_TYPE);
        builder.content(body);
        Mockito.when(mockOdooClient.update(resType, id, body)).thenReturn(HttpStatus.OK.value());

        ResultActions result = mockMvc.perform(builder);

        result.andExpect(status().isOk());
        Assertions.assertEquals(lastUpdated, SyncUtils.getLastUpdated(resType, id));
    }

    @Test
    public void createOrUpdate_shouldSwallowAnyThrownExceptions() throws Exception {
        final String id = "12345";
        final String resType = "Patient";
        final Map<?, ?> data = Map.of("meta", Map.of("lastUpdated", DateUtils.serialize(LocalDateTime.now())));
        final String body = MAPPER.writeValueAsString(data);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.put("/fhir/" + resType + "/" + id);
        builder.contentType(Constants.MEDIA_TYPE);
        builder.content(body);
        Mockito.when(mockOdooClient.update(resType, id, body)).thenThrow(new RuntimeException("Test"));
        LocalDateTime lastUpdated = LocalDateTime.now();
        SyncUtils.saveLastUpdated(resType, id, lastUpdated);

        ResultActions result = mockMvc.perform(builder);

        result.andExpect(status().isOk());
        Mockito.verify(mockOdooClient).update(resType, id, body);
        Assertions.assertEquals(lastUpdated, SyncUtils.getLastUpdated(resType, id));
    }

    @Test
    public void delete_ShouldDeleteTheResourceInOdoo() throws Exception {
        final String id = "12345";
        final String resType = "Patient";
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/fhir/" + resType + "/" + id);
        SyncUtils.saveLastUpdated(resType, id, LocalDateTime.now());

        ResultActions result = mockMvc.perform(builder);

        result.andExpect(status().isNoContent());
        Mockito.verify(mockOdooClient).delete(resType, id);
        Assertions.assertNull(SyncUtils.getLastUpdated(resType, id));
    }

    @Test
    public void delete_shouldSwallowAnyThrownExceptions() throws Exception {
        final String id = "12345";
        final String resType = "Patient";
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/fhir/" + resType + "/" + id);
        SyncUtils.saveLastUpdated(resType, id, LocalDateTime.now());
        Mockito.doThrow(new RuntimeException("Test")).when(mockOdooClient).delete(resType, id);
        LocalDateTime lastUpdated = LocalDateTime.now();
        SyncUtils.saveLastUpdated(resType, id, lastUpdated);

        ResultActions result = mockMvc.perform(builder);

        result.andExpect(status().isNoContent());
        Mockito.verify(mockOdooClient).delete(resType, id);
        Assertions.assertEquals(lastUpdated, SyncUtils.getLastUpdated(resType, id));
    }
}
