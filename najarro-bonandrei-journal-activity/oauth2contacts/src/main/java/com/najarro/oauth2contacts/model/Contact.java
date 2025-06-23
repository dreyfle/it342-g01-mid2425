package com.najarro.oauth2contacts.model;

import java.util.List;
import java.util.Optional;

/**
 * Represents a simplified Google Contact with names (first and last), and email addresses.
 * This class is designed to map the relevant fields from the Google People API response.
 */
public class Contact {

  private String resourceName;
  private String etag;
  private String firstName; // New field for first name
  private String lastName;  // New field for last name
  private List<EmailAddress> emailAddresses;
  private List<PhoneNumber> phoneNumbers;

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  // Getters and Setters for firstName and lastName
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  // Existing getters and setters
  public List<EmailAddress> getEmailAddresses() {
    return emailAddresses;
  }

  public void setEmailAddresses(List<EmailAddress> emailAddresses) {
    this.emailAddresses = emailAddresses;
  }

  public List<PhoneNumber> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  /**
   * Helper method to get the full display name (First Last).
   * Combines first and last name, handling cases where one or both might be null.
   */
  public String getFullName() {
    StringBuilder fullName = new StringBuilder();
    if (firstName != null && !firstName.isEmpty()) {
      fullName.append(firstName);
    }
    if (lastName != null && !lastName.isEmpty()) {
      if (fullName.length() > 0) {
        fullName.append(" ");
      }
      fullName.append(lastName);
    }
    return fullName.length() > 0 ? fullName.toString() : null;
  }

  /**
   * Helper method to get the first email address of the contact.
   */
  public String getFirstEmailAddress() {
    return Optional.ofNullable(emailAddresses)
            .flatMap(list -> list.stream().filter(email -> email.getValue() != null).findFirst())
            .map(EmailAddress::getValue)
            .orElse(null);
  }

  /**
   * Helper method to get the first phone number of the contact.
   */
  public String getFirstPhoneNumber() {
    return Optional.ofNullable(phoneNumbers)
            .flatMap(list -> list.stream().filter(phone -> phone.getValue() != null).findFirst())
            .map(PhoneNumber::getValue)
            .orElse(null);
  }

  /**
   * Inner class to represent a contact's name details from Google People API.
   * Corresponds to the 'names' field, specifically 'givenName' and 'familyName'.
   */
  public static class Name {
    private String givenName;    // Corresponds to first name
    private String familyName;   // Corresponds to last name

    public String getGivenName() {
      return givenName;
    }

    public void setGivenName(String givenName) {
      this.givenName = givenName;
    }

    public String getFamilyName() {
      return familyName;
    }

    public void setFamilyName(String familyName) {
      this.familyName = familyName;
    }
  }

  /**
   * Inner class to represent a contact's email address.
   * Corresponds to the 'emailAddresses' field in the People API response.
   */
  public static class EmailAddress {
    private String value;
    private String type;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

  /**
   * Inner class to represent a contact's phone number.
   * Corresponds to the 'phoneNumbers' field in the People API response.
   */
  public static class PhoneNumber {
    private String value;
    private String type;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }
}
