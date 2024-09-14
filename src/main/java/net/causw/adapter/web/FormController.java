package net.causw.adapter.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.causw.application.dto.form.*;
import net.causw.application.form.FormService;
import net.causw.config.security.SecurityService;
import net.causw.config.security.userdetails.CustomUserDetails;
import net.causw.domain.exceptions.ErrorCode;
import net.causw.domain.exceptions.UnauthorizedException;
import net.causw.domain.model.util.MessageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/forms")
public class FormController {
    private final FormService formService;
    private final SecurityService securityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.isActiveAndNotNoneUserAndAcademicRecordCertified() and " +
            "hasAnyRole('ADMIN','PERSIDENT', 'VICE_PRESIDENT', 'LEADER_CIRCLE')")
    public FormResponseDto createForm(
            @Valid @RequestBody FormCreateRequestDto formCreateRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    )
    {
        return formService.createForm(userDetails.getUser(), formCreateRequestDto);
    }

    @GetMapping("/{formId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@securityService.isActiveAndNotNoneUserAndAcademicRecordCertified()")
    public FormResponseDto findForm(@PathVariable(name = "formId") String formId) {
        if (!securityService.hasAccessToForm(formId)) {
            throw new UnauthorizedException(ErrorCode.API_NOT_ACCESSIBLE, MessageUtil.API_NOT_ACCESSIBLE);
        }
        return formService.findForm(formId);
    }

    @DeleteMapping("/{formId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@securityService.isActiveAndNotNoneUserAndAcademicRecordCertified() and " +
            "hasAnyRole('ADMIN','PERSIDENT', 'VICE_PRESIDENT', 'LEADER_CIRCLE')")
    public void deleteForm(
            @PathVariable(name = "formId") String formId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        formService.deleteForm(formId, userDetails.getUser());
    }

    @PostMapping("/{formId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@securityService.isActiveAndNotNoneUserAndAcademicRecordCertified()")
    public void replyForm(
            @PathVariable(name = "formId") String formId,
            @Valid @RequestBody FormReplyRequestDto formReplyRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        if (!securityService.hasAccessToForm(formId)) {
            throw new UnauthorizedException(ErrorCode.API_NOT_ACCESSIBLE, MessageUtil.API_NOT_ACCESSIBLE);
        }
        formService.replyForm(formId, formReplyRequestDto, userDetails.getUser());
    }

    @GetMapping("/{formId}/results")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@securityService.isActiveAndNotNoneUserAndAcademicRecordCertified() and " +
            "hasAnyRole('ADMIN','PERSIDENT', 'VICE_PRESIDENT', 'LEADER_CIRCLE')")
    public List<ReplyUserResponseDto> findUserReply(
            @PathVariable(name = "formId") String formId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return formService.findUserReply(formId, userDetails.getUser());
    }

    @GetMapping("/{formId}/summary")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@securityService.isActiveAndNotNoneUserAndAcademicRecordCertified() and " +
            "hasAnyRole('ADMIN','PERSIDENT', 'VICE_PRESIDENT', 'LEADER_CIRCLE')")
    public List<QuestionSummaryResponseDto> findSummaryReply(
            @PathVariable(name = "formId") String formId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return formService.findSummaryReply(formId, userDetails.getUser());
    }
}