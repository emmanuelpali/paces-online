package com.pacesonline.runservice.exception;

import com.pacesonline.runservice.controller.RunController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RunController.class)
public class RunExceptionHandler {

    @ExceptionHandler(InvalidRunPaceException.class)
    public ProblemDetail handleInvalidRunPace(InvalidRunPaceException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}