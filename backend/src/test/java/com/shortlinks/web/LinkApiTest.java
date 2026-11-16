package com.shortlinks.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

/** End-to-end tests of the HTTP API against an in-memory H2 database. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:linkapitest;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LinkApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private String createLink(String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("code").asText();
    }

    @Test
    void createsLinkRedirectsAndCountsClicks() throws Exception {
        String code = createLink("{\"url\":\"https://example.com/page\"}");

        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/page"));
        mvc.perform(get("/" + code)).andExpect(status().isFound());

        mvc.perform(get("/api/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"))
                .andExpect(jsonPath("$.clickCount").value(2));
    }

    @Test
    void duplicateAliasIsConflict() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"alias\":\"dup-alias\"}";
        createLink(body);

        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidInputIsBadRequest() throws Exception {
        for (String body : new String[] {
                "{\"url\":\"ftp://example.com\"}",
                "{\"url\":\"not a url\"}",
                "{\"url\":\"\"}",
                "{\"url\":\"https://example.com\",\"alias\":\"api\"}",
                "{\"url\":\"https://example.com\",\"alias\":\"a!\"}",
                "{\"url\":\"https://example.com\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}" }) {
            mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void unknownCodeIsNotFound() throws Exception {
        mvc.perform(get("/doesnotexist")).andExpect(status().isNotFound());
        mvc.perform(get("/api/links/doesnotexist")).andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesLink() throws Exception {
        String code = createLink("{\"url\":\"https://example.com\",\"alias\":\"to-delete\"}");

        mvc.perform(delete("/api/links/" + code)).andExpect(status().isNoContent());
        mvc.perform(get("/api/links/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void listContainsCreatedLink() throws Exception {
        String code = createLink("{\"url\":\"https://example.com/listed\",\"alias\":\"listed-one\"}");

        mvc.perform(get("/api/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == '" + code + "')]").exists());
    }
}
