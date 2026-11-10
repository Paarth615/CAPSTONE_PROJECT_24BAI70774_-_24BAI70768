package com.lifelink;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end check of the whole workflow through the real security chain and an in-memory database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lifelinktest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.enabled=false"
})
@AutoConfigureMockMvc
class LifeLinkFlowTest {

    @Autowired
    MockMvc mvc;

    private String register(String json) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions send(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        req.header("Authorization", "Bearer " + token);
        if (json != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(req);
    }

    private String donorJson(String email, String dob) {
        return """
                {"name":"Test Donor","email":"%s","password":"Password@123","role":"DONOR",
                 "phone":"9876543210","city":"Lucknow","latitude":26.85,"longitude":80.95,
                 "bloodGroup":"B+","dateOfBirth":"%s","weightKg":70}
                """.formatted(email, dob);
    }

    private String requestJson(String group, int units) {
        return """
                {"patientName":"Rohan Verma","bloodGroup":"%s","unitsRequired":%d,"hospital":"KGMU",
                 "city":"Lucknow","latitude":26.8467,"longitude":80.9462,"urgency":"CRITICAL",
                 "requiredBy":"%s","contactPhone":"9415000001","notes":"Urgent"}
                """.formatted(group, units, LocalDate.now().plusDays(3));
    }

    @Test
    void fullDonationWorkflow() throws Exception {
        String donorToken = register(donorJson("flow.donor@test.com", "1998-05-10"));
        String requesterToken = register("""
                {"name":"Hospital Desk","email":"flow.req@test.com","password":"Password@123",
                 "role":"REQUESTER","phone":"9415000001","city":"Lucknow"}
                """);

        // guard rails
        mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(donorJson("flow.donor@test.com", "1998-05-10")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(donorJson("kid@test.com", LocalDate.now().minusYears(15).toString())))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"flow.donor@test.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        send(get("/api/admin/stats"), donorToken, null).andExpect(status().isForbidden());

        // requester raises a request; donors cannot be blocked from the public stats
        String created = send(post("/api/requests"), requesterToken, requestJson("B+", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        int requestId = JsonPath.read(created, "$.id");

        // smart matching finds the donor and explains why
        send(get("/api/requests/" + requestId + "/matches"), requesterToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Donor"))
                .andExpect(jsonPath("$[0].exactMatch").value(true))
                .andExpect(jsonPath("$[0].reasons").isNotEmpty());
        // donors cannot see the match list of someone else's request
        send(get("/api/requests/" + requestId + "/matches"), donorToken, null).andExpect(status().isForbidden());

        // donor sees it in their feed (encoded '+' in the query string)
        send(get("/api/requests?forMe=true&blood=B%2B"), donorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId))
                .andExpect(jsonPath("$[0].compatibleWithMe").value(true));

        // donor responds once, a second attempt is rejected
        String resp = send(post("/api/requests/" + requestId + "/respond"), donorToken, "{\"message\":\"On my way\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        int responseId = JsonPath.read(resp, "$.id");
        send(post("/api/requests/" + requestId + "/respond"), donorToken, "{}").andExpect(status().isConflict());

        // the donor cannot accept their own offer, the requester can
        send(put("/api/responses/" + responseId + "/status"), donorToken, "{\"status\":\"ACCEPTED\"}")
                .andExpect(status().isForbidden());
        send(put("/api/responses/" + responseId + "/status"), requesterToken, "{\"status\":\"ACCEPTED\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        send(put("/api/responses/" + responseId + "/status"), requesterToken, "{\"status\":\"COMPLETED\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

        // one unit was needed, so the request is now fulfilled
        send(get("/api/requests/" + requestId), requesterToken, null)
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.unitsFulfilled").value(1));

        // donor is now in the cool-down window
        send(get("/api/users/me"), donorToken, null)
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.completedDonations").value(1));
        String second = send(post("/api/requests"), requesterToken, requestJson("B+", 1))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int secondId = JsonPath.read(second, "$.id");
        send(post("/api/requests/" + secondId + "/respond"), donorToken, "{}").andExpect(status().isBadRequest());

        // donor received notifications and public stats are open
        send(get("/api/notifications"), donorToken, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isNotEmpty());
        mvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donationsCompleted").value(1));
    }

    @Test
    void incompatibleDonorIsRejected() throws Exception {
        String donorToken = register(donorJson("incompat.donor@test.com", "1995-01-15")); // B+
        String requesterToken = register("""
                {"name":"Family","email":"incompat.req@test.com","password":"Password@123",
                 "role":"REQUESTER","phone":"9415000002","city":"Lucknow"}
                """);
        String created = send(post("/api/requests"), requesterToken, requestJson("A-", 1))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");
        send(post("/api/requests/" + id + "/respond"), donorToken, "{}").andExpect(status().isBadRequest());
        send(get("/api/requests/" + id + "/matches"), requesterToken, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }
}
