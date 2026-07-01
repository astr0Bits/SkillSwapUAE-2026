package dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import validator.NoHtml;

public class UpdateAccountRequest {
    @NotBlank
    @Email
    private String email;

    @NoHtml
    private String name;

    @Email
    private String newEmail;

    @NoHtml
    private String location;

    private String newPassword;

    // current password for verification
    private String currentPassword;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}