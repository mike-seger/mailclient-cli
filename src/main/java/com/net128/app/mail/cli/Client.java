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
            System.out.println("Usage: java Client [boxes|messages|show|delete|send] ...");
            return;
        }
        
        var props = new Properties();
        try (InputStream input = new FileInputStream("mail.properties")) {
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        var user = props.getProperty("user");
        var password = props.getProperty("password");
        var imapHost = props.getProperty("imap-host");
        var imapPort = props.getProperty("imap-port");

        var imapService = new IMAPService(user, password, imapHost, imapPort);
        var smtpHost = props.getProperty("smtp-host");
        var smtpPort = props.getProperty("smtp-port");
        var smtpService = new SMTPService(smtpHost, smtpPort, user, password, false);

        try {
            if(!"send".equals(args[0])) imapService.connect();
            switch (args[0]) {
                case "boxes":
                    imapService.listMailboxes().forEach(System.out::println);
                    break;
                case "messages":
                    if (args.length < 2) {
                        System.out.println("Usage: java Client messages <box-name>");
                        return;
                    }
                    imapService.listMessages(args[1]).forEach(System.out::println);
                    break;
                case "show":
                    if (args.length < 3) {
                        System.out.println("Usage: java Client show <box-name> <message-number>");
                        return;
                    }
                    var content = imapService.showMessage(Integer.parseInt(args[2]), args[1]);
                    System.out.println(content);
                    break;
                case "delete":
                    if (args.length < 3) {
                        System.out.println("Usage: java Client delete <box-name> <message-number>");
                        return;
                    }
                    var result = imapService.deleteMessage(Integer.parseInt(args[2]), args[1]);
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
                    smtpService.sendMessage(args[1], args[2], args[3], args[4]);
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
