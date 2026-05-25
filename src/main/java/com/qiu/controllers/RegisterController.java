package com.qiu.controllers;

import com.qiu.dto.RegisterDTO;
import com.qiu.entities.User;
import com.qiu.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

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



    // Przetwarzanie danych wysłanych z formularza
    @PostMapping("/register")
    public String processRegistration(@RequestParam("username") String username,
                                      @RequestParam("password") String password,
                                      Model model) {

        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            userService.registerNewUser(user);
            return "redirect:/login?success=true";
        } catch (Exception e) {
            model.addAttribute("error", "Użytkownik o tej nazwie już istnieje!");

            // Ważne: musisz ponownie dodać obiekt 'user' do modelu,
            // aby Thymeleaf nie wyrzucił błędu IllegalStateException
            model.addAttribute("error", "Użytkownik o tej nazwie już istnieje!");
            return "register";
        }
    }
}