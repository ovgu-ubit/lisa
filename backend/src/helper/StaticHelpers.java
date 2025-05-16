package helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class StaticHelpers {

	public static String inputStreamToString(InputStream inputStream) {
		Scanner scanner = new Scanner(inputStream, "UTF-8");
		String res = "";
		if (scanner.hasNext())
			res = scanner.useDelimiter("\\A").next();
		else
			res = "";
		scanner.close();
		return res;
	}

	public static boolean copyTo(File source, File dest) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(source));
			out = new BufferedOutputStream(new FileOutputStream(dest));
			byte[] buffer = new byte[1024];
			int lengthRead;
			while ((lengthRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, lengthRead);
				out.flush();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (Exception e) {
				}
			;
			if (out != null)
				try {
					out.close();
				} catch (Exception e) {
				}
			;
		}
	}
}
