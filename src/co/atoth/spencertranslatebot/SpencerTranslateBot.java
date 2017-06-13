package co.atoth.spencertranslatebot;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.events.SlackMessageUpdated;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import com.ullink.slack.simpleslackapi.listeners.SlackMessageUpdatedListener;

import java.io.IOException;

public class SpencerTranslateBot {

    public static final String SLACK_API_KEY = System.getProperty("slackApiKey");
    public static final String GOOGLE_API_KEY = System.getProperty("googleApiKey");
    public static final String CHANNEL_NAME = System.getProperty("channelName");

    public static void main(String[] args) throws IOException {

        SlackSession session = SlackSessionFactory.createWebSocketSlackSession(SLACK_API_KEY);
        session.addMessagePostedListener(new SlackMessagePostedListener() {
            @Override
            public void onEvent(SlackMessagePosted event, SlackSession session) {

                SlackChannel channel = event.getChannel();
                SlackUser sender = event.getSender();
                String message  = event.getMessageContent();

                TranslateMessageHandler.onEvent(message, channel, sender, session);
            }
        });
        session.addMessageUpdatedListener(new SlackMessageUpdatedListener() {
            @Override
            public void onEvent(SlackMessageUpdated event, SlackSession session) {

                SlackChannel channel = event.getChannel();
                SlackUser sender = null;
                String message  = event.getNewMessage();

                TranslateMessageHandler.onEvent(message, channel, sender, session);
            }
        });
        session.connect();
    }
}