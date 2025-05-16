package helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.naming.InitialContext;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SSHConnection {
	// private String host_test = "xmdb.gbv.de";
	private String host_test = "t-lbsmdb.gbv.de";
	private String host_prod = "lbsmdb.gbv.de";
	private int port = 22;

	private Session session = null;

	@SuppressWarnings("unchecked")
	public SSHConnection(boolean test, String user) throws JSchException {
		String privateKey = null;
		String known_hosts = null;
		try {
			InitialContext initialContext = new InitialContext();
			if (initialContext != null) {
				Map<String, String> system = (Map<String, String>) initialContext.lookup("java:/comp/env/system");
				privateKey = system.get("key");
				known_hosts = system.get("known_hosts");
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		// establish SSH session
		JSch jsch = new JSch();
		// String privateKey = "/home/jetty/.ssh/id_rsa";
		jsch.addIdentity(privateKey);
		// jsch.setKnownHosts("/home/jetty/.ssh/known_hosts");
		jsch.setKnownHosts(known_hosts);

		session = test ? jsch.getSession(user, host_test, port) : jsch.getSession(user, host_prod, port);

		session.connect();

	}

	public void executeCommand(String command, boolean write_output) throws JSchException, IOException {
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);
		channel.setInputStream(null);
		((ChannelExec) channel).setErrStream(System.err);

		InputStream in = channel.getInputStream();
		channel.connect();
		byte[] tmp = new byte[1024];
		while (true) {
			while (in.available() > 0) {
				int i = in.read(tmp, 0, 1024);
				if (i < 0)
					break;
				if (write_output)
					System.out.print(new String(tmp, 0, i));
			}
			if (channel.isClosed()) {
				if (write_output)
					System.out.println("exit-status: " + channel.getExitStatus());
				break;
			}
			try {
				Thread.sleep(100);
			} catch (Exception ee) {
			}
		}
		channel.disconnect();
	}

	public void sftpToRemote(String lfile, String rfile) throws JSchException, SftpException {
		Channel sftp = session.openChannel("sftp");

		// 5 seconds timeout
		sftp.connect(5000);

		ChannelSftp channelSftp = (ChannelSftp) sftp;

		// transfer file from local to remote server
		channelSftp.put(lfile, rfile);

		// download file from remote server to local
		// channelSftp.get(remoteFile, localFile);

		channelSftp.exit();
	}

	public void scpToRemote(String lfile, String rfile) throws JSchException, IOException {
		boolean ptimestamp = false;
		FileInputStream fis = null;
		// exec 'scp -t rfile' remotely
		rfile = rfile.replace("'", "'\"'\"'");
		rfile = "'" + rfile + "'";
		String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();

		channel.connect();

		if (checkAck(in) != 0) {
			return;
		}

		File _lfile = new File(lfile);

		if (ptimestamp) {
			command = "T " + (_lfile.lastModified() / 1000) + " 0";
			// The access time should be sent here,
			// but it is not accessible with JavaAPI ;-<
			command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				return;
			}
		}

		// send "C0644 filesize filename", where filename should not include '/'
		long filesize = _lfile.length();
		command = "C0644 " + filesize + " ";
		if (lfile.lastIndexOf('/') > 0) {
			command += lfile.substring(lfile.lastIndexOf('/') + 1);
		} else {
			command += lfile;
		}
		command += "\n";
		out.write(command.getBytes());
		out.flush();
		if (checkAck(in) != 0) {
			return;
		}

		// send a content of lfile
		fis = new FileInputStream(lfile);
		byte[] buf = new byte[1024];
		while (true) {
			int len = fis.read(buf, 0, buf.length);
			if (len <= 0)
				break;
			out.write(buf, 0, len); // out.flush();
		}
		fis.close();
		fis = null;
		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();
		if (checkAck(in) != 0) {
			return;
		}
		out.close();
		channel.disconnect();
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print("ERROR: " + sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print("FATAL ERROR: " + sb.toString());
			}
		}
		return b;
	}

	@Override
	public void finalize() throws Throwable {
		if (session != null)
			session.disconnect();
	}

	public static class MyLogger implements com.jcraft.jsch.Logger {
		static java.util.Hashtable<Integer, String> name = new java.util.Hashtable<Integer, String>();
		static {
			name.put(new Integer(DEBUG), "DEBUG: ");
			name.put(new Integer(INFO), "INFO: ");
			name.put(new Integer(WARN), "WARN: ");
			name.put(new Integer(ERROR), "ERROR: ");
			name.put(new Integer(FATAL), "FATAL: ");
		}

		public boolean isEnabled(int level) {
			return true;
		}

		public void log(int level, String message) {
			System.err.print(name.get(new Integer(level)));
			System.err.println(message);
		}
	}
}
