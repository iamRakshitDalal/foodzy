package com.foodzy.server.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/health")
    public String healthCheck() {
        return "Foodzy Backend is Running";
    }

}
