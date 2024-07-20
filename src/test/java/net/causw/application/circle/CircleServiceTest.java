package net.causw.application.circle;

import groovy.lang.Tuple2;
import jakarta.validation.Validator;
import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.circle.CircleMember;
import net.causw.adapter.persistence.repository.*;
import net.causw.adapter.persistence.user.User;
import net.causw.application.dto.circle.CircleResponseDto;
import net.causw.application.dto.circle.CirclesResponseDto;
import net.causw.application.util.TestUtil;
import net.causw.domain.exceptions.BadRequestException;
import net.causw.domain.exceptions.ErrorCode;
import net.causw.domain.exceptions.UnauthorizedException;
import net.causw.domain.model.enums.CircleMemberStatus;
import net.causw.domain.model.enums.Role;
import net.causw.domain.model.enums.UserState;
import net.causw.domain.model.util.MessageUtil;
import net.causw.domain.model.util.StaticValue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

// 서버 오류 발생으로 인한 test 실패를 구분하기 위해, 우선 MockitoExtension으로 test 코드를 작성 후
// 마지막에 Springboot를 이용하는 test 코드로 어노테이션 변경
@ExtendWith(MockitoExtension.class)
class CircleServiceTest {

    @InjectMocks
    CircleService  circleService;

    @Mock
    CircleRepository circleRepository;

    @Mock
    CircleMemberRepository circleMemberRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    BoardRepository boardRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    Validator validator;

    Circle circle;
    CircleMember circleMember;
    User user;

    @BeforeEach
    void setUp() {
        circle = ObjectFixtures.getCircle("circle1");
        circleMember = ObjectFixtures.getCircleMember(CircleMemberStatus.MEMBER, circle);
        user = ObjectFixtures.getUser("user1");
    }

    @Test
    @DisplayName("findById 성공 테스트 - circleID로 찾기")
    void findById() {
        // given
        String circleId = "testCircleID";
        Long MemberCount = 2L;
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.getNumMember(circleId)).willReturn(MemberCount);

        // when
        CircleResponseDto result = circleService.findById(circleId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(circle.getName());
        assertThat(result.getMainImage()).isEqualTo(circle.getMainImage());
        assertThat(result.getDescription()).isEqualTo(circle.getDescription());
        assertThat(result.getIsDeleted()).isEqualTo(circle.getIsDeleted());
        assertThat(result.getLeaderId()).isEqualTo(circle.getLeader().get().getId());
        assertThat(result.getLeaderName()).isEqualTo(circle.getLeader().get().getName());
        assertThat(result.getNumMember()).isEqualTo(MemberCount);
    }

    @Test
    @DisplayName("findById 에러 테스트 - circle이 존재하지 않는 경우")
    void findById_whenCircleNotFound() {
        // given
        String nonExistentCircleId = "nonExistentId";
        given(circleRepository.findById(nonExistentCircleId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> circleService.findById(nonExistentCircleId))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROW_DOES_NOT_EXIST)
                .hasMessage(MessageUtil.SMALL_CLUB_NOT_FOUND);
    }

    @Test
    @DisplayName("findById 에러 테스트 - 삭제된 Circle ID 호출")
    void findById_whenDeletedCircle() {
        // Given
        String deletedCircleId = "deletedCircleId";
        circle = ObjectFixtures.getDeletedCirle();
        given(circleRepository.findById(deletedCircleId)).willReturn(Optional.of(circle));

        // When & Then
        assertThatThrownBy(() -> circleService.findById(deletedCircleId))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TARGET_DELETED)
                .hasMessage("삭제된 " + StaticValue.DOMAIN_CIRCLE + " 입니다.");
    }

    @ParameterizedTest
    @DisplayName("findAll 성공 테스트 - User가 Admin/President/VicePresident 인 경우")
    @EnumSource(value = Role.class, names = {"ADMIN", "PRESIDENT", "VICE_PRESIDENT"})
    void findAll_whenAdminOrPresidentOrVicePresident(Role role) {
        // Given
        Circle circle2 = ObjectFixtures.getCircle("circle2");
        User user = ObjectFixtures.getUser(role);
        String userId = user.getId();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(circleRepository.findAll()).willReturn(Arrays.asList(circle, circle2));
        given(circleMemberRepository.findByUser_Id(userId)).willReturn(Arrays.asList());

        // When
        List<CirclesResponseDto> result = circleService.findAll(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo(circle.getName());
        assertThat(result.get(1).getName()).isEqualTo(circle2.getName());
    }

    @ParameterizedTest
    @DisplayName("findAll 성공 테스트 - 동아리 가입상태가 Member와 Await 가 섞여 있는 경우  ")
    @EnumSource(value = Role.class, names = {"COUNCIL","LEADER_CIRCLE","COMMON"})
    void findAll_whenNormalUser(Role role) {
        // Given
        List<Circle> circles = Arrays.asList(circle, ObjectFixtures.getCircle("circle2"),
                ObjectFixtures.getCircle("circle3"));
        List<CircleMember> circleMembers = Arrays.asList(circleMember,
                ObjectFixtures.getCircleMember(CircleMemberStatus.MEMBER, circles.get(1)),
                ObjectFixtures.getCircleMember(CircleMemberStatus.AWAIT, circles.get(2)));
        User user = ObjectFixtures.getUser(role);
        String userId = user.getId();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(circleRepository.findAll()).willReturn(circles);
        given(circleMemberRepository.findByUser_Id(userId)).willReturn(circleMembers);

        // 메소드 안에서 Map이 CircleID를 키로 사용하기 떄문에 ID가 반드시 필요
        for(int i=0; i < circles.toArray().length ; i++){
            TestUtil.setId(circles.get(i),"id","circle"+i);
        }

        // When
        List<CirclesResponseDto> result = circleService.findAll(userId);

        // Then
        assertThat(result).hasSize(3);
        for(int i=0; i<2; i++){
            assertThat(result.get(i).getName()).isEqualTo(circles.get(i).getName());
            assertThat(result.get(i).getIsJoined()).isEqualTo(TRUE);
        }
        assertThat(result.get(2).getName()).isEqualTo(circles.get(2).getName());
        assertThat(result.get(2).getIsJoined()).isEqualTo(FALSE);
    }

    @Test
    @DisplayName("findAll 에러 테스트 - User가 존재하지 않는 경우")
    void findAll_whenUserNotFound() {
        // given
        String nonExistentUserId = "nonExistentId";
        given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> circleService.findAll(nonExistentUserId))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROW_DOES_NOT_EXIST)
                .hasMessage(MessageUtil.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("findAll 에러 테스트 - User의 Role이 None 인 경우")
    void findAll_whenUserIsNone() {
        // given
        user = ObjectFixtures.getUser(Role.NONE);
        String userId = "roleNone";
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> circleService.findAll(userId))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEED_SIGN_IN)
                .hasMessage("접근 권한이 없습니다. 다시 로그인 해주세요. 문제 반복시 관리자에게 문의해주세요.");
    }

    @ParameterizedTest
    @DisplayName("findAll 에러 테스트 - userState에 따른 에러 종류 확인 ")
    @EnumSource(value = UserState.class, names = {"DROP", "INACTIVE", "AWAIT", "REJECT"})
    void findAll_whenUserStateThrowsException(UserState state) {
        // given
        user = ObjectFixtures.getUser(state);
        String userId = user.getId();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // 각각의 state에 해당하는 에러
        Map<UserState, Tuple2<ErrorCode, String>> expectedErrors = Map.of(
                UserState.DROP, new Tuple2<>(ErrorCode.BLOCKED_USER, "추방된 사용자 입니다."),
                UserState.INACTIVE, new Tuple2<>(ErrorCode.INACTIVE_USER, "비활성화된 사용자 입니다."),
                UserState.AWAIT, new Tuple2<>(ErrorCode.AWAITING_USER, "대기 중인 사용자 입니다."),
                UserState.REJECT, new Tuple2<>(ErrorCode.REJECT_USER, "가입이 거절된 사용자 입니다.")
        );

        Tuple2<ErrorCode, String> expectedError = expectedErrors.get(state);

        // when & then
        assertThatThrownBy(() -> circleService.findAll(userId))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedError.getFirst())
                .hasMessage(expectedError.getSecond());
    }

}