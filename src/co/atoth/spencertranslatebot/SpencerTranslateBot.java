package co.atoth.spencertranslatebot;

import com.ullink.slack.simpleslackapi.*;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class SpencerTranslateBot {

    private static final Logger logger = LoggerFactory.getLogger(SpencerTranslateBot.class);

    public static final String SLACK_API_KEY = System.getProperty("slackApiKey");
    public static final String GOOGLE_API_KEY = System.getProperty("googleApiKey");
    public static final String CHANNEL_NAME = System.getProperty("channelName");

    private static MessageHandler messageHandler = new MessageHandler();

    public static void main(String[] args) throws IOException {

        logger.debug("SLACK_API_KEY: " + SLACK_API_KEY);
        logger.debug("GOOGLE_API_KEY: " + GOOGLE_API_KEY);
        logger.debug("CHANNEL_NAME: " + CHANNEL_NAME);

        //System.out.println(TranslateUtil.isRomanian("ce se intampla", .3f));

        SlackSession session = SlackSessionFactory
                .getSlackSessionBuilder(SLACK_API_KEY)
                .withAutoreconnectOnDisconnection(true)
                .withConnectionHeartbeat(5000, TimeUnit.MILLISECONDS)
                .withProxy(Proxy.Type.HTTP, System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")))
                .build();

        session.addMessagePostedListener((event, messageSession) -> {

            SlackChannel channel = event.getChannel();
            SlackUser sender = event.getSender();
            String message  = event.getMessageContent();

            //Self filter
            if(session.sessionPersona().getId().equals(event.getSender().getId())){
                return;
            }
            //Channel filter if set
            if(!channel.getName().equals(CHANNEL_NAME)){
                return;
            }

            //Check if command parsing needed
            String cmdReply = messageHandler.parseCommands(session.sessionPersona().getId(), message);
            if(cmdReply != null){
                session.sendMessage(channel, cmdReply);
            } else {

                //Check if translation need
                String reply = messageHandler.getTranslation(sender.getUserName(), message);
                if(reply != null){
                    sendReplyMessage(reply, session, event);
                }
            }
        });

        // GREET THY KING
        session.addPresenceChangeListener((event, presenceSession) -> {
            SlackUser user = presenceSession.findUserByUserName("slbruce");

            if(event.getPresence() == SlackPersona.SlackPresence.ACTIVE && event.getUserId().equals(user.getId())){

                SlackPreparedMessage msg =
                        new SlackPreparedMessage.Builder()
                                .withMessage(":crown: Welcome Master Spencer! :crown: :bow: I exist to serve you. :bow:")
                                .build();

                presenceSession.sendMessageToUser(user, msg);
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
                        .withMessage(Jsoup.parse(message).text())
                        .build();

        session.sendMessage(channel, msg);
    }

}