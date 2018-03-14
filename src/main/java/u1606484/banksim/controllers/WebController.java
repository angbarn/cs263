package u1606484.banksim.controllers;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import u1606484.banksim.weblogic.LoginSystem;

@RestController
public class WebController {

    private final LoginSystem loginSystem = new LoginSystem();

    @RequestMapping(
            value = "/",
            method = {RequestMethod.POST, RequestMethod.GET}
    )
    @ResponseBody
    public ModelAndView index(@RequestParam
            (name = "loginError", required = false, defaultValue = "noerror")
            String loginError) {
        Map<String, String> model = new HashMap<>();
        model.put("login_success",
                loginError.equals("error")
                        ? "login_input_failure"
                        : "login_input");

        return new ModelAndView("/index", model);
    }

    @RequestMapping(
            value = "/attempt_login",
            method = RequestMethod.POST
    )
    @ResponseBody
    public ModelAndView attemptLoginStage1(String username, String password,
            RedirectAttributes redirectAttributes) {
        String failBoxStyling = "box-shadow: 5px 5px 20px #f00;";

        System.out.println("Username: " + username + " password: " + password);

        Map<String, String> model = new HashMap<>();

        int userId;
        try {
            userId = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            model.put("box_styling", failBoxStyling);
            return new ModelAndView("index", model);
        }

        boolean loginSuccess = loginSystem.attemptBasicLogin(userId, password);
        if (loginSuccess) {
            loginSystem.sendOtac(userId);
            model.put("username", username);
            model.put("password", password);
            return new ModelAndView("login2", model);
        } else {
            model.put("box_styling", failBoxStyling);
            return new ModelAndView("index", model);
        }
    }

}