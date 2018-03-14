package u1606484.banksim.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import u1606484.banksim.weblogic.LoginSystem;

@RestController
public class WebController {

    private final LoginSystem loginSystem = new LoginSystem();

    @RequestMapping(
            value = "/index",
            method = RequestMethod.GET
    )
    public Model index(String loginSuccess, Model model) {
        model.addAttribute("login_success",
                loginSuccess.equals("failure")
                        ? "login_input_failure"
                        : "login_input");
        return model;
    }

    @RequestMapping(
            value = "/attempt_login",
            method = RequestMethod.POST
    )
    public String attemptLoginStage1(String username, String password) {
        String failRedirect = "redirect:/index?loginSuccess=failure";
        String successRedirect = "redirect:/attempt_login_2";

        int userId;
        try {
            userId = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            return failRedirect;
        }

        boolean loginSuccess = loginSystem.attemptBasicLogin(userId, password);
        if (loginSuccess) {
            return successRedirect;
        } else {
            return failRedirect;
        }
    }

}