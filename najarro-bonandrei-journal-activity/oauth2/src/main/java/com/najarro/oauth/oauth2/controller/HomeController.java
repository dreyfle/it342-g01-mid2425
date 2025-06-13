package com.najarro.oauth.oauth2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping
    public String home() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Hello World</title>
            </head>
            <body>
                <h1>Hello World</h1>
            </body>
            </html>
            """;
    }

    @GetMapping("/secured")
    public String secured() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Hello World</title>
            </head>
            <body>
                <h1>Hello to the Secure World</h1>
            </body>
            </html>
            """;
    }

}
