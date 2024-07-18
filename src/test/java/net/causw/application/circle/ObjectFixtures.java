package net.causw.application.circle;

import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.user.User;
import net.causw.domain.model.enums.Role;
import net.causw.domain.model.enums.UserState;
import net.causw.domain.model.user.UserDomainModel;

import static java.lang.Boolean.FALSE;

public class ObjectFixtures {

    //User
    public static User getUser() {
        UserDomainModel userDomain = UserDomainModel.of("user1", "email", "name", "password", "studentId"
                , 2021, Role.ADMIN, "profileImage", "testRefreshToken",UserState.ACTIVE);
        return User.from(userDomain);
    }

    // Circle
    public static Circle getCircle() {
        return Circle.of("testUser1","testMainImage","testDescription", FALSE, getUser());
    }

}