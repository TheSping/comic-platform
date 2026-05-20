package comic.platform.backend.core.exception;

import lombok.Getter;

@Getter
public class ComicException extends RuntimeException {
    private final int code;

    public ComicException(int code, String message) {
        super(message);
        this.code = code;
    }
}
