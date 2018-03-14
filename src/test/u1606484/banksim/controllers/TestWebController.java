package u1606484.banksim.controllers;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet
        .AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TestWebController {

    @Autowired
    private MockMvc mvc;

    @Test
    public void getHello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(equalTo("Greetings from Spring Boot!")));
    }

    @Test
    public void attemptLoginStage1() throws Exception {
        // String -> String -> String -> ()
        Function<String, Function<String, Consumer<String>>> attempt = m -> u
                -> p -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post("/attempt_login", u, p)
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().string(equalTo(m)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        String failRedirect = "redirect:/index?loginSuccess=failure";
        String successRedirect = "redirect:/attempt_login_2";

        Function<String, Consumer<String>> fail = attempt.apply(failRedirect);
        Function<String, Consumer<String>> succ = attempt
                .apply(successRedirect);

        // Correct username, incorrect password
        fail.apply("1").accept("");
        // Incorrect username, correct password
        fail.apply("1000").accept("jess continues to be a disappointment");
        // Correct both
        succ.apply("1").accept("jess continues to be a disappointment");
    }
}