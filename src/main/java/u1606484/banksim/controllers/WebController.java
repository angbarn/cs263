package u1606484.banksim.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import u1606484.banksim.SecurityService;
import u1606484.banksim.databases.UserAuthenticationPackage;
import u1606484.banksim.weblogic.LoginSystem;

@RestController
public class WebController {

    private static final String FAIL_BOX_STYLING
            = "box-shadow: 5px 5px 20px #f00;";
    private static final String LOGIN_ONE = "index";
    private static final String LOGIN_TWO = "login2";
    private static final String SUCCESS = "success";

    private final LoginSystem loginSystem = new LoginSystem();

    private OptionalInt parseUserId(String userId) {
        try {
            int v = Integer.parseInt(userId);
            return OptionalInt.of(v);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    @RequestMapping(
            value = {"", "index"},
            method = {RequestMethod.POST, RequestMethod.GET}
    )
    @ResponseBody
    public ModelAndView index(
            @RequestParam(name = "loginError", required = false, defaultValue
                    = "no-error") String loginError,
            @RequestParam(name = "password", required = false, defaultValue =
                    "") String password,
            @RequestParam(name = "username", required = false, defaultValue =
                    "") String username,
            @RequestParam(name = "otac", required = false, defaultValue = "")
                    String otac,
            @CookieValue(value = "session_token", defaultValue = "") String
                    sessionKey,
            HttpServletResponse response) {

        // Style input boxes if there is an error
        Map<String, String> model = new HashMap<>();

        Optional<UserAuthenticationPackage> key = loginSystem
                .getUserFromSession(sessionKey);
        @SuppressWarnings("OptionalIsPresent") int keyLevel =
                key.isPresent() ? key.get().getOtacLevel() : -1;

        int userId;
        boolean success;
        String view;

        System.out.println(sessionKey + " - " + keyLevel);

        if (keyLevel == -1) {
            userId = parseUserId(username).orElse(-1);
            success = loginSystem.attemptBasicLogin(userId, password, response);

            // Send OTAC if we succeed
            if (success) {
                loginSystem.sendOtac(userId);
            }

            // Set view as appropriate
            view = success ? LOGIN_TWO : LOGIN_ONE;
        } else if (keyLevel == 0) {
            userId = key.get().getUserId();
            success = loginSystem.attemptOtacLogin(userId, otac, response);

            // Re-send OTAC if we fail
            if (!success) {
                loginSystem.sendOtac(userId);
            }

            view = success ? SUCCESS : LOGIN_TWO;
        } else if (keyLevel == 1) {
            userId = key.get().getUserId();
            success = true;

            view = SUCCESS;
        } else {
            throw new IllegalStateException("Invalid key level");
        }

        if ((!success) && (!otac.equals("") || !username.equals("") || !password
                .equals(""))) {
            model.put("box_styling", FAIL_BOX_STYLING);
        }

        model.put("username", Integer.toString(userId));

        return new ModelAndView(view, model);
    }

    @RequestMapping(
            value = "logout",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    @ResponseBody
    public String logout(
            @CookieValue(value = "session_token", defaultValue = "") String
                    sessionToken, HttpServletResponse response) {
        Optional<UserAuthenticationPackage> user = loginSystem
                .getUserFromSession(sessionToken);

        // Can only log somebody out if we're sure they're the person logged in
        if (user.isPresent() && user.get().getOtacLevel() == 1) {
            loginSystem.terminateSessions(user.get().getUserId());
        }

        try {
            response.sendRedirect("index");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "You are now logged out";
    }

    @RequestMapping(
            value = "register",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    @ResponseBody
    public ModelAndView register() {
        return new ModelAndView("register");
    }

    @RequestMapping(
            value = "register_submit",
            method = {RequestMethod.POST}
    )
    @ResponseBody
    public ModelAndView registerSubmit(String firstName, String lastName,
            String phoneNumber, String addressLine1, String addressLine2,
            String postcode, String county, String password1, String password2,
            @CookieValue(value = "session_token", defaultValue = "") String
                    sessionKey,
            HttpServletResponse response) {

        Supplier<ModelAndView> redirect = () -> {
            try {
                response.sendRedirect("/");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return new ModelAndView("index");
        };

        // Reject if already logged in
        if (!sessionKey.equals("") && loginSystem.getUserFromSession(sessionKey)
                .isPresent()) {
            return redirect.get();
        }

        // Reject if passwords do not match
        if (!password1.equals(password2)) {
            return redirect.get();
        }

        // Reject if passwords are too short
        if (password1.length() < SecurityService.MINIMUM_PASSWORD_LENGTH) {
            return redirect.get();
        }

        // Otherwise, create user
        int newUserId = loginSystem
                .createUser(firstName, lastName, phoneNumber, addressLine1,
                        addressLine2, postcode, county, password1);
        loginSystem.sendAccountId(phoneNumber, newUserId);

        return redirect.get();
    }

    /*

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

    */
}