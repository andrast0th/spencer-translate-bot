package co.atoth.spencertranslatebot;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SpencerTranslateBot {

    private static final Logger logger = LoggerFactory.getLogger(SpencerTranslateBot.class);

    public static final String SLACK_API_KEY = System.getProperty("slackApiKey");
    public static final String GOOGLE_API_KEY = System.getProperty("googleApiKey");
    public static final String CHANNEL_NAME = System.getProperty("channelName");

    public static void main(String[] args) throws IOException {

        logger.debug("SLACK_API_KEY: " + SLACK_API_KEY);
        logger.debug("GOOGLE_API_KEY: " + GOOGLE_API_KEY);
        logger.debug("CHANNEL_NAME: " + CHANNEL_NAME);

        SlackSession session = SlackSessionFactory.createWebSocketSlackSession(SLACK_API_KEY);
        session.addMessagePostedListener((event, session12) -> {
            SlackChannel channel = event.getChannel();
            SlackUser sender = event.getSender();
            String message  = event.getMessageContent();

            TranslateMessageHandler.onEvent(message, channel, sender, session12);
        });
        session.addMessageUpdatedListener((event, session1) -> {
            SlackChannel channel = event.getChannel();
            SlackUser sender = null;
            String message  = event.getNewMessage();

            TranslateMessageHandler.onEvent(message, channel, sender, session1);
        });
        session.connect();
    }
}