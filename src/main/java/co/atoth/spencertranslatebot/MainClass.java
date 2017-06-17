package co.atoth.spencertranslatebot;

import co.atoth.spencertranslatebot.repository.BotRepository;
import co.atoth.spencertranslatebot.repository.DefaultBotRepository;
import co.atoth.spencertranslatebot.util.BotInfo;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class MainClass {

    private static final Logger logger = LoggerFactory.getLogger(MainClass.class);

    public static final String SLACK_API_KEY = System.getProperty("slackApiKey");
    public static final String GOOGLE_API_KEY = System.getProperty("googleApiKey");

    public static void main(String[] args) throws IOException {

        logger.debug("SLACK_API_KEY: " + SLACK_API_KEY);
        logger.debug("GOOGLE_API_KEY: " + GOOGLE_API_KEY);
        logger.debug("META: " + System.getProperty("line.separator") + BotInfo.getInfo(System.getProperty("line.separator")));

        if(SLACK_API_KEY == null){
            throw new RuntimeException("SLACK_API_KEY vm argument missing, exiting...");
        }
        if(GOOGLE_API_KEY == null){
            throw new RuntimeException("GOOGLE_API_KEY vm argument missing, exiting...");
        }

        SlackSession slackSession;

        //Set proxy if needed
        if(System.getProperties().containsKey("http.proxyHost") && System.getProperties().containsKey("http.proxyPort")){
            String proxyAddress = System.getProperty("http.proxyHost");
            int port = Integer.parseInt(System.getProperty("http.proxyPort"));

            slackSession = SlackSessionFactory
                        .getSlackSessionBuilder(SLACK_API_KEY)
                        .withAutoreconnectOnDisconnection(true)
                        .withConnectionHeartbeat(5000, TimeUnit.MILLISECONDS)
                        .withProxy(Proxy.Type.HTTP, proxyAddress, port)
                        .build();

        } else {
            slackSession = SlackSessionFactory
                    .createWebSocketSlackSession(SLACK_API_KEY);
        }

        BotRepository botRepository = new DefaultBotRepository();
        new MessageHandler(botRepository, slackSession);
        slackSession.connect();
    }



}