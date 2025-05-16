package filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CORSInterceptor implements Filter {

	private static final String[] allowedOrigins = { "http://localhost:4200", "https://test.ub.ovgu.de",
			"https://service.ub.ovgu.de" };

	private static boolean isAllowedOrigin(String origin) {
		if (origin != null) {
			for (String allowedOrigin : allowedOrigins) {
				if (origin.equals(allowedOrigin)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		String origin = ((HttpServletRequest) servletRequest).getHeader("Origin");
		if (isAllowedOrigin(origin)) {
			// Authorize the origin, all headers as well as GET methods and enable cookie
			// passthrough:
			((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin", origin);
			((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers",
					"Content-Type, Authorization");
			((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods", "GET");
			((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Credentials", "true");
			// CORS handshake (pre-flight request for CORS options):
			if ("OPTIONS".equalsIgnoreCase(((HttpServletRequest) servletRequest).getMethod())) {
				((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK);
				return;
			}
		}
		// Pass the request along the filter chain:
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}
}
