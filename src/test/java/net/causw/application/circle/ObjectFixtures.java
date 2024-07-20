package net.causw.application.circle;

import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.circle.CircleMember;
import net.causw.adapter.persistence.user.User;
import net.causw.domain.model.enums.CircleMemberStatus;
import net.causw.domain.model.enums.Role;
import net.causw.domain.model.enums.UserState;
import net.causw.domain.model.user.UserDomainModel;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class ObjectFixtures {

    //User
    public static User getUser(String userID){
        UserDomainModel userDomain = UserDomainModel.of(userID, "email3", "name3", "password3", "studentId3"
                , 2023, Role.COMMON, "profileImage3", "testRefreshToken3",UserState.ACTIVE);
        return User.from(userDomain);
    }

    public static User getUser(Role role){
        UserDomainModel userDomain = UserDomainModel.of("testID", "email3", "name3", "password3", "studentId3"
                , 2023, role, "profileImage3", "testRefreshToken3",UserState.ACTIVE);
        return User.from(userDomain);
    }

    public static User getUser(UserState userState){
        UserDomainModel userDomain = UserDomainModel.of("testID", "email", "name", "password", "studentId"
                , 2023, Role.COMMON, "profileImage", "testRefreshToken",userState);
        return User.from(userDomain);
    }

    // Circle
    public static Circle getCircle(String circleName) {
        return Circle.of(circleName,"testMainImage","testDescription", FALSE, getUser("testID"));
    }

    public static Circle getDeletedCirle(){
        return Circle.of("deletedCircleName","testMainImage","testDescription", TRUE, getUser("testID"));
    }

    // CircleMember
    public static CircleMember getCircleMember(CircleMemberStatus status, Circle circle){
        return CircleMember.of(status, circle ,getUser(Role.COMMON));
    }


}