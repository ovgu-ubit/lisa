package helper;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mailer {
	String relay = "fake.mailrelay.test";
	Session session;

	public Mailer() {
		Properties prop = System.getProperties();
		prop.setProperty("mail.smtp.host", relay);
		session = Session.getDefaultInstance(prop, null);
	}

	public void sendMail(String from, String to, String subject, String text) throws MessagingException {
		MimeMessage message = new MimeMessage(session);

		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject(subject);
		message.setText(text);

		Transport.send(message);
	}

	public void sendMailHTML(String from, String to, String subject, String textHTML) throws MessagingException {
		MimeMessage message = new MimeMessage(session);

		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject(subject);
		message.setContent(textHTML, "text/html; charset=utf-8");

		Transport.send(message);
	}

	public void sendMailWithAttachement(String from, String to, String subject, String text, String attachementFile,
			String attachementName) throws AddressException, MessagingException {
		MimeMessage message = new MimeMessage(session);

		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject(subject);

		// text
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(text);
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		// attachement
		BodyPart messageBodyPart2 = new MimeBodyPart();
		DataSource source = new FileDataSource(attachementFile);
		messageBodyPart2.setDataHandler(new DataHandler(source));
		messageBodyPart2.setFileName(attachementName);
		multipart.addBodyPart(messageBodyPart2);

		message.setContent(multipart);

		Transport.send(message);
	}

	public static void main(String[] args) {
		Mailer m = new Mailer();
		try {
			m.sendMail("chrischu@ovgu.de", "sbosse@ovgu.de", "test", "testtext");
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
