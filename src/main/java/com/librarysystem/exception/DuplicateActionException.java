package com.librarysystem.exception;

import org.springframework.http.HttpStatus;

public class DuplicateActionException extends ApiException {

    public DuplicateActionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

}
