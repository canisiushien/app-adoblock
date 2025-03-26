package bf.canisiuslab.adoblock.exception;

/**
 * remonte au client les exceptions capturées
 *
 * @author Canisius <canisiushien@gmail.com>
 */
public class CustomException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CustomException(String message) {
        super(message);
    }
}
