package u1606484.banksim.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

    @RequestMapping(
            value = "/index",
            method = RequestMethod.GET
    )
    public Model index(Model model) {
        return model;
    }

}