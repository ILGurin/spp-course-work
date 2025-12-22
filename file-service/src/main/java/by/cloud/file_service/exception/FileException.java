package by.cloud.file_service.exception;

public class FileException extends RuntimeException {
    public FileException(String message) {
        super(message);
    }

    public FileException(String message, Object... args) {
        super(String.format(message, args));
    }
}

