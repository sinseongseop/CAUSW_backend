package net.causw.application.form;

import jakarta.servlet.http.HttpServletResponse;
import net.causw.adapter.persistence.board.Board;
import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.circle.CircleMember;
import net.causw.adapter.persistence.form.*;
import net.causw.adapter.persistence.post.Post;
import net.causw.adapter.persistence.repository.board.BoardRepository;
import net.causw.adapter.persistence.repository.circle.CircleMemberRepository;
import net.causw.adapter.persistence.repository.circle.CircleRepository;
import net.causw.adapter.persistence.repository.form.*;
import net.causw.adapter.persistence.repository.post.PostRepository;
import net.causw.adapter.persistence.repository.user.UserRepository;
import net.causw.adapter.persistence.repository.userCouncilFee.UserCouncilFeeRepository;
import net.causw.adapter.persistence.userCouncilFee.UserCouncilFee;
import net.causw.application.dto.form.response.OptionResponseDto;
import net.causw.application.dto.form.response.QuestionResponseDto;
import net.causw.application.dto.form.response.reply.*;
import net.causw.application.dto.form.request.FormReplyRequestDto;
import net.causw.application.dto.form.response.OptionSummaryResponseDto;
import net.causw.application.dto.form.request.QuestionReplyRequestDto;
import net.causw.application.dto.form.response.QuestionSummaryResponseDto;
import net.causw.application.dto.util.dtoMapper.FormDtoMapper;
import net.causw.application.excel.FormExcelService;
import net.causw.domain.aop.annotation.MeasureTime;
import net.causw.domain.exceptions.BadRequestException;
import net.causw.domain.exceptions.ErrorCode;
import net.causw.domain.exceptions.InternalServerException;
import net.causw.domain.exceptions.UnauthorizedException;
import net.causw.domain.model.enums.circle.CircleMemberStatus;
import net.causw.domain.model.enums.form.FormType;
import net.causw.domain.model.enums.form.QuestionType;
import net.causw.domain.model.enums.form.RegisteredSemester;
import net.causw.domain.model.enums.user.Role;
import net.causw.domain.model.enums.userAcademicRecord.AcademicStatus;
import net.causw.domain.model.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import net.causw.adapter.persistence.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@MeasureTime
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormService {
    private final FormRepository formRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final BoardRepository boardRepository;
    private final CircleRepository circleRepository;
    private final ReplyQuestionRepository replyQuestionRepository;
    private final UserRepository userRepository;
    private final CircleMemberRepository circleMemberRepository;
    private final UserCouncilFeeRepository userCouncilFeeRepository;
    private final ReplyRepository replyRepository;
    private final PostRepository postRepository;
    private final FormExcelService formExcelService;

    @Transactional
    public void closeForm(
            String formId,
            User user
    ) {
        Form form = getForm(formId);

        validateCanAccessFormResult(user, form);

        form.setIsClosed(true);

        formRepository.save(form);
    }

    @Transactional
    public void replyForm(
            String formId,
            FormReplyRequestDto formReplyRequestDto,
            User writer
    ) {
        Form form = getForm(formId);

        if (form.getIsClosed()) {
            throw new BadRequestException(
                    ErrorCode.NOT_ALLOWED_TO_REPLY_FORM,
                    MessageUtil.FORM_CLOSED
            );
        }

        // writer가 해당 form에 대한 권한이 있는지 확인
        Set<AcademicStatus> allowedAcademicStatus = new HashSet<>();

        if (form.getIsAllowedEnrolled())
            allowedAcademicStatus.add(AcademicStatus.ENROLLED);
        if (form.getIsAllowedLeaveOfAbsence())
            allowedAcademicStatus.add(AcademicStatus.LEAVE_OF_ABSENCE);
        if (form.getIsAllowedGraduation())
            allowedAcademicStatus.add(AcademicStatus.GRADUATED);

        if (!allowedAcademicStatus.contains(writer.getAcademicStatus())) {
            throw new BadRequestException(
                ErrorCode.NOT_ALLOWED_TO_REPLY_FORM,
                MessageUtil.NOT_ALLOWED_TO_REPLY_FORM
            );
        } else {
            if (allowedAcademicStatus.contains(AcademicStatus.ENROLLED)
                    && writer.getAcademicStatus().equals(AcademicStatus.ENROLLED)
            ) {
                EnumSet<RegisteredSemester> allowedRegisteredSemester = form.getEnrolledRegisteredSemester();
                if (!allowedRegisteredSemester
                        .stream()
                        .map(RegisteredSemester::getSemester)
                        .collect(Collectors.toSet())
                        .contains(writer.getCurrentCompletedSemester())
                ) {
                    throw new BadRequestException(
                            ErrorCode.NOT_ALLOWED_TO_REPLY_FORM,
                            MessageUtil.NOT_ALLOWED_TO_REPLY_FORM
                    );
                }

                if (form.getIsNeedCouncilFeePaid()) {
                    // 학생회비 납부 필요
                    UserCouncilFee userCouncilFee = userCouncilFeeRepository.findByUser(writer).orElseThrow(
                            () -> new BadRequestException(
                                    ErrorCode.NOT_ALLOWED_TO_REPLY_FORM,
                                    MessageUtil.NOT_ALLOWED_TO_REPLY_FORM
                            )
                    );

                    if (!getIsAppliedCurrentSemester(userCouncilFee)) {
                        throw new BadRequestException(
                                ErrorCode.NOT_ALLOWED_TO_REPLY_FORM,
                                MessageUtil.NOT_ALLOWED_TO_REPLY_FORM
                        );
                    }
                }
            }

            if (allowedAcademicStatus.contains(AcademicStatus.LEAVE_OF_ABSENCE)
                    && writer.getAcademicStatus().equals(AcademicStatus.LEAVE_OF_ABSENCE)
            ) {
                EnumSet<RegisteredSemester> allowedRegisteredSemester = form.getLeaveOfAbsenceRegisteredSemester();
                if (!allowedRegisteredSemester
                        .stream()
                        .map(RegisteredSemester::getSemester)
                        .collect(Collectors.toSet())
                        .contains(writer.getCurrentCompletedSemester())
                ) {
                    throw new BadRequestException(
                            ErrorCode.NOT_ALLOWED_TO_REPLY_FORM,
                            MessageUtil.NOT_ALLOWED_TO_REPLY_FORM
                    );
                }
            }
        }

        // 중복 답변 검사
        if (replyRepository.existsByFormAndUser(form, writer)) {
            throw new BadRequestException(
                    ErrorCode.ROW_ALREADY_EXIST,
                    MessageUtil.ALREADY_REPLIED
            );
        }


        // 주관식, 객관식 질문에 따라 유효한 답변인지 검증 및 저장
        List<ReplyQuestion> replyQuestionList = new ArrayList<>();

        for (QuestionReplyRequestDto questionReplyRequestDto : formReplyRequestDto.getQuestionReplyRequestDtoList()) {
            FormQuestion formQuestion = getQuestion(questionReplyRequestDto.getQuestionId());

            // 객관식일 시
            if (formQuestion.getQuestionType().equals(QuestionType.OBJECTIVE)) {
                if (questionReplyRequestDto.getQuestionReply() != null) {
                    throw new BadRequestException(
                            ErrorCode.INVALID_PARAMETER,
                            MessageUtil.INVALID_REPLY_INFO
                    );
                }

                if (!formQuestion.getIsMultiple() && questionReplyRequestDto.getSelectedOptionList().size() > 1) {
                    throw new BadRequestException(
                            ErrorCode.INVALID_PARAMETER,
                            MessageUtil.INVALID_REPLY_INFO
                    );
                }

                // 객관식일 시: 유효한 옵션 번호 선택했는지 검사
                List<Integer> formQuestionOptionNumberList = formQuestion.getFormQuestionOptionList()
                        .stream()
                        .map(FormQuestionOption::getNumber)
                        .toList();

                questionReplyRequestDto.getSelectedOptionList().forEach(optionNumber -> {
                    if (!formQuestionOptionNumberList.contains(optionNumber)) {
                        throw new BadRequestException(
                                ErrorCode.INVALID_PARAMETER,
                                MessageUtil.INVALID_REPLY_INFO
                        );
                    }
                });
            }
            // 주관식일 시
            else {
                if (questionReplyRequestDto.getSelectedOptionList() != null) {
                    throw new BadRequestException(
                            ErrorCode.INVALID_PARAMETER,
                            MessageUtil.INVALID_REPLY_INFO
                    );
                }
            }

            ReplyQuestion replyQuestion = ReplyQuestion.of(
                    formQuestion,
                    formQuestion.getQuestionType().equals(QuestionType.SUBJECTIVE) ?
                            questionReplyRequestDto.getQuestionReply()
                            : null,
                    formQuestion.getQuestionType().equals(QuestionType.OBJECTIVE) ?
                            questionReplyRequestDto.getSelectedOptionList()
                            : null
            );

            replyQuestionList.add(replyQuestion);
        }

        replyRepository.save(Reply.of(form, writer, replyQuestionList));
    }

    public ReplyPageResponseDto findAllReplyPageByForm(String formId, Pageable pageable, User user){
        Form form = getForm(formId);

        validateCanAccessFormResult(user, form);

        Page<Reply> replyPage = replyRepository.findAllByForm(form, pageable);

        return toReplyPageResponseDto(form, replyPage);
    }

    //2. 각 질문별 결과를 반환(요약)
    public List<QuestionSummaryResponseDto> findSummaryReply(String formId, User user){
        Form form = getForm(formId);

        validateCanAccessFormResult(user, form);

        List<FormQuestion> formQuestionList = form.getFormQuestionList();

        List<ReplyQuestion> replyQuestionList = replyQuestionRepository.findAllByForm(form);

        Map<FormQuestion, List<ReplyQuestion>> replyQuestionMap = formQuestionList
                .stream()
                .collect(Collectors.toMap(
                        formQuestion -> formQuestion,
                        formQuestion -> replyQuestionList.stream()
                                .filter(replyQuestion -> replyQuestion.getFormQuestion().equals(formQuestion))
                                .collect(Collectors.toList())
                ));

        List<QuestionSummaryResponseDto> questionSummaryResponseDtoList = new ArrayList<>();
        for (FormQuestion formQuestion : formQuestionList) {
            questionSummaryResponseDtoList.add(
                    toQuestionSummaryResponseDto(
                            formQuestion,
                            replyQuestionMap.get(formQuestion)
                    )
            );
        }

        return questionSummaryResponseDtoList;
    }

    public void exportFormResult(String formId, User user, HttpServletResponse response) {
        Form form = getForm(formId);

        validateCanAccessFormResult(user, form);

        String fileName = form.getTitle() + "_결과";

        List<Reply> replyList = replyRepository.findAllByForm(form);

        ReplyListResponseDto replyListResponseDto = toReplyListResponseDto(form, replyList);

        List<String> headerStringList = new ArrayList<>(List.of(
                "제출 시각",
                "이메일(아이디)",
                "이름",
                "닉네임",
                "입학년도",
                "학번",
                "학부/학과",
                "전화번호",
                "학적상태",
                "현재 학기",
                "졸업 년도",
                "졸업 유형",
                "동문네트워크 가입일",
                "본 학기 학생회비 적용 여부",
                "납부 시점 학기",
                "납부한 학기 수",
                "잔여 학생회비 적용 학기",
                "환불 여부"
        ));
        List<String> questionStringList = replyListResponseDto.getQuestionResponseDtoList()
                .stream()
                .map(questionResponseDto -> (
                        questionResponseDto.getQuestionNumber().toString()
                                + "."
                                + questionResponseDto.getQuestionText()
                )).toList();
        headerStringList.addAll(questionStringList);

        LinkedHashMap<String, List<ReplyResponseDto>> sheetNameDataMap = new LinkedHashMap<>();
        sheetNameDataMap.put("결과", replyListResponseDto.getReplyResponseDtoList());

        formExcelService.generateExcel(
                response,
                fileName,
                headerStringList,
                sheetNameDataMap
        );
    }

    // private methods
    private void validateCanAccessFormResult(User user, Form form) {
        if (form.getFormType().equals(FormType.POST_FORM)) {
            Post post = postRepository.findByForm(form).orElseThrow(
                    () -> new BadRequestException(
                            ErrorCode.ROW_DOES_NOT_EXIST,
                            MessageUtil.FORM_NOT_FOUND
                    )
            );
            if (post.getWriter().equals(user)) {
                throw new UnauthorizedException(
                        ErrorCode.API_NOT_ALLOWED,
                        MessageUtil.NOT_ALLOWED_TO_ACCESS_REPLY
                );
            }
        } else {
            Circle circle = form.getCircle();
            if (circle == null) {
                throw new InternalServerException(
                        ErrorCode.INTERNAL_SERVER,
                        MessageUtil.INTERNAL_SERVER_ERROR
                );
            }
            CircleMember circleMember = circleMemberRepository.findByUser_IdAndCircle_Id(user.getId(), circle.getId()).orElseThrow(
                    () -> new UnauthorizedException(
                            ErrorCode.NOT_MEMBER,
                            MessageUtil.CIRCLE_APPLY_INVALID
                    )
            );
            if (circleMember.getStatus().equals(CircleMemberStatus.MEMBER) ||
                    !user.getRoles().contains(Role.LEADER_CIRCLE)
            ) {
                throw new UnauthorizedException(
                        ErrorCode.API_NOT_ALLOWED,
                        MessageUtil.NOT_ALLOWED_TO_ACCESS_REPLY
                );
            }
        }
    }

    private User getUser(String userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.USER_NOT_FOUND
                )
        );
    }

    private Form getForm(String formId){
        return formRepository.findById(formId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.FORM_NOT_FOUND
                )
        );
    }

    private FormQuestion getQuestion(String questionId){
        return questionRepository.findById(questionId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.QUESTION_NOT_FOUND
                )
        );
    }

    private FormQuestionOption getOption(String optionId){
        return optionRepository.findById(optionId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.OPTION_NOT_FOUND
                )
        );

    }

    private Circle getCircle(String circleId) {
        return circleRepository.findById(circleId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.SMALL_CLUB_NOT_FOUND
                )
        );
    }

    private Board getBoard(String boardId) {
        return boardRepository.findById(boardId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.BOARD_NOT_FOUND
                )
        );
    }

    private CircleMember getCircleMember(String userId, String circleId) {
        return circleMemberRepository.findByUser_IdAndCircle_Id(userId, circleId).orElseThrow(
                () -> new UnauthorizedException(
                        ErrorCode.NOT_MEMBER,
                        MessageUtil.CIRCLE_APPLY_INVALID
                )
        );
    }

    private User getCircleLeader(Circle circle) {
        User leader = circle.getLeader().orElse(null);
        if (leader == null) {
            throw new InternalServerException(
                    ErrorCode.INTERNAL_SERVER,
                    MessageUtil.CIRCLE_WITHOUT_LEADER
            );
        }
        return leader;
    }

    private Boolean getIsAppliedCurrentSemester(UserCouncilFee userCouncilFee) {
        Integer startOfAppliedSemester = userCouncilFee.getPaidAt();
        Integer endOfAppliedSemester = ( userCouncilFee.getIsRefunded() ) ?
                ( startOfAppliedSemester - 1 ) + userCouncilFee.getNumOfPaidSemester() :
                userCouncilFee.getRefundedAt();
        Boolean isAppliedThisSemester;

        if (userCouncilFee.getIsJoinedService()) {
            isAppliedThisSemester = (startOfAppliedSemester <= userCouncilFee.getUser().getCurrentCompletedSemester()) &&
                    (userCouncilFee.getUser().getCurrentCompletedSemester() <= endOfAppliedSemester);
        } else {
            isAppliedThisSemester = (startOfAppliedSemester <= userCouncilFee.getCouncilFeeFakeUser().getCurrentCompletedSemester()) &&
                    (userCouncilFee.getCouncilFeeFakeUser().getCurrentCompletedSemester() <= endOfAppliedSemester);
        }
        return isAppliedThisSemester;
    }

    private Integer getRestOfSemester(UserCouncilFee userCouncilFee) {
        Integer startOfAppliedSemester = userCouncilFee.getPaidAt();
        Integer endOfAppliedSemester = ( userCouncilFee.getIsRefunded() ) ?
                ( startOfAppliedSemester - 1 ) + userCouncilFee.getNumOfPaidSemester() :
                userCouncilFee.getRefundedAt();
        Integer restOfSemester;

        if (userCouncilFee.getIsJoinedService()) {
            restOfSemester = Math.max(endOfAppliedSemester - userCouncilFee.getUser().getCurrentCompletedSemester(), 0);
        } else {
            restOfSemester = Math.max(endOfAppliedSemester - userCouncilFee.getCouncilFeeFakeUser().getCurrentCompletedSemester(), 0);
        }
        return restOfSemester;
    }

    // Dto Mapper

    private ReplyPageResponseDto toReplyPageResponseDto(Form form, Page<Reply> replyPage) {
        return FormDtoMapper.INSTANCE.toReplyPageResponseDto(
                form.getFormQuestionList().stream()
                        .map(this::toQuestionResponseDto)
                        .toList(),

                replyPage.map(reply -> {
                    User replyUser = reply.getUser();

                    List<ReplyQuestionResponseDto> questionReplyList = reply.getReplyQuestionList()
                            .stream()
                            .map(this::toReplyQuestionResponseDto)
                            .toList();

                    return this.toReplyResponseDto(
                            replyUser,
                            questionReplyList,
                            reply.getCreatedAt()
                    );
                })
        );
    }

    private ReplyListResponseDto toReplyListResponseDto(Form form, List<Reply> replyList) {
        return FormDtoMapper.INSTANCE.toReplyListResponseDto(
                form.getFormQuestionList().stream()
                        .map(this::toQuestionResponseDto)
                        .toList(),

                replyList.stream()
                        .map(reply -> {
                            User replyUser = reply.getUser();

                            List<ReplyQuestionResponseDto> questionReplyList = reply.getReplyQuestionList()
                                    .stream()
                                    .map(this::toReplyQuestionResponseDto)
                                    .toList();

                            return this.toReplyResponseDto(
                                    replyUser,
                                    questionReplyList,
                                    reply.getCreatedAt()
                            );
                        })
                        .toList()
        );
    }

    private QuestionResponseDto toQuestionResponseDto(FormQuestion formQuestion) {
        return FormDtoMapper.INSTANCE.toQuestionResponseDto(
                formQuestion,
                formQuestion.getFormQuestionOptionList().stream()
                        .map(this::toOptionResponseDto)
                        .toList());
    }

    private OptionResponseDto toOptionResponseDto(FormQuestionOption formQuestionOption) {
        return FormDtoMapper.INSTANCE.toOptionResponseDto(formQuestionOption);
    }

    private ReplyQuestionResponseDto toReplyQuestionResponseDto(ReplyQuestion replyQuestion) {
        return FormDtoMapper.INSTANCE.toReplyQuestionResponseDto(replyQuestion);
    }

    private ReplyResponseDto toReplyResponseDto(User user, List<ReplyQuestionResponseDto> replyQuestionResponseDtoList, LocalDateTime createdAt) {
        return FormDtoMapper.INSTANCE.toReplyResponseDto(
                this.toReplyUserResponseDto(user),
                replyQuestionResponseDtoList,
                createdAt
        );
    }

    private ReplyUserResponseDto toReplyUserResponseDto(User user) {
        UserCouncilFee userCouncilFee = userCouncilFeeRepository.findByUser(user).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.USER_COUNCIL_FEE_NOT_FOUND
                )
        );

        Boolean isAppliedThisSemester = getIsAppliedCurrentSemester(userCouncilFee);
        Integer restOfSemester = getRestOfSemester(userCouncilFee);

        return FormDtoMapper.INSTANCE.toReplyUserResponseDto(user, userCouncilFee, isAppliedThisSemester, restOfSemester);
    }




    private QuestionSummaryResponseDto toQuestionSummaryResponseDto(
            FormQuestion formQuestion,
            List<ReplyQuestion> replyQuestionList
    ) {
        if (formQuestion.getQuestionType().equals(QuestionType.SUBJECTIVE)) {
            return FormDtoMapper.INSTANCE.toQuestionSummaryResponseDto(
                    formQuestion,
                    replyQuestionList.stream()
                            .map(ReplyQuestion::getQuestionAnswer)
                            .toList(),
                    null
            );
        } else {
            return FormDtoMapper.INSTANCE.toQuestionSummaryResponseDto(
                    formQuestion,
                    null,
                    formQuestion.getFormQuestionOptionList().stream()
                            .map(formQuestionOption -> {
                                Long selectedCount = replyQuestionList
                                        .stream()
                                        .filter(replyQuestion -> replyQuestion.getSelectedOptionList().contains(formQuestionOption.getNumber()))
                                        .count();
                                return toOptionSummaryResponseDto(formQuestionOption, selectedCount);
                            })
                            .toList()
            );
        }

    }

    private OptionSummaryResponseDto toOptionSummaryResponseDto(
            FormQuestionOption formQuestionOption,
            Long selectedCount
    ) {
        return FormDtoMapper.INSTANCE.toOptionSummaryResponseDto(formQuestionOption, selectedCount);
    }

}
