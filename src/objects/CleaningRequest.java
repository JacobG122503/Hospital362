package objects;

import types.CleaningType;

public class CleaningRequest {
    private String roomNumber;
    private CleaningType cleaningType;
    private String requestedByName;
    private String timestamp;
    private String status;
    private String details;

    public CleaningRequest(String roomNumber, CleaningType cleaningType,
                           String requestedByName, String timestamp, String status,
                           String details) {
        this.roomNumber = roomNumber;
        this.cleaningType = cleaningType;
        this.requestedByName = requestedByName;
        this.timestamp = timestamp;
        this.status = status;
        this.details = details == null ? "" : details;
    }

    public String getRoomNumber() { return roomNumber; }
    public CleaningType getCleaningType() { return cleaningType; }
    public String getRequestedByName() { return requestedByName; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }

    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        String base = "Room: " + roomNumber
                + ", Type: " + cleaningType
                + ", Requested By: " + requestedByName
                + ", Submitted: " + timestamp
                + ", Status: " + status;
        return details.isEmpty() ? base : base + " | Details: " + details;
    }
}
