package museon_online.astor_butler.domain.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventBookingDraft implements Serializable {

    private String eventType;
    private String eventDate;
    private String guestCount;
    private String eventFormat;
    private String budget;
    private String menuPreferences;
    private String technicalRequirements;
    private String contactDetails;
    private String clientComment;
}
