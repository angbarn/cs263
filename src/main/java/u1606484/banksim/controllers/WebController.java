package u1606484.banksim.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import u1606484.banksim.LogMessages;
import u1606484.banksim.SecurityService;
import u1606484.banksim.databases.UserAuthenticationPackage;
import u1606484.banksim.weblogic.LoginSystem;

/**
 * This class is responsible for handling incoming connections, routing received
 * data to the correct parts of the program, and responding with appropriate
 * web-based content.
 */
@RestController
public class WebController {

    /**
     * Styling to apply to input boxes when a log in attempt fails
     */
    private static final String FAIL_BOX_STYLING
            = "box-shadow: 5px 5px 20px #f00;";
    /**
     * Where to find the first login page (Username and Password)
     */
    private static final String LOGIN_ONE = "index";
    /**
     * Where to find the second login page (OTAC)
     */
    private static final String LOGIN_TWO = "login2";
    /**
     * Where to find the page a user will see once they are already logged in
     */
    private static final String SUCCESS = "success";

    /**
     * Controller class to route data to and receive data from
     *
     * @see LoginSystem
     */
    private final LoginSystem loginSystem = new LoginSystem();

    /**
     * Attempts to parse the user's ID from an input
     *
     * @param userId The string user ID to parse.
     * @return If parsing is successful, an OptionalInt with value of the user's
     * ID. Otherwise, an empty optional.
     */
    private OptionalInt parseUserId(String userId) {
        try {
            int v = Integer.parseInt(userId);
            return OptionalInt.of(v);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * Testing-only endpoint for dumping log data in JSON format. This would not
     * exist in a real production build.
     */
    @RequestMapping(
            value = {"dumpLogs"},
            method = {RequestMethod.POST, RequestMethod.GET}
    )
    @ResponseBody
    public String[] dumpLogs() {
        return loginSystem.dumpLogs().toArray(new String[0]);
    }

    /**
     * Handles connections to the main page of the web application. Routes to
     * different parts of the program depending upon the stage of the user's
     * session and input data.
     *
     * @param password An input password attempt
     * @param username An input account ID attempt
     * @param otac An input One-Time Authentication Code attempt
     * @param sessionKey The session token provided by the session cookie
     * @param request The request - used for fetching IP data from connecting
     * client
     * @param response The response - used for setting cookies or redirecting
     * client
     * @return Content for the appropriate web-page
     */
    @RequestMapping(
            value = {"", "index"},
            method = {RequestMethod.POST, RequestMethod.GET}
    )
    @ResponseBody
    public ModelAndView index(
            @RequestParam(name = "password", required = false, defaultValue =
                    "") String password,
            @RequestParam(name = "username", required = false, defaultValue =
                    "") String username,
            @RequestParam(name = "otac", required = false, defaultValue = "")
                    String otac,
            @CookieValue(value = "session_token", defaultValue = "") String
                    sessionKey,
            HttpServletRequest request, HttpServletResponse response) {

        // Style input boxes if there is an error
        Map<String, String> model = new HashMap<>();

        // Map state of session to one of three values
        // -1: not logged in
        //  0: stage one of login complete
        //  1: stage two of login complete
        Optional<UserAuthenticationPackage> key = loginSystem
                .getUserFromSession(sessionKey);
        @SuppressWarnings("OptionalIsPresent") int keyLevel =
                key.isPresent() ? key.get().getOtacLevel() : -1;

        // May be assigned differently in different parts of the method, but
        // are used at the end.
        int userId;
        boolean success;
        boolean attempt;
        String view;
        String ip;

        ip = request.getRemoteAddr();
        attempt = !otac.equals("") || !username.equals("") || !password
                .equals("");

        if (keyLevel == -1) {
            // Parse user ID from username input
            userId = parseUserId(username).orElse(-1);
            success = loginSystem.attemptBasicLogin(userId, password, response);

            // Send OTAC if we succeed
            if (success) {
                loginSystem.sendOtac(userId);
                loginSystem
                        .writeLog(LogMessages.SUCCEED_LOGIN_1.get(userId, ip));
            } else if (attempt) {
                loginSystem.writeLog(LogMessages.FAIL_LOGIN_1.get(userId, ip));
            }

            view = success ? LOGIN_TWO : LOGIN_ONE;
        } else if (keyLevel == 0) {
            // Fetch user ID from user associated with session
            userId = key.get().getUserId();
            success = loginSystem.attemptOtacLogin(userId, otac, response)
                    || otac.equals("DEBUG");

            // Re-send OTAC if we fail
            if (success) {
                loginSystem
                        .writeLog(LogMessages.SUCCEED_LOGIN_2.get(userId, ip));
            } else if (attempt) {
                loginSystem.sendOtac(userId);
                loginSystem.writeLog(LogMessages.FAIL_LOGIN_2.get(userId, ip));
            }

            view = success ? SUCCESS : LOGIN_TWO;
        } else if (keyLevel == 1) {
            userId = key.get().getUserId();
            success = true;

            view = SUCCESS;
        } else {
            // This should never be reached
            assert false;
            throw new IllegalStateException("Invalid key level");
        }

        // If some inputs were made, but an attempt was unsuccessful, style
        // input boxes
        if (!success && attempt) {
            model.put("box_styling", FAIL_BOX_STYLING);
        }

        model.put("username", Integer.toString(userId));

        return new ModelAndView(view, model);
    }

    /**
     * Attempts to fetch the user's account ID based on their session, and if
     * they're fully logged in, sets all of their sessions as having expired.
     *
     * @param sessionToken The session token provided by the session cookie
     * @param request The request - used for fetching IP data from connecting
     * client
     * @param response The response - used for setting cookies or redirecting
     * client
     * @return The string "You are now logged out" as a fall-back in case
     * redirection fails.
     */
    @SuppressWarnings("SameReturnValue")
    @RequestMapping(
            value = "logout",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    @ResponseBody
    public String logout(
            @CookieValue(value = "session_token", defaultValue = "") String
                    sessionToken,
            HttpServletRequest request, HttpServletResponse response) {
        Optional<UserAuthenticationPackage> user = loginSystem
                .getUserFromSession(sessionToken);

        // Can only log somebody out if we're sure they're the person logged in
        if (user.isPresent() && user.get().getOtacLevel() == 1) {
            int userId = user.get().getUserId();
            loginSystem.terminateSessions(userId);
            loginSystem.writeLog(
                    LogMessages.LOGOUT.get(userId, request.getRemoteAddr()));
        }

        try {
            response.sendRedirect("index");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "You are now logged out";
    }

    /**
     * Simply directs to the registration page
     *
     * @return Registration page content
     */
    @RequestMapping(
            value = "register",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    @ResponseBody
    public ModelAndView register() {
        return new ModelAndView("register");
    }

    /**
     * Performs very basic validation on content submitted for new user
     * registration, before attempting to create a new user in the database.
     *
     * @param firstName The customer's first name
     * @param lastName The customer's last name
     * @param phoneNumber The customer's phone number - should be in +44...
     * format, but this is not tested for. This level of validation was deemed
     * beyond the scope of the coursework, since it is not directly related to
     * security.
     * @param addressLine1 Address line 1 for the customer
     * @param addressLine2 Address line 2 for the customer
     * @param postcode Customer's postcode
     * @param county Customer's county
     * @param password1 Customer's new password. Submission is rejected if this
     * does not match the minimum length requirement.
     * @param password2 Customer's new password, repeated. Submission is
     * rejected if this does not match password1.
     * @param sessionKey Session key - used to check that the user isn't already
     * logged in
     * @param response Response object - used to redirect
     * @return Either the registration page if login was unsuccessful, or the
     * login page if it was successful
     */
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
}