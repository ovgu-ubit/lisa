package helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

public class ParamParsing {

	public static String trimWhitespaces(String s) throws UnsupportedEncodingException {
		return URLDecoder.decode(s, StandardCharsets.UTF_8.name()).replaceAll("[\u2000-\u200a]", " ").trim();
	}

	public static String parseString(HttpServletRequest request, String key) throws UnsupportedEncodingException {
		String url_value = request.getParameter(key);
		if (url_value == null) {
			return null;
		}
		return ParamParsing.trimWhitespaces(url_value);
	}

	public static String[] parseStringArray(HttpServletRequest request, String key, String sep)
			throws UnsupportedEncodingException {
		String url_value = request.getParameter(key);
		if (url_value == null) {
			return new String[] {};
		}
		String[] values = url_value.split(sep);
		String[] result = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = ParamParsing.trimWhitespaces(values[i]);
		}
		return result;
	}

	public static boolean parseBoolean(HttpServletRequest request, String key) throws UnsupportedEncodingException {
		String url_value = request.getParameter(key);
		if (url_value == null) {
			return false;
		}
		return Boolean.valueOf(ParamParsing.trimWhitespaces(url_value));
	}
}
