package by.example.cloud.controller;

import by.example.cloud.exceptions.UsernameAlreadyInUseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameAlreadyInUseException.class)
    public ResponseEntity<String> handleUsernameAlreadyExistsException() {
        String errorMessage = "User with this username already exists";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
    }


}
