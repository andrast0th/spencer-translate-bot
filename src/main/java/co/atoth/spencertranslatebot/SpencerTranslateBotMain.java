package co.atoth.spencertranslatebot;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class SpencerTranslateBotMain {

    private static final Logger logger = LoggerFactory.getLogger(SpencerTranslateBotMain.class);

    static final String SLACK_API_KEY = System.getProperty("slackApiKey");
    static final String GOOGLE_API_KEY = System.getProperty("googleApiKey");

    static final String MASTER_USERNAME = "slbruce";
    static final String MASTER_GREET = "*:crown: Long live Master Spencer! :crown: :bow: I exist to serve you. :bow:*";

    private static MessageHandler messageHandler = new MessageHandler();

    public static void main(String[] args) throws IOException {

        logger.debug("SLACK_API_KEY: " + SLACK_API_KEY);
        logger.debug("GOOGLE_API_KEY: " + GOOGLE_API_KEY);
        logger.debug("META: " + System.getProperty("line.separator") + messageHandler.getMetaFromManifest());

        if(SLACK_API_KEY == null){
            throw new RuntimeException("SLACK_API_KEY vm argument missing, exiting...");
        }
        if(GOOGLE_API_KEY == null){
            throw new RuntimeException("GOOGLE_API_KEY vm argument missing, exiting...");
        }

        SlackSession session;

        //Set proxy if needed
        if(System.getProperties().containsKey("http.proxyHost") && System.getProperties().containsKey("http.proxyPort")){
            String proxyAddress = System.getProperty("http.proxyHost");
            int port = Integer.parseInt(System.getProperty("http.proxyPort"));

            session = SlackSessionFactory
                        .getSlackSessionBuilder(SLACK_API_KEY)
                        .withAutoreconnectOnDisconnection(true)
                        .withConnectionHeartbeat(5000, TimeUnit.MILLISECONDS)
                        .withProxy(Proxy.Type.HTTP, proxyAddress, port)
                        .build();

        } else {
            session = SlackSessionFactory
                    .createWebSocketSlackSession(SLACK_API_KEY);
        }

        session.addMessagePostedListener((event, messageSession) -> {

            SlackChannel channel = event.getChannel();
            SlackUser sender = event.getSender();
            String message  = event.getMessageContent();

            //Self filter
            if(session.sessionPersona().getId().equals(event.getSender().getId())){
                return;
            }

            //Check if command parsing needed
            String cmdReply = messageHandler.parseCommands(session, channel, message);
            if(cmdReply != null){
                if(sender.getUserName().equals(MASTER_USERNAME)){
                    cmdReply = MASTER_GREET + "\n" + cmdReply;
                }
                session.sendMessage(channel, cmdReply);
            } else {

                //Check if active on this channel
                if(!messageHandler.isActive(channel)){
                    return;
                }

                //Check if translation need
                String reply = messageHandler.getTranslation(session, sender, message);
                if(reply != null){
                    sendReplyMessage(reply, session, event);
                }
            }
        });

        session.connect();
    }

    private static void sendReplyMessage(String message, SlackSession session, SlackMessagePosted event){

        SlackChannel channel = event.getChannel();

        String replyTimeStamp;
        if(event.getThreadTimestamp() != null){
            replyTimeStamp = event.getThreadTimestamp();
        } else {
            replyTimeStamp = event.getTimeStamp();
        }

        SlackPreparedMessage msg =
                new SlackPreparedMessage.Builder()
                        .withThreadTimestamp(replyTimeStamp)
                        .withMessage(message)
                        .build();

        session.sendMessage(channel, msg);
    }

}