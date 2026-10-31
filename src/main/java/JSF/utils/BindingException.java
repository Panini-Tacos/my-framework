package JSF.utils;

public class BindingException extends Exception {

    private static final long serialVersionUID = 1L;

    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
