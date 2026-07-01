package service;
/*Centralises audit logging. Should be called from authentication filters,
 *  login controllers, and any sensitive operations.*/

import model.AuditLog;
import repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    //Convenience method for logging a successful event. Calls the private log method with status "SUCCESS".
    public void logSuccess(String username, String action, HttpServletRequest request, String message) {
        log(username, action, request, "SUCCESS", message);
    }

    //Convenience method for logging a failed event.
    public void logFailure(String username, String action, HttpServletRequest request, String message) {
        log(username, action, request, "FAILURE", message);
    }
    
    private void log(String username, String action, HttpServletRequest request, String status, String message) {
        AuditLog log = new AuditLog(); //Creates a new AuditLog instance.
        log.setUserEmail(username);//Sets the user email
        log.setAction(action);//Sets the action
        String ip = request != null ? request.getRemoteAddr() : null;
        log.setIpAddress(ip);
        //log.setIpAddress(request.getRemoteAddr());//Retrieves the client IP from the request
        //Sets status and message.
        log.setStatus(status);
        log.setMessage(message);
        auditLogRepository.save(log);//Saves the log to the database.
    }
}
