package u1606484.banksim.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

import java.util.Optional;
import javax.servlet.http.Cookie;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet
        .AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import u1606484.banksim.DummyTwoFactor;
import u1606484.banksim.databases.ApplicationDatabaseManager;
import u1606484.banksim.interfaces.ITwoFactorService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TestWebController {

    @Autowired
    private MockMvc mvc;

    private ResultActions attemptLogin(String u, String p)
            throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/")
                .param("username", u)
                .param("password", p)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private ResultActions attemptLogin2(String u, String s, String o)
            throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/")
                .param("otac", o)
                .cookie(new Cookie("session_token", s))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private Optional<String> fetchOtac(int userId) {
        ApplicationDatabaseManager m = new ApplicationDatabaseManager();
        Optional<byte[]> k = m.fetchLoginKey(userId);

        if (k.isPresent()) {
            ITwoFactorService s = new DummyTwoFactor(8, 30 * 1000, 30);
            return Optional.of(s.generateOtac(k.get()));
        } else {
            return Optional.empty();
        }
    }

    @Test
    public void attemptLoginNoPassword() throws Exception {
        attemptLogin("1", "placeholder=\"Account ID\"")
                .andExpect(content().string(containsString("Account")));
    }

    @Test
    public void attemptLoginNoUsername() throws Exception {
        attemptLogin("", "").andExpect(content()
                .string(containsString("placeholder=\"Account ID\"")));
    }

    @Test
    public void attemptLoginCorrect() throws Exception {
        attemptLogin("1", "12345").andExpect(content()
                .string(containsString("placeholder=\"One-time password\"")));
    }

    @Test
    public void attemptOtac() throws Exception {
        MvcResult r = attemptLogin("1", "12345").andReturn();
        Optional<Cookie> c = Optional
                .ofNullable(r.getResponse().getCookie("session_token"));

        if (!c.isPresent()) {
            throw new IllegalStateException("Session token not set correctly");
        }

        Optional<String> otac = fetchOtac(1);

        if (!otac.isPresent()) {
            throw new IllegalStateException("no otac");
        }

        attemptLogin2("1", c.get().getValue(), otac.get()).andExpect(content()
                .string(containsString(
                        "<title>Logged in to Wondough</title>")));

    }
}