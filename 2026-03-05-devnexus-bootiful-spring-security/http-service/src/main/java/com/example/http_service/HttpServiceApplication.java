package com.example.http_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@SpringBootApplication
public class HttpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpServiceApplication.class, args);
    }

}

@Controller
@ResponseBody
class RestController {

    @GetMapping("/rest")
    Map<String, String> rest(Principal principal) {
        return Map.of("name", principal.getName());
    }
}