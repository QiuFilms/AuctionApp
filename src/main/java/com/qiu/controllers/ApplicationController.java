package com.qiu.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

@Controller
@RequestMapping("")
public class ApplicationController implements Serializable {
    @GetMapping("/")
    public String showIndexPage() {
        return "auctions";
    }

    @GetMapping("/marketplace")
    public String showMarketplacePage() {
        return "marketplace";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // Twoja dodatkowa logika...
        return "redirect:/login";
    }


}
