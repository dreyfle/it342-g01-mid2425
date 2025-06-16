package com.najarro.oauth2contacts.controller;

import com.najarro.oauth2contacts.service.GoogleContactsService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Controller
public class GoogleContactsController {
  private final GoogleContactsService service;

  public GoogleContactsController(GoogleContactsService service) {
    this.service = service;
  }
  // index page
  @GetMapping("/home")
  public String home(Model model, OAuth2AuthenticationToken authentication) {
    if (authentication != null) {
      OAuth2AuthorizedClient client = getAuthorizedClient(authentication);
      System.out.println("Token: " + client.getAccessToken().getTokenValue());
      Map<String, Object> userInfo = service.getProfile(client.getAccessToken().getTokenValue());
      model.addAttribute("userInfo", userInfo);
    }
    return "home";
  }

  private OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken authentication) {
    OAuth2AuthorizedClientService clientService = new InMemoryOAuth2AuthorizedClientService(
            new InMemoryClientRegistrationRepository()
    );
    return clientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName());
  }

//
//  @GetMapping("/contacts")
//  public String getContacts() {
//
//  }
}
