package hu.pagavcs.bl;

public class PagaException extends Exception {

	private final PagaExceptionType type;

	public enum PagaExceptionType {
		LOGIN_FAILED, CONNECTION_ERROR, UNIMPLEMENTED, INVALID_PARAMETERS
	}

	public PagaException(PagaExceptionType type) {
		super(type.toString());
		this.type = type;

	}

	public PagaExceptionType getType() {
		return type;
	}

}
