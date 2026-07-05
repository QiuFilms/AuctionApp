package com.qiu.controllers;

import com.qiu.dto.AuthRequest;
import com.qiu.entities.User;
import com.qiu.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        return "register";
    }


    @PostMapping("/register")
    public String processRegistration(AuthRequest registerRequest, Model model) {
        try {
            userService.registerNewUser(registerRequest);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Użytkownik o tej nazwie już istnieje!");

            return "register";
        }
    }
}