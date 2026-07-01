package model;
/*Captures messages from the contact page. 
 * It is not persisted directly (likely forwarded to an email service 
 * or stored separately). This DTO is used by a controller endpoint 
 * (e.g., /api/contact).
*/
//A simple POJO (Plain Old Java Object), not a JPA entity. 
//It’s used as a data transfer object (DTO) for contact form submissions.
public class ContactMessage {
	//Fields corresponding to a contact form.
    private String name;
    private String email;
    private String phone;
    private String message;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
