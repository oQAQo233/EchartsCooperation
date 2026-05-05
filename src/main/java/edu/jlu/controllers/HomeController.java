package edu.jlu.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/sleep/home")
    public String home() {
        return "pages/home";
    }

    @GetMapping("/sleep/test-noise")
    public String testNoise() {
        return "pages/test-noise";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/sleep/home";
    }
}
