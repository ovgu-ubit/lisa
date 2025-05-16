package servlet;

import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import guard.AccessGuard;

public abstract class PermissionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		String servletClassName = this.getClass().getName();
		ServletContext context = this.getServletContext();
		final Class<?>[] formal_args = { HttpServletRequest.class, HttpServletResponse.class };
		// Set role and app given by annotation, according to calling servlet and
		// respective method:
		for (ServiceMethod serviceMethod : ServiceMethod.values()) {
			try {
				Method method = this.getClass().getDeclaredMethod(serviceMethod.getServletMethod(), formal_args);
				AccessGuard annotation = method.getAnnotation(AccessGuard.class);
				if (annotation != null) {
					String contextSignature = "_" + servletClassName + "_" + serviceMethod.getName();
					context.setAttribute("Permissions" + contextSignature, annotation.permissions());
				}
			} catch (NoSuchMethodException | NullPointerException e) {
			}
		}
	}
}
