package com.najarro.oauth2contacts.model;

import java.util.List;
import java.util.Optional;

public class Contact {
  private List<Name> names;
    private List<EmailAddress> emailAddresses;
    private List<PhoneNumber> phoneNumbers;

    // Getters and Setters

    public List<Name> getNames() {
        return names;
    }

    public void setNames(List<Name> names) {
        this.names = names;
    }

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
     * Helper method to get the display name of the contact.
     * Prefers the display name if available, otherwise returns null.
     */
    public String getDisplayName() {
        return Optional.ofNullable(names)
                .flatMap(list -> list.stream().filter(name -> name.getDisplayName() != null).findFirst())
                .map(Name::getDisplayName)
                .orElse(null);
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
     * Inner class to represent a contact's name.
     * Corresponds to the 'names' field in the People API response.
     */
    public static class Name {
        private String displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * Inner class to represent a contact's email address.
     * Corresponds to the 'emailAddresses' field in the People API response.
     */
    public static class EmailAddress {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Inner class to represent a contact's phone number.
     * Corresponds to the 'phoneNumbers' field in the People API response.
     */
    public static class PhoneNumber {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
