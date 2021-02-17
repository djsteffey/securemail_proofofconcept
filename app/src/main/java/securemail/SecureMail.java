package securemail;

import java.security.Security;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import java.nio.charset.*;

public class SecureMail {
    // const
    private static final String HEADER = "===SECUREMAIL===";

    // variables
    private String m_username;
    private String m_password;
    private byte[] m_publickey;
    private byte[] m_privatekey;

    // methods
    public SecureMail() throws Exception{
        // set username and password
        this.m_username = "****";
        this.m_password = "****";

        // generate keys
        if (this.generateKeys() == false){
            System.out.println("Unable to generate encryption keys");
            return;
        }

        // send a test message
        if (this.sendTestMessage() == false){
            System.out.println("Unable to send test message");
            return;
        }
        System.out.println("Test message sent");

        // read all unread messages
        if (this.readUnreadMessages() == false){
            System.out.println("Unable to read unread messages");
        }
        System.out.println("Done");
    }

    private boolean generateKeys(){
        // first see if we can read them from a file
        try{
            FileInputStream fis = new FileInputStream("user.keys");
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.m_publickey = (byte[])ois.readObject();
            this.m_privatekey = (byte[])ois.readObject();
            ois.close();
            fis.close();
            System.out.println("Keys loaded from file");
            return true;
        } catch (Exception e){
            try{
                // couldnt load from file so try to generate them new
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
                gen.initialize(512, random);

                KeyPair pair = gen.generateKeyPair();
                this.m_publickey = pair.getPublic().getEncoded();
                this.m_privatekey = pair.getPrivate().getEncoded();

                FileOutputStream fos = new FileOutputStream("user.keys");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(this.m_publickey);
                oos.writeObject(this.m_privatekey);
                oos.close();
                fos.close();

                System.out.println("New keys generated");

                return true;
            } catch (Exception e1){
                e1.printStackTrace();
                return false;
            }
        }
    }

    private boolean sendTestMessage(){
        String to = "****";
        String from = "****";
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("****", "****");
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("This is the Subject Line!");
            message.setText(this.encodeMessageBody("This is my test message"));
            Transport.send(message);
            return true;
        } catch (Exception mex) {
            mex.printStackTrace();
            return false;
        }
    }

    private boolean readUnreadMessages() {
        try{
            // setup the session
            Session session = Session.getDefaultInstance(new Properties());

            // open the store
            Store store = session.getStore("imaps");
            store.connect("imap.googlemail.com", 993, "djsteffey@gmail.com", "dhxtkzktlgsddmne");

            // get the inbox folder
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // get number unread messages
            int unread_count = inbox.getUnreadMessageCount();

            // get number total messages
            int message_count = inbox.getMessageCount();
            System.out.println(unread_count + " unread messages and " + message_count + " total messages");

            // get the last unread messages
            Message[] messages = inbox.getMessages(message_count - unread_count + 1, message_count);

            System.out.println(messages.length + " messages fetched");

            // go through each message
            for ( Message message : messages ) {
                System.out.println(
                    "From: " + message.getFrom()[0] +
                    " Subject: " + message.getSubject() + 
                    "\n\t" + this.decodeMessageBody(message.getContent().toString())
                );
            }

            // done
            return true;

        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    static void sendMail(){
        // Mention the Recipient's email address
        String to = "djsteffey@gmail.com";
        // Mention the Sender's email address
        String from = "djsteffey@gmail.com";
        // Mention the SMTP server address. Below Gmail's SMTP server is being used to send email
        String host = "smtp.gmail.com";
        // Get system properties
        Properties properties = System.getProperties();
        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        // Get the Session object.// and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("djsteffey@gmail.com", "dhxtkzktlgsddmne");
            }
        });
        // Used to debug SMTP issues
        session.setDebug(true);
        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);
            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));
            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // Set Subject: header field
            message.setSubject("This is the Subject Line!");
            // Now set the actual message
            message.setText("This is actual message");
            System.out.println("sending...");
            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

    String encodeMessageBody(String content) throws Exception{
        return HEADER + "\n" + this.encryptText(content);
    }

    String decodeMessageBody(String content) throws Exception{
        // make sure has header
        if (content.substring(0, HEADER.length()).equals(HEADER)){
            return this.decryptText(content.substring(HEADER.length()));
        }
        return "Not encrypted with SECUREMAIL: " + content;
    }

    String encryptText(String text) throws Exception{
        byte[] data = encrypt(this.m_publickey, text.getBytes("UTF-8"));
        String encoded = Base64.getEncoder().encodeToString(data);
        return encoded;
    }

    String decryptText(String text) throws Exception{
        byte[] decoded = Base64.getDecoder().decode(text.trim());
        byte[] data = decrypt(this.m_privatekey, decoded);
        String s = new String(data, StandardCharsets.UTF_8);
        return s;
    }

    private static byte[] encrypt(byte[] public_key, byte[] input_data) throws Exception{
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(public_key));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(input_data);
    }

    private static byte[] decrypt(byte[] private_key, byte[] input_data) throws Exception {
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(private_key));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(input_data);
    }
}
