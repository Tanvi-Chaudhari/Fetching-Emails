package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

public class GMail {
    private static final String APPLICATION_NAME = "Fetching Emails";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String USER_ID = "me";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

    private static final String CREDENTIALS_FILE_PATH =
            System.getProperty("user.dir") +
                    File.separator + "src" +
                    File.separator + "main" +
                    File.separator + "resources" +
                    File.separator + "credentials" +
                    File.separator + "credentials_new.json";

    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.dir") +
            File.separator + "src" +
            File.separator + "main" +
            File.separator + "resources" +
            File.separator + "credentials";

    private static Gmail service;
    private static Message message;


    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(new File(CREDENTIALS_FILE_PATH));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(9999).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /*
    Personalized Code
    */

    /* The getService() method returns the fully constructed and authorized Gmail service object,
    which can now be used to make API calls to interact with the user's Gmail account, such
    as listing emails, sending emails, etc.*/

    public static Gmail getService() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

    /* The getSenderAndSubject method retrieves the sender and subject of a specific email message from Gmail. */
    public static Map<String, String> getSenderAndSubject(Gmail service, String userId, Message message) throws IOException {
        Message fullMessage = service.users().messages().get(userId, message.getId()).execute();
        List<com.google.api.services.gmail.model.MessagePartHeader> headers = fullMessage.getPayload().getHeaders();

        String sender = "";
        String subject = "";

        for (MessagePartHeader header : headers) {
            if (header.getName().equalsIgnoreCase("From")) {
                sender = header.getValue();
            } else if (header.getName().equalsIgnoreCase("Subject")) {
                subject = header.getValue();
            }
        }

        // Return sender and subject as a map
        Map<String, String> emailDetails = new HashMap<>();
        emailDetails.put("sender", sender);
        emailDetails.put("subject", subject);

        return emailDetails;
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        // Suppress the warning
        Logger.getLogger(FileDataStoreFactory.class.getName()).setLevel(Level.SEVERE);

        try {
            Gmail service = getService(); // Initialize the service correctly
            List<Thread> threads = service.users().threads().list("me").execute().getThreads();

            // Limit to the latest 200 emails
            int maxThreads = Math.min(threads.size(), 200);

            for (int i = 0; i < maxThreads; i++) {
                String threadId = threads.get(i).getId();
                List<Message> response = service.users().threads().get("me", threadId).execute().getMessages();

                for (Message message : response) {
                    Map<String, String> emailDetails = getSenderAndSubject(service, USER_ID, message);
                    String sender = emailDetails.get("sender");
                    String subject = emailDetails.get("subject");

                    System.out.println("Sender: " + sender);
                    System.out.println("Subject: " + subject);
                    System.out.println("-----------------------------------------");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}