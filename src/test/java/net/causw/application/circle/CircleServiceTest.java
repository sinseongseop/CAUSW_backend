package net.causw.application.circle;

import jakarta.validation.Validator;
import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.repository.*;
import net.causw.application.dto.circle.CircleResponseDto;
import net.causw.domain.exceptions.BadRequestException;
import net.causw.domain.exceptions.ErrorCode;
import net.causw.domain.model.util.MessageUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

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

    @BeforeEach
    void setUp() {
        circle = ObjectFixtures.getCircle();
    }

    @Test
    @DisplayName("findById 성공 테스트 - circleID로 찾기")
    void findById() {
        // given
        String circleId = "testCircleID";
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));

        // when
        CircleResponseDto result = circleService.findById(circleId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(circle.getName());
        assertThat(result.getMainImage()).isEqualTo(circle.getMainImage());
        assertThat(result.getDescription()).isEqualTo(circle.getDescription());
        assertThat(result.getIsDeleted()).isEqualTo(circle.getIsDeleted());
        assertThat(result.getLeaderId()).isEqualTo(circle.getLeader().get().getId());
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



}