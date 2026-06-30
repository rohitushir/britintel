package com.companieswatch.watchlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.companieswatch.account.User;
import com.companieswatch.account.UserRepository;
import com.companieswatch.companieshouse.rest.CompaniesHouseRestClient;
import com.companieswatch.companieshouse.rest.CompanyNotFoundException;
import com.companieswatch.companieshouse.rest.CompanyProfile;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class WatchListFlowTest {

    /** The Clerk user id (JWT {@code sub}) the seeded account is keyed by. */
    private static final String CLERK_SUB = "user_test_clerk_id";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired WatchedCompanyRepository watchedCompanyRepository;

    @MockitoBean CompaniesHouseRestClient restClient;

    private Long userId;

    @BeforeEach
    void setUp() {
        watchedCompanyRepository.deleteAll();
        userRepository.deleteAll();
        User user = new User("lender@firm.com", CLERK_SUB);
        user.setCompanyCap(2);
        userId = userRepository.save(user).getId();
    }

    /** A mock Clerk session token for the seeded user (provisioning resolves it by {@code sub}). */
    private static RequestPostProcessor asUser() {
        return jwt().jwt(token -> token.subject(CLERK_SUB));
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void addsCompanyWithBackfillAndCachesName() throws Exception {
        when(restClient.fetchProfile(eq("00000006")))
                .thenReturn(new CompanyProfile("00000006", "ACME LTD", "active",
                        LocalDate.of(2000, 1, 1), "{}", "{}"));

        mvc.perform(post("/api/watchlist").with(asUser())
                        .contentType("application/json")
                        .content("{\"companyNumber\":\"00000006\"}"))
                .andExpect(status().isCreated());

        var watched = watchedCompanyRepository.findByUserIdAndCompanyNumber(userId, "00000006");
        assertThat(watched).isPresent();
        assertThat(watched.get().getCompanyName()).isEqualTo("ACME LTD");
    }

    @Test
    void rejectsUnknownCompany() throws Exception {
        when(restClient.fetchProfile(eq("99999999")))
                .thenThrow(new CompanyNotFoundException("99999999"));

        mvc.perform(post("/api/watchlist").with(asUser())
                        .contentType("application/json")
                        .content("{\"companyNumber\":\"99999999\"}"))
                .andExpect(status().isNotFound());

        assertThat(watchedCompanyRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void enforcesPerAccountCap() throws Exception {
        when(restClient.fetchProfile(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> new CompanyProfile(inv.getArgument(0), "X", "active",
                        null, "{}", "{}"));

        add("00000001").andExpect(status().isCreated());
        add("00000002").andExpect(status().isCreated());
        // cap is 2 -> third add is rejected.
        add("00000003").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void removesCompany() throws Exception {
        when(restClient.fetchProfile(eq("00000006")))
                .thenReturn(new CompanyProfile("00000006", "ACME LTD", "active", null, "{}", "{}"));
        add("00000006").andExpect(status().isCreated());

        mvc.perform(delete("/api/watchlist/00000006").with(asUser()))
                .andExpect(status().isNoContent());
        assertThat(watchedCompanyRepository.findByUserId(userId)).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions add(String number) throws Exception {
        return mvc.perform(post("/api/watchlist").with(asUser())
                .contentType("application/json")
                .content("{\"companyNumber\":\"" + number + "\"}"));
    }
}
