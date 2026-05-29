package com.qiu.controllers;

import com.qiu.dto.RegisterRequest;
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
    public String processRegistration(RegisterRequest registerRequest, Model model) {
        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(registerRequest.getPassword());
            userService.registerNewUser(user);
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Użytkownik o tej nazwie już istnieje!");

            model.addAttribute("error", "Użytkownik o tej nazwie już istnieje!");
            return "register";
        }
    }
}