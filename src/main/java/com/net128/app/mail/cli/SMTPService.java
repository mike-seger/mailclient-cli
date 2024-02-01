package com.net128.app.mail.cli;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

public class SMTPService {
    private String smtpHost;
    private String smtpPort;
    private String user;
    private String password;
    private boolean requiresAuth;

    public SMTPService(String smtpHost, String smtpPort, String user, String password, boolean requiresAuth) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.user = user;
        this.password = password;
        this.requiresAuth = requiresAuth; // Indicates if the server requires authentication
    }

    public void sendMessage(String from, String to, String subject, String body) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", requiresAuth ? "true" : "false");
        properties.put("mail.smtp.starttls.enable", "false"); // Adjust this based on your server's requirements
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        Session session;
        if (requiresAuth) {
            session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            });
        } else {
            session = Session.getInstance(properties);
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        
        // Create a message part to handle the message body
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(unescapeString(body), "text/plain");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    private String unescapeString(String text) {
        return text.replace("\\n", "\n").replace("\\t", "\t"); // Add more as needed
    }
}
