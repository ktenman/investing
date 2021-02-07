package ee.tenman.investing.exception;

public class NotSupportedSymbolException extends RuntimeException {
    public NotSupportedSymbolException() {
        super();
    }

    public NotSupportedSymbolException(String s) {
        super(s);
    }

    public NotSupportedSymbolException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NotSupportedSymbolException(Throwable throwable) {
        super(throwable);
    }
}
