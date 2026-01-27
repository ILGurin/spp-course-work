package by.cloud.file_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DirectoryConflictException extends RuntimeException {
    public DirectoryConflictException(String message) {
        super(message);
    }

    public DirectoryConflictException(String message, Object... args) {
        super(String.format(message, args));
    }
}

