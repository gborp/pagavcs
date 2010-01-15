package hu.pagavcs.bl;

public class PagaException extends Exception {

	private final PagaExceptionType type;

	public enum PagaExceptionType {
		LOGIN_FAILED
	}

	public PagaException(PagaExceptionType type) {
		this.type = type;

	}

	public PagaExceptionType getType() {
		return type;
	}

}
