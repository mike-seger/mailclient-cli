package com.net128.app.mail.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import jakarta.mail.MessagingException;

public class Client {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Client [boxes|messages <box-name>|show <box-name> <message-number>]");
            return;
        }
        
        // Load properties from imap.properties
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("mail.properties")) {
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        String user = props.getProperty("user");
        String password = props.getProperty("password");
        String imapHost = props.getProperty("imap-host");
        String imapPort = props.getProperty("imap-port");

        IMAPService imapService = new IMAPService(user, password, imapHost, imapPort);

        String smtpHost = props.getProperty("smtp-host");
        String smtpPort = props.getProperty("smtp-port");

        SMTPService smtpService = new SMTPService(smtpHost, smtpPort, user, password, false);

        try {
            switch (args[0]) {
                case "boxes":
                    imapService.connect();
                    List<String> mailboxes = imapService.listMailboxes();
                    mailboxes.forEach(System.out::println);
                    break;
                case "messages":
                    if (args.length < 2) {
                        System.out.println("Usage: java Client messages <box-name>");
                        return;
                    }
                    imapService.connect();
                    List<String> messages = imapService.listMessages(args[1]);
                    messages.forEach(System.out::println);
                    break;
                case "show":
                    if (args.length < 3) {
                        System.out.println("Usage: java Client show <box-name> <message-number>");
                        return;
                    }
                    imapService.connect();
                    String content = imapService.showMessage(Integer.parseInt(args[2]), args[1]);
                    System.out.println(content);
                    break;
                case "delete":
                    if (args.length < 3) {
                        System.out.println("Usage: java Client delete <box-name> <message-number>");
                        return;
                    }
                    imapService.connect();
                    boolean result = imapService.deleteMessage(Integer.parseInt(args[2]), args[1]);
                    if (result) {
                        System.out.println("Message deleted successfully.");
                    } else {
                        System.out.println("Failed to delete the message.");
                    }
                    break;
                case "send":
                    if (args.length < 5) {
                        System.out.println("Usage: java Client send <from> <to> <subject> <body>");
                        return;
                    }
                    String from = args[1];
                    String to = args[2];
                    String subject = args[3];
                    String body = args[4];
                    smtpService.sendMessage(from, to, subject, body);
                    System.out.println("Message sent successfully.");
                    break;
                default:
                    System.out.println("Invalid command. Use one of: boxes, messages, show");
                    break;
            }
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                imapService.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
