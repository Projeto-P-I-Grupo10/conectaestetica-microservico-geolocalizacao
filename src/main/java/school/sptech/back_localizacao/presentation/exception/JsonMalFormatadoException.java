package school.sptech.back_localizacao.presentation.exception;

public class JsonMalFormatadoException extends RuntimeException {
    public JsonMalFormatadoException(String message, Throwable e) {
        super(message, e);
    }
}
