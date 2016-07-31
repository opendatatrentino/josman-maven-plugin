package eu.trentorise.opendata.josman.exceptions;

/**
 * A runtime exception to raise when something is not found.
 * 
 * @since 0.8.0
 */
public class JosmanIoException extends JosmanException {
    
    private static final long serialVersionUID = 1L;

    private JosmanIoException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     * 
     * @since 0.8.0
     */
    public JosmanIoException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.8.0
     */
    public JosmanIoException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.8.0
     */
    public JosmanIoException(String msg) {
        super(msg);
    }
}