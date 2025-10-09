package museon_online.astor_butler.user;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long   id;

    @Column(nullable = false, unique = true)
    private Long   telegramId;

    private String firstName;
    private String lastName;
    private String username;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.GUEST;
}