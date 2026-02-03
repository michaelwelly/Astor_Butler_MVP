package museon_online.astor_butler.domain.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring",
        imports = {LocalDateTime.class, UserRole.class})
public interface UserMapper {

    @Mapping(target = "telegramId", source = "id")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "username", source = "userName")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "role", expression = "java(UserRole.GUEST)")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    museon_online.astor_butler.domain.user.User fromTelegramUser(
            org.telegram.telegrambots.meta.api.objects.User telegramUser
    );
}