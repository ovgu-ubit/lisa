package helper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Map;

import javax.naming.InitialContext;

public class FileArchiver {

	String CRLF = "\r\n"; // Line separator required by multipart/form-data.
	String url;
	String token;

	@SuppressWarnings("unchecked")
	public FileArchiver() {
		try {
			InitialContext initialContext = new InitialContext();
			if (initialContext != null) {
				Map<String, String> system = (Map<String, String>) initialContext.lookup("java:/comp/env/auth");
				this.url = system.get("auth_url") + "user/file/";
				this.token = system.get("auth_token");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int archiveFile(File file, String username, String email) throws IOException {
		String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
		// File file = new File("Unbenannt.bmp");

		HttpURLConnection connection = (HttpURLConnection) new URL(url + username).openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Cookie", "auth-token=" + token);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		// connection.setRequestProperty( "Content-Length",
		// String.valueOf(bytes.length));
		// connection.connect();
		OutputStream os = connection.getOutputStream();
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
		
		//email
		writer.append(CRLF);
		writer.append("--" + boundary).append(CRLF);
		writer.append("Content-Disposition: form-data; name=\"email\"")
				.append(CRLF);
		writer.append(CRLF).flush();		
		writer.append(email).append(CRLF);		
		
		//writer.append("--" + boundary + "--").append(CRLF).flush();
		

		// Send binary file.
		writer.append("--" + boundary).append(CRLF);
		writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"")
				.append(CRLF);
		writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(CRLF);
		writer.append("Content-Transfer-Encoding: binary").append(CRLF);
		writer.append(CRLF).flush();

		// content
		Files.copy(file.toPath(), os);

		// writer.append(str);
		os.flush(); // Important before continuing with writer!
		writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
		// End of multipart/form-data.
		writer.append("--" + boundary + "--").append(CRLF).flush();

		return connection.getResponseCode();
	}
}
