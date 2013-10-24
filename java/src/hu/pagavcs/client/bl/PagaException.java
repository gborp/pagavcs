package hu.pagavcs.client.bl;

public class PagaException extends Exception {

	private final PagaExceptionType type;
	private String verboseMessage;

	public enum PagaExceptionType {
		LOGIN_FAILED, CONNECTION_ERROR, UNIMPLEMENTED, INVALID_PARAMETERS, NOT_DIRECTORY
	}

	public PagaException(PagaExceptionType type) {
		super(type.toString());
		this.type = type;
	}

	public PagaException(PagaExceptionType type, String verboseMessage) {
		super(type.toString() + " " + verboseMessage);
		this.type = type;
		this.verboseMessage = verboseMessage;
	}

	public PagaExceptionType getType() {
		return type;
	}

	public String getVerboseMessage() {
		return verboseMessage;
	}

}
