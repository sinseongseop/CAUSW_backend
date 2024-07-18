package net.causw.application.circle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Validator;
import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.repository.*;
import net.causw.application.dto.circle.CircleResponseDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    CircleResponseDto circleResponseDto;

    @BeforeEach
    void setUp() {
        circle = ObjectFixtures.getCircle();
        String circleId = "testCircleID";
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
    }

    @Test
    @DisplayName("Id에 해당하는 cicle 찾기 test")
    void findById() {
        // given
        String circleId = "testCircleID";

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

}