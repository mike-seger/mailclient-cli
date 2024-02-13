package com.net128.app.mail.cli;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.SortTerm;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.search.FlagTerm;
import java.io.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class IMAPService implements AutoCloseable {
    private final String user;
    private final String password;
    private final String imapHost;
    private final String imapPort;
    private Store store;

    public IMAPService(String user, String password, String imapHost, String imapPort) {
        this.user = user;
        this.password = password;
        this.imapHost = imapHost;
        this.imapPort = imapPort;
    }

    public void connect() throws MessagingException {
        var properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", imapHost);
        properties.put("mail.imap.port", imapPort);
        properties.put("mail.imap.ssl.enable", "false");

        Session session = Session.getInstance(properties);
        store = session.getStore();
        store.connect(imapHost, Integer.parseInt(imapPort), user, password);
    }

    public void close() throws MessagingException {
        if (store != null) {
            store.close();
        }
    }

    public List<String> listMailboxes() throws MessagingException {
        var mailboxes = new ArrayList<String>();
        var defaultFolder = store.getDefaultFolder();
        var folders = defaultFolder.list();
        for (Folder folder : folders) {
            mailboxes.add(folder.getFullName());
        }
        return mailboxes;
    }

    public List<String> listMessages(String boxName) throws MessagingException {
        var messagesInfo = new ArrayList<String>();
        var folder = store.getFolder(boxName);
        if (!folder.isOpen()) {
            folder.open(Folder.READ_ONLY);
        }
        if (!(folder instanceof IMAPFolder)) {
            System.out.println("This feature requires an IMAP server that supports the SORT extension.");
            return messagesInfo;
        }

        var imapFolder = (IMAPFolder) folder;
        
        // Use the IMAPFolder sort method - sorting by date in descending order
        var messages = imapFolder.getSortedMessages(new SortTerm[]{SortTerm.REVERSE, SortTerm.DATE});
        for (Message message : messages) {
            messagesInfo.add(String.format("%s - %s", message.getMessageNumber(), 
                message.getReceivedDate() != null ? message.getReceivedDate().toString() : "No Date", 
                message.getSubject() != null ? message.getSubject() : "No Subject"));
        }

        folder.close(false);
        return messagesInfo;
    }

    public String showMessage(int messageId, String boxName) throws MessagingException, IOException {
        var folder = store.getFolder(boxName);
        folder.open(Folder.READ_ONLY);
        var message = folder.getMessage(messageId);

        var subject = message.getSubject();
        subject = subject != null ? subject : "No Subject"; // Handle null subject

        var fromAddresses = message.getFrom();
        var from = (fromAddresses != null && fromAddresses.length > 0) ? fromAddresses[0].toString() : "Unknown Sender"; // Handle null or empty 'From' header

        var toAddresses = message.getRecipients(Message.RecipientType.TO);
        var to = (toAddresses != null && toAddresses.length > 0) 
            ? Arrays.stream(toAddresses).map(Address::toString).collect(Collectors.joining(", ")) 
            : "Unknown Recipient";

        var content = getTextFromMessage(message);

        folder.close(false);

        return String.format("Subject: %s\nFrom: %s\nTo: %s\n\nContent:\n%s", subject, from, to, content);
    }
    
    public boolean deleteMessage(int messageId, String boxName) {
        var success = false;
        Folder folder = null;
        try {
            folder = store.getFolder(boxName);
            folder.open(Folder.READ_WRITE); // Open in read-write mode
            var message = folder.getMessage(messageId);

            if (message != null) {
                message.setFlag(Flags.Flag.DELETED, true); // Mark the message for deletion
                folder.expunge(); // Permanently delete the message
                success = true;
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        } finally {
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(true); // Close the folder and expunge deleted messages
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            var mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        var result = new StringBuilder();
        var count = mimeMultipart.getCount();
        for (var i = 0; i < count; i++) {
            var bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
                break;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}
