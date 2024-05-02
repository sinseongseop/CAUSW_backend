package net.causw.application.comment;

import lombok.RequiredArgsConstructor;
import net.causw.application.dto.comment.ChildCommentCreateRequestDto;
import net.causw.application.dto.comment.ChildCommentResponseDto;
import net.causw.application.dto.comment.ChildCommentUpdateRequestDto;
import net.causw.application.spi.ChildCommentPort;
import net.causw.application.spi.CircleMemberPort;
import net.causw.application.spi.CommentPort;
import net.causw.application.spi.PostPort;
import net.causw.application.spi.UserPort;
import net.causw.domain.exceptions.BadRequestException;
import net.causw.domain.exceptions.ErrorCode;
import net.causw.domain.exceptions.InternalServerException;
import net.causw.domain.exceptions.UnauthorizedException;
import net.causw.domain.model.comment.ChildCommentDomainModel;
import net.causw.domain.model.circle.CircleMemberDomainModel;
import net.causw.domain.model.enums.CircleMemberStatus;
import net.causw.domain.model.comment.CommentDomainModel;
import net.causw.domain.model.post.PostDomainModel;
import net.causw.domain.model.enums.Role;
import net.causw.domain.model.util.MessageUtil;
import net.causw.domain.model.util.StaticValue;
import net.causw.domain.model.user.UserDomainModel;
import net.causw.domain.validation.CircleMemberStatusValidator;
import net.causw.domain.validation.ConstraintValidator;
import net.causw.domain.validation.ContentsAdminValidator;
import net.causw.domain.validation.TargetIsDeletedValidator;
import net.causw.domain.validation.UserEqualValidator;
import net.causw.domain.validation.UserRoleIsNoneValidator;
import net.causw.domain.validation.UserStateValidator;
import net.causw.domain.validation.ValidatorBucket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChildCommentService {
    private final ChildCommentPort childCommentPort;
    private final CommentPort commentPort;
    private final UserPort userPort;
    private final CircleMemberPort circleMemberPort;
    private final PostPort postPort;
    private final Validator validator;


    @Transactional
    public ChildCommentResponseDto createChildComment(String creatorId, ChildCommentCreateRequestDto childCommentCreateRequestDto) {
        ValidatorBucket validatorBucket = ValidatorBucket.of();

        UserDomainModel creatorDomainModel = this.userPort.findById(creatorId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.LOGIN_USER_NOT_FOUND
                )
        );

        validatorBucket
                .consistOf(UserStateValidator.of(creatorDomainModel.getState()))
                .consistOf(UserRoleIsNoneValidator.of(creatorDomainModel.getRole()));

        Optional<ChildCommentDomainModel> refChildCommentDomainModel = childCommentCreateRequestDto.getRefChildComment().map(
                refChildCommentId -> this.childCommentPort.findById(refChildCommentId).orElseThrow(
                        () -> new BadRequestException(
                                ErrorCode.ROW_DOES_NOT_EXIST,
                                MessageUtil.COMMENT_NOT_FOUND
                        )
                )
        );

        CommentDomainModel parentCommentDomainModel = this.commentPort.findById(childCommentCreateRequestDto.getParentCommentId()).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.COMMENT_NOT_FOUND
                )
        );

        PostDomainModel postDomainModel = this.postPort.findPostById(parentCommentDomainModel.getPostId()).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.POST_NOT_FOUND
                )
        );

        ChildCommentDomainModel childCommentDomainModel = ChildCommentDomainModel.of(
                childCommentCreateRequestDto.getContent(),
                refChildCommentDomainModel.map(refChildComment -> refChildComment.getWriter().getName()).orElse(null),
                childCommentCreateRequestDto.getRefChildComment().orElse(null),
                creatorDomainModel,
                parentCommentDomainModel
        );

        validatorBucket
                .consistOf(UserStateValidator.of(creatorDomainModel.getState()))
                .consistOf(UserRoleIsNoneValidator.of(creatorDomainModel.getRole()))
                .consistOf(TargetIsDeletedValidator.of(postDomainModel.getBoard().getIsDeleted(), StaticValue.DOMAIN_BOARD))
                .consistOf(TargetIsDeletedValidator.of(postDomainModel.getIsDeleted(), StaticValue.DOMAIN_POST))
                .consistOf(ConstraintValidator.of(childCommentDomainModel, this.validator));

        refChildCommentDomainModel.ifPresent(
                refChildComment -> validatorBucket
                        .consistOf(TargetIsDeletedValidator.of(refChildComment.getIsDeleted(), StaticValue.DOMAIN_CHILD_COMMENT))
        );

        postDomainModel.getBoard().getCircle()
                .filter(circleDomainModel -> !creatorDomainModel.getRole().equals(Role.ADMIN) && !creatorDomainModel.getRole().getValue().contains("PRESIDENT"))
                .ifPresent(
                        circleDomainModel -> {
                            CircleMemberDomainModel circleMemberDomainModel = this.circleMemberPort.findByUserIdAndCircleId(
                                    creatorId,
                                    circleDomainModel.getId()
                            ).orElseThrow(
                                    () -> new UnauthorizedException(
                                            ErrorCode.NOT_MEMBER,
                                            MessageUtil.CIRCLE_APPLY_INVALID
                                    )
                            );

                            validatorBucket
                                    .consistOf(TargetIsDeletedValidator.of(circleDomainModel.getIsDeleted(), StaticValue.DOMAIN_CIRCLE))
                                    .consistOf(CircleMemberStatusValidator.of(
                                            circleMemberDomainModel.getStatus(),
                                            List.of(CircleMemberStatus.MEMBER)
                                    ));
                        }
                );

        validatorBucket
                .validate();

        return ChildCommentResponseDto.of(
                this.childCommentPort.create(childCommentDomainModel, postDomainModel),
                creatorDomainModel,
                postDomainModel.getBoard()
        );
    }

    @Transactional
    public ChildCommentResponseDto updateChildComment(
            String updaterId,
            String childCommentId,
            ChildCommentUpdateRequestDto childCommentUpdateRequestDto
    ) {
        ValidatorBucket validatorBucket = ValidatorBucket.of();

        UserDomainModel updater = this.userPort.findById(updaterId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.LOGIN_USER_NOT_FOUND
                )
        );

        ChildCommentDomainModel childCommentDomainModel = this.childCommentPort.findById(childCommentId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.COMMENT_NOT_FOUND
                )
        );

        PostDomainModel postDomainModel = this.postPort.findPostById(childCommentDomainModel.getParentComment().getPostId()).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.POST_NOT_FOUND
                )
        );

        childCommentDomainModel.update(
                childCommentUpdateRequestDto.getContent()
        );

        validatorBucket
                .consistOf(UserStateValidator.of(updater.getState()))
                .consistOf(UserRoleIsNoneValidator.of(updater.getRole()))
                .consistOf(TargetIsDeletedValidator.of(postDomainModel.getBoard().getIsDeleted(), StaticValue.DOMAIN_BOARD))
                .consistOf(TargetIsDeletedValidator.of(postDomainModel.getIsDeleted(), StaticValue.DOMAIN_POST))
                .consistOf(TargetIsDeletedValidator.of(childCommentDomainModel.getIsDeleted(), StaticValue.DOMAIN_CHILD_COMMENT))
                .consistOf(ConstraintValidator.of(childCommentDomainModel, this.validator))
                .consistOf(ContentsAdminValidator.of(
                        updater.getRole(),
                        updaterId,
                        childCommentDomainModel.getWriter().getId(),
                        List.of()
                ));

        postDomainModel.getBoard().getCircle()
                .filter(circleDomainModel -> !updater.getRole().equals(Role.ADMIN) && !updater.getRole().getValue().contains("PRESIDENT"))
                .ifPresent(
                        circleDomainModel -> {
                            CircleMemberDomainModel circleMemberDomainModel = this.circleMemberPort.findByUserIdAndCircleId(
                                    updaterId,
                                    circleDomainModel.getId()
                            ).orElseThrow(
                                    () -> new UnauthorizedException(
                                            ErrorCode.NOT_MEMBER,
                                            MessageUtil.CIRCLE_APPLY_INVALID
                                    )
                            );

                            validatorBucket
                                    .consistOf(TargetIsDeletedValidator.of(circleDomainModel.getIsDeleted(), StaticValue.DOMAIN_CIRCLE))
                                    .consistOf(CircleMemberStatusValidator.of(
                                            circleMemberDomainModel.getStatus(),
                                            List.of(CircleMemberStatus.MEMBER)
                                    ));
                        }
                );

        validatorBucket
                .validate();

        return ChildCommentResponseDto.of(
                this.childCommentPort.update(childCommentId, childCommentDomainModel).orElseThrow(
                        () -> new InternalServerException(
                                ErrorCode.INTERNAL_SERVER,
                                MessageUtil.INTERNAL_SERVER_ERROR
                        )
                ),
                updater,
                postDomainModel.getBoard()
        );
    }

    @Transactional
    public ChildCommentResponseDto deleteChildComment(String deleterId, String childCommentId) {
        ValidatorBucket validatorBucket = ValidatorBucket.of();

        UserDomainModel deleterDomainModel = this.userPort.findById(deleterId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.LOGIN_USER_NOT_FOUND
                )
        );

        ChildCommentDomainModel childCommentDomainModel = this.childCommentPort.findById(childCommentId).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.COMMENT_NOT_FOUND
                )
        );

        PostDomainModel postDomainModel = this.postPort.findPostById(childCommentDomainModel.getParentComment().getPostId()).orElseThrow(
                () -> new BadRequestException(
                        ErrorCode.ROW_DOES_NOT_EXIST,
                        MessageUtil.POST_NOT_FOUND
                )
        );

        validatorBucket
                .consistOf(UserStateValidator.of(deleterDomainModel.getState()))
                .consistOf(UserRoleIsNoneValidator.of(deleterDomainModel.getRole()))
                .consistOf(TargetIsDeletedValidator.of(childCommentDomainModel.getIsDeleted(), StaticValue.DOMAIN_CHILD_COMMENT));

        postDomainModel.getBoard().getCircle()
                .filter(circleDomainModel -> !deleterDomainModel.getRole().equals(Role.ADMIN) && !deleterDomainModel.getRole().getValue().contains("PRESIDENT"))
                .ifPresentOrElse(
                        circleDomainModel -> {
                            CircleMemberDomainModel circleMemberDomainModel = this.circleMemberPort.findByUserIdAndCircleId(
                                    deleterId,
                                    circleDomainModel.getId()
                            ).orElseThrow(
                                    () -> new UnauthorizedException(
                                            ErrorCode.NOT_MEMBER,
                                            MessageUtil.CIRCLE_APPLY_INVALID
                                    )
                            );

                            validatorBucket
                                    .consistOf(TargetIsDeletedValidator.of(circleDomainModel.getIsDeleted(), StaticValue.DOMAIN_CIRCLE))
                                    .consistOf(CircleMemberStatusValidator.of(
                                            circleMemberDomainModel.getStatus(),
                                            List.of(CircleMemberStatus.MEMBER)
                                    ))
                                    .consistOf(ContentsAdminValidator.of(
                                            deleterDomainModel.getRole(),
                                            deleterId,
                                            childCommentDomainModel.getWriter().getId(),
                                            List.of(Role.LEADER_CIRCLE)
                                    ));

                            if (deleterDomainModel.getRole().getValue().contains("LEADER_CIRCLE") && !childCommentDomainModel.getWriter().getId().equals(deleterId)) {
                                validatorBucket
                                        .consistOf(UserEqualValidator.of(
                                                circleDomainModel.getLeader().map(UserDomainModel::getId).orElseThrow(
                                                        () -> new InternalServerException(
                                                                ErrorCode.INTERNAL_SERVER,
                                                                MessageUtil.CIRCLE_WITHOUT_LEADER
                                                        )
                                                ),
                                                deleterId
                                        ));
                            }
                        },
                        () -> validatorBucket
                                .consistOf(ContentsAdminValidator.of(
                                        deleterDomainModel.getRole(),
                                        deleterId,
                                        childCommentDomainModel.getWriter().getId(),
                                        List.of()
                                ))

                );

        validatorBucket
                .validate();

        return ChildCommentResponseDto.of(
                this.childCommentPort.delete(childCommentId).orElseThrow(
                        () -> new InternalServerException(
                                ErrorCode.INTERNAL_SERVER,
                                MessageUtil.INTERNAL_SERVER_ERROR
                        )
                ),
                deleterDomainModel,
                postDomainModel.getBoard()
        );
    }
}