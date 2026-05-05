package museon_online.astor_butler.domain.booking;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_bookings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_date")
    private String eventDate;

    @Column(name = "guest_count")
    private String guestCount;

    @Column(name = "event_format")
    private String eventFormat;

    @Column(name = "budget")
    private String budget;

    @Column(name = "menu_preferences")
    private String menuPreferences;

    @Column(name = "technical_requirements")
    private String technicalRequirements;

    @Column(name = "contact_details")
    private String contactDetails;

    @Column(name = "client_comment")
    private String clientComment;

    @Column(name = "manager_comment")
    private String managerComment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = BookingStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
