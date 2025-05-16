package servlet;

public enum ServiceMethod {
	DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE;

	public final String getName() {
		return this.name().toUpperCase();
	}

	public final String getServletMethod() {
		String methodName = this.name();
		return "do" + Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1).toLowerCase();
	}
}
