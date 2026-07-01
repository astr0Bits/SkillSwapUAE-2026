// enums/SessionStatus.java
package enums;

public enum SessionStatus {
	OPEN, 
    PENDING,      // waiting for mentor approval
    SCHEDULED,    // accepted, ready to happen
    COMPLETED,
    CANCELLED
}