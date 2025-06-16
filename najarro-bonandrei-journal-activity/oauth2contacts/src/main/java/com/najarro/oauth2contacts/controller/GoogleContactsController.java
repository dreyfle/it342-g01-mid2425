package com.najarro.oauth2contacts.controller;

import com.najarro.oauth2contacts.model.Contact;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate; // Import RestTemplate

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class GoogleContactsController {

    private final RestTemplate restTemplate;

    /**
     * Constructor for GoogleContactsController.
     * Initializes RestTemplate for making HTTP requests.
     */
    public GoogleContactsController() {
        this.restTemplate = new RestTemplate(); // RestTemplate is thread-safe and can be reused.
    }

    /**
     * Handles the root URL and the custom /login endpoint.
     * If the user is not authenticated, it provides a link to initiate Google OAuth login.
     *
     * @return The name of the Thymeleaf template for the home/login page.
     */
    @GetMapping
    public String login() {
        // Get the current authentication object from the SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated (not anonymousUser and not null)
        // Spring Security often uses "anonymousUser" for unauthenticated requests
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            // User is authenticated, redirect to the home page
            return "redirect:/home";
        } else {
            // User is not authenticated, show the login page
            return "index"; // This will map to src/main/resources/templates/index.html
        }
    }

    /**
     * Handles the home page for authenticated users.
     * Displays user information and a button to view contacts.
     *
     * @param oauth2User The authenticated OAuth2User representing the Google user.
     * @param model The Model object to pass data to the Thymeleaf template.
     * @return The name of the Thymeleaf template for the home page.
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        System.out.println("User: " + oauth2User);
        model.addAttribute("userName", oauth2User.getAttribute("name"));
        model.addAttribute("userEmail", oauth2User.getAttribute("email"));
        return "home"; // This will map to src/main/resources/templates/home.html
    }

    /**
     * Fetches and displays the user's Google Contacts.
     * This endpoint is accessible only after successful Google OAuth authentication.
     *
     * @param authorizedClient The OAuth2AuthorizedClient containing the access token for Google.
     * @param oauth2User The authenticated OAuth2User representing the Google user.
     * @param model The Model object to pass data to the Thymeleaf template.
     * @return The name of the Thymeleaf template to display contacts.
     */
    @GetMapping("/contacts")
    public String getContacts(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OAuth2User oauth2User, // Keep for display on contacts page
            Model model) {

        // Add user information to the model (useful for consistent headers/footers)
        model.addAttribute("userName", oauth2User.getAttribute("name"));
        model.addAttribute("userEmail", oauth2User.getAttribute("email"));

        // Prepare headers for the API call, including the Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
        HttpEntity<String> entity = new HttpEntity<>(headers); // Create an entity with headers

        // Make a request to the Google People API using RestTemplate
        // The response is expected as a Map
        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses,phoneNumbers",
                HttpMethod.GET, // HTTP GET method
                entity,       // Request entity with headers
                Map.class     // Expected response body type
        );

        Map<String, List<Map<String, Object>>> response = responseEntity.getBody();

        List<Contact> contacts = Collections.emptyList();
        if (response != null && response.containsKey("connections")) {
            // Extract the list of connections from the response map
            List<Map<String, Object>> connections = (List<Map<String, Object>>) response.get("connections");

            // Convert raw map data into a list of Contact objects
            contacts = connections.stream()
                    .map(this::mapToContact) // Map each connection map to a Contact object
                    .collect(Collectors.toList());
        }

        model.addAttribute("contacts", contacts);
        return "contacts"; // This will map to src/main/resources/templates/contacts.html
    }

    /**
     * Helper method to map a raw Google People API connection map to a Contact object.
     * This handles the nested structure of the API response for names, emails, and phone numbers.
     *
     * @param connection The raw map representing a single contact connection from the API.
     * @return A populated Contact object.
     */
    private Contact mapToContact(Map<String, Object> connection) {
        Contact contact = new Contact();

        // Extract and set names
        List<Map<String, String>> names = (List<Map<String, String>>) connection.get("names");
        if (names != null && !names.isEmpty()) {
            List<Contact.Name> contactNames = names.stream()
                    .map(nameMap -> {
                        Contact.Name n = new Contact.Name();
                        n.setDisplayName(nameMap.get("displayName"));
                        return n;
                    })
                    .collect(Collectors.toList());
            contact.setNames(contactNames);
        }

        // Extract and set email addresses
        List<Map<String, String>> emails = (List<Map<String, String>>) connection.get("emailAddresses");
        if (emails != null && !emails.isEmpty()) {
            List<Contact.EmailAddress> contactEmails = emails.stream()
                    .map(emailMap -> {
                        Contact.EmailAddress e = new Contact.EmailAddress();
                        e.setValue(emailMap.get("value"));
                        return e;
                    })
                    .collect(Collectors.toList());
            contact.setEmailAddresses(contactEmails);
        }

        // Extract and set phone numbers
        List<Map<String, String>> phoneNumbers = (List<Map<String, String>>) connection.get("phoneNumbers");
        if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
            List<Contact.PhoneNumber> contactPhones = phoneNumbers.stream()
                    .map(phoneMap -> {
                        Contact.PhoneNumber p = new Contact.PhoneNumber();
                        p.setValue(phoneMap.get("value"));
                        return p;
                    })
                    .collect(Collectors.toList());
            contact.setPhoneNumbers(contactPhones);
        }

        return contact;
    }
}

