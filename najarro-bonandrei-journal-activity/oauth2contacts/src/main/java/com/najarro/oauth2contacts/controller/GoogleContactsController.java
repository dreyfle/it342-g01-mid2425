package com.najarro.oauth2contacts.controller;

import com.najarro.oauth2contacts.model.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate; // Import RestTemplate
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GoogleContactsController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for GoogleContactsController.
     * Initializes RestTemplate for making HTTP requests.
     */
    public GoogleContactsController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate; // RestTemplate is thread-safe and can be reused.
        this.objectMapper = new ObjectMapper();
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
     * Displays the form for adding a new contact or editing an existing one.
     *
     * @param resourceName Optional. The resourceName of the contact to edit. If null, it's an add operation.
     * @param authorizedClient The OAuth2AuthorizedClient containing the access token.
     * @param model The Model object to pass data to the Thymeleaf template.
     * @param redirectAttributes Used for passing flash attributes after redirect.
     * @return The name of the Thymeleaf template for the contact form.
     */
    @GetMapping("/contact-form")
    public String showContactForm(@RequestParam(required = false) String resourceName,
                                  @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        model.addAttribute("contact", new Contact()); // Default empty contact for 'add' mode
        model.addAttribute("mode", "add"); // Default mode

        // If resourceName is provided, it's an edit operation
        if (resourceName != null && !resourceName.isEmpty()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Fetch the specific contact details
                ResponseEntity<Map> responseEntity = restTemplate.exchange(
                        "https://people.googleapis.com/v1/" + resourceName + "?personFields=names,emailAddresses,phoneNumbers",
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                Map<String, Object> apiContact = responseEntity.getBody();
                if (apiContact != null) {
                    Contact existingContact = mapApiToContact(apiContact); // Map API response to Contact object
                    System.out.println("Current Etag taken from API: " + existingContact.getEtag());
                    model.addAttribute("contact", existingContact);
                    model.addAttribute("mode", "edit");
                }
            } catch (HttpClientErrorException.NotFound e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Contact not found for editing.");
                return "redirect:/contacts";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Error fetching contact for editing: " + e.getMessage());
                return "redirect:/contacts";
            }
        }
        return "contact_form"; // Renamed from add_contact
    }

    /**
     * Handles the submission of the contact form (add or edit).
     *
     * @param contact The Contact object populated from the form.
     * @param authorizedClient The OAuth2AuthorizedClient containing the access token.
     * @param redirectAttributes Used for passing flash attributes after redirect.
     * @return A redirect URL to the contacts list.
     */
    @PostMapping("/save-contact")
    public String saveContact(@ModelAttribute Contact contact,
                              @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
                              RedirectAttributes redirectAttributes) {
        try {
            // System.out.println("Contact etag passed to save contact: "+ contact.getEtag());
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
            headers.setContentType(MediaType.APPLICATION_JSON); // Important for JSON payloads

            Map<String, Object> person = new HashMap<>();

            if (contact.getEtag() != null && !contact.getEtag().isEmpty()) {
                person.put("etag", contact.getEtag());
            }

            List<Map<String, String>> names = new ArrayList<>();
            if (contact.getFirstName() != null && !contact.getFirstName().isEmpty()) {
                Map<String, String> name = new HashMap<>();
                name.put("givenName", contact.getFirstName());
                if (contact.getLastName() != null && !contact.getLastName().isEmpty()) {
                    name.put("familyName", contact.getLastName());
                }
                names.add(name);
            } else if (contact.getLastName() != null && !contact.getLastName().isEmpty()) {
                // If only last name is provided, still include it
                Map<String, String> name = new HashMap<>();
                name.put("familyName", contact.getLastName());
                names.add(name);
            }
            person.put("names", names);

            List<Map<String, String>> emailAddresses = new ArrayList<>();
            if (contact.getEmailAddresses() != null) {
                for (Contact.EmailAddress email : contact.getEmailAddresses()) {
                    // Only add email to list if its value is not empty
                    if (email.getValue() != null && !email.getValue().isEmpty()) {
                        Map<String, String> emailMap = new HashMap<>();
                        emailMap.put("value", email.getValue());
                        if (email.getType() != null && !email.getType().isEmpty()) {
                             emailMap.put("type", email.getType()); // Include type if provided
                        }
                        emailAddresses.add(emailMap);
                    }
                }
            }
            person.put("emailAddresses", emailAddresses);

            List<Map<String, String>> phoneNumbers = new ArrayList<>();
            if (contact.getPhoneNumbers() != null) {
                for (Contact.PhoneNumber phone : contact.getPhoneNumbers()) {
                    if (phone.getValue() != null && !phone.getValue().isEmpty()) {
                        Map<String, String> phoneMap = new HashMap<>();
                        phoneMap.put("value", phone.getValue());
                        if (phone.getType() != null && !phone.getType().isEmpty()) {
                            phoneMap.put("type", phone.getType()); // Include type if provided
                        }
                        phoneNumbers.add(phoneMap);
                    }
                }
            }
            person.put("phoneNumbers", phoneNumbers);

            // Convert person map to JSON string
            String jsonBody = objectMapper.writeValueAsString(person);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            if (contact.getResourceName() == null || contact.getResourceName().isEmpty()) {
                // ADD new contact (POST request)
                restTemplate.exchange(
                        "https://people.googleapis.com/v1/people:createContact",
                        HttpMethod.POST,
                        requestEntity,
                        String.class // Response is the created Person object, we just need success
                );
                redirectAttributes.addFlashAttribute("successMessage", "Contact added successfully!");
            } else {
                // EDIT existing contact (PATCH request)
                // Build the updatePersonFields mask based on fields that might have changed
                // System.out.println("Person: "+ person);
                // System.out.println("RequestEntity: "+ requestEntity);
                List<String> updateMaskFields = new ArrayList<>();
                if (person.containsKey("names")) updateMaskFields.add("names"); // Check if names were actually provided in the payload
                if (person.containsKey("emailAddresses")) updateMaskFields.add("emailAddresses");
                if (person.containsKey("phoneNumbers")) updateMaskFields.add("phoneNumbers");

                // If no fields are being updated, just redirect without an API call
                if (updateMaskFields.isEmpty()) {
                    redirectAttributes.addFlashAttribute("infoMessage", "No changes detected for contact.");
                    return "redirect:/contacts";
                }

                String updatePersonFields = String.join(",", updateMaskFields);
                // URL encode the updatePersonFields value
//                String encodedUpdatePersonFields = URLEncoder.encode(updatePersonFields, StandardCharsets.UTF_8);
                System.out.println("URL: https://people.googleapis.com/v1/" + contact.getResourceName() + ":updateContact?updatePersonFields=" + updatePersonFields);
                restTemplate.exchange(
                        "https://people.googleapis.com/v1/" + contact.getResourceName() + ":updateContact?updatePersonFields=" + updatePersonFields,
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class // Response is the updated Person object
                );
                redirectAttributes.addFlashAttribute("successMessage", "Contact updated successfully!");
                System.out.println("S U C C E S S");
            }
        } catch (HttpClientErrorException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "API Error: " + e.getResponseBodyAsString());
            System.err.println("API Error: " + e.getResponseBodyAsString()); // Log for debugging

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            System.err.println("Unexpected Error: " + e.getMessage()); // Log for debugging
        }

        return "redirect:/contacts";
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
                    .map(this::mapApiToContact) // Map each connection map to a Contact object
                    .collect(Collectors.toList());
        }

        model.addAttribute("contacts", contacts);
        return "contacts"; // This will map to src/main/resources/templates/contacts.html
    }

    /**
     * Helper method to map a raw Google People API response (for a single Person) to a Contact object.
     * Used for both fetching all contacts and fetching a single contact for editing.
     *
     * @param apiContact The raw map representing a single contact connection from the API.
     * @return A populated Contact object.
     */
    private Contact mapApiToContact(Map<String, Object> apiContact) {
        Contact contact = new Contact();

        // Set resourceName
        contact.setResourceName((String) apiContact.get("resourceName"));
        contact.setEtag((String) apiContact.get("etag"));

        // Extract and set names (givenName and familyName)
        List<Map<String, String>> names = (List<Map<String, String>>) apiContact.get("names");
        if (names != null && !names.isEmpty()) {
            Map<String, String> primaryName = names.stream()
                    .filter(nameMap -> nameMap.containsKey("givenName") || nameMap.containsKey("familyName"))
                    .findFirst()
                    .orElse(names.get(0));

            contact.setFirstName(primaryName.get("givenName"));
            contact.setLastName(primaryName.get("familyName"));
        }

        // Extract and set email addresses
        List<Map<String, String>> emails = (List<Map<String, String>>) apiContact.get("emailAddresses");
        if (emails != null && !emails.isEmpty()) {
            List<Contact.EmailAddress> contactEmails = emails.stream()
                    .map(emailMap -> {
                        Contact.EmailAddress e = new Contact.EmailAddress();
                        e.setValue(emailMap.get("value"));
                        e.setType(emailMap.get("type")); // Also map type
                        return e;
                    })
                    .collect(Collectors.toList());
            contact.setEmailAddresses(contactEmails);
        }

        // Extract and set phone numbers
        List<Map<String, String>> phoneNumbers = (List<Map<String, String>>) apiContact.get("phoneNumbers");
        if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
            List<Contact.PhoneNumber> contactPhones = phoneNumbers.stream()
                    .map(phoneMap -> {
                        Contact.PhoneNumber p = new Contact.PhoneNumber();
                        p.setValue(phoneMap.get("value"));
                        p.setType(phoneMap.get("type")); // Also map type
                        return p;
                    })
                    .collect(Collectors.toList());
            contact.setPhoneNumbers(contactPhones);
        }

        return contact;
    }

    /**
     * Handles the deletion of a contact.
     * This endpoint expects a POST request with the contact's resourceName.
     *
     * @param resourceName The resourceName of the contact to delete.
     * @param authorizedClient The OAuth2AuthorizedClient containing the access token.
     * @param redirectAttributes Used for passing flash attributes after redirect.
     * @return A redirect URL to the contacts list.
     */
    @PostMapping("/delete-contact") // Using POST for deletion to integrate with Spring Security CSRF
    public String deleteContact(@RequestParam String resourceName,
                                @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
                                RedirectAttributes redirectAttributes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
            HttpEntity<String> entity = new HttpEntity<>(headers); // No request body needed for DELETE

            // The Google People API for delete is a simple DELETE request to the resourceName
            restTemplate.exchange(
                    "https://people.googleapis.com/v1/" + resourceName + ":deleteContact",
                    HttpMethod.DELETE,
                    entity,
                    String.class // Response is empty for successful delete
            );
            redirectAttributes.addFlashAttribute("successMessage", "Contact deleted successfully!");
        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Contact not found for deletion.");
            System.err.println("API Error (Delete - Not Found): " + e.getResponseBodyAsString());
        } catch (HttpClientErrorException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "API Error deleting contact: " + e.getResponseBodyAsString());
            System.err.println("API Error (Delete): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during deletion: " + e.getMessage());
            System.err.println("Unexpected Error (Delete): " + e.getMessage());
        }
        return "redirect:/contacts";
    }
}

