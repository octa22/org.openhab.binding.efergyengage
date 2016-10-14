package org.openhab.binding.efergyengage.internal;

/**
 * Created by Ondřej Pečta on 10. 8. 2016.
 */
public class EfergyEngageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EfergyEngageException(String message) {
        super(message);
    }

    public EfergyEngageException(final Throwable cause) {
        super(cause);
    }

    public EfergyEngageException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
