package u1606484.banksim.controllers;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import u1606484.banksim.weblogic.LoginSystem;

@RestController
public class WebController {

    private static final String FAIL_BOX_STYLING
            = "box-shadow: 5px 5px 20px #f00;";

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
            Model model) {

        int userId;
        try {
            userId = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            model.addAttribute("box_styling", FAIL_BOX_STYLING);
            return new ModelAndView("index", model.asMap());
        }

        boolean loginSuccess = loginSystem.attemptBasicLogin(userId, password);
        if (loginSuccess) {
            loginSystem.sendOtac(userId);
            model.addAttribute("username", username);
            model.addAttribute("password", password);
            return new ModelAndView("login2", model.asMap());
        } else {
            model.addAttribute("box_styling", FAIL_BOX_STYLING);
            return new ModelAndView("index", model.asMap());
        }
    }

    @RequestMapping(
            value = "attempt_login_2",
            method = RequestMethod.POST
    )
    @ResponseBody
    public ModelAndView attemptLoginStage2(String username, String password,
            String otac, Model model) {

        System.out.println("hello attempt 2");

        int userId;
        try {
            userId = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            model.addAttribute("box_styling", FAIL_BOX_STYLING);
            return new ModelAndView("index", model.asMap());
        }

        boolean loginSuccess = loginSystem
                .attemptOtacLogin(userId, password, otac);
        if (loginSuccess) {
            return new ModelAndView("success", model.asMap());
        } else {
            loginSystem.sendOtac(userId);
            model.addAttribute("box_styling", FAIL_BOX_STYLING);
            return new ModelAndView("login2", model.asMap());
        }
    }
}