import java.util.*;
import com.googlecode.gmail4j.*;
import com.googlecode.gmail4j.http.*;
import com.googlecode.gmail4j.rss.*;
import com.googlecode.gmail4j.auth.*;

class SecureMail{
    public static void main(String[] args){
        GmailClient client = new RssGmailClient();
        GmailConnection connection = new HttpGmailConnection(new Credentials("djsteffey", "ok".toCharArray()));
        client.setConnection(connection);
        final List<GmailMessage> messages = client.getUnreadMessages();
        for (GmailMessage message : messages) {
            System.out.println(message);
        }
    }
}