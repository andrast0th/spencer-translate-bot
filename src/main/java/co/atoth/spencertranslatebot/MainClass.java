package co.atoth.spencertranslatebot;

import co.atoth.spencertranslatebot.repository.BotRepository;
import co.atoth.spencertranslatebot.repository.DefaultBotRepository;
import co.atoth.spencertranslatebot.repository.google.GoogleCloudDataStoreBotRepository;
import co.atoth.spencertranslatebot.translation.TranslationService;
import co.atoth.spencertranslatebot.util.BotInfo;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class MainClass {

    private static final Logger logger = LoggerFactory.getLogger(MainClass.class);

    private static MessageHandler messageHandler;

    public static void main(String[] args) throws IOException {

        final Options options = new Options();

        //API keys
        options.addOption(Option.builder()
                .argName("slackApiKey").longOpt("slackApiKey")
                .desc("API key for your Slack team")
                .hasArg()
                .required()
                .build());

        options.addOption(Option.builder()
                .argName("googleTranslateApiKey").longOpt("googleTranslateApiKey")
                .desc("API key for Google Translate API")
                .hasArg()
                .required()
                .build());

        //Cloud storage
        options.addOption(Option.builder()
                .argName("googleCloudProjectName").longOpt("googleCloudProjectName")
                .desc("Project name from cloud.google.com, needed for Google Cloud Storage API")
                .hasArg()
                .build());

        options.addOption(Option.builder()
                .argName("googleCloudServiceAccountJsonFile").longOpt("googleCloudServiceAccountJsonFile")
                .desc("JSON file containing the service account details, needed for Google Cloud Storage API")
                .hasArg()
                .build());


        options.addOption(Option.builder()
                .argName("alertUserName").longOpt("alertUserName")
                .desc("Alert this user when the bot has connected or discconeted to the Slack team")
                .hasArg()
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException exp) {
            logger.error("Parsing command line arguments failed: " + exp.getMessage());
            return;
        }

        String slackApiKey = commandLine.getOptionValue("slackApiKey");
        String googleTranslateApiKey = commandLine.getOptionValue("googleTranslateApiKey");

        logger.debug("slackApiKey: " + slackApiKey);
        logger.debug("googleTranslateApiKey: " + googleTranslateApiKey);
        logger.info("bot info: " + System.getProperty("line.separator") + BotInfo.getInfo(System.getProperty("line.separator")));

        SlackSession slackSession = initSlackSession(slackApiKey);

        slackSession.addSlackConnectedListener((event, session) -> {
            if(event.getConnectedPersona().getId().equals(session.sessionPersona().getId())){

                logger.info("Bot has connected to slack, botId: " + session.sessionPersona().getId());

                TranslationService translationService = new TranslationService(googleTranslateApiKey);

                BotRepository botRepository = initBotRepository(session, commandLine);
                if(botRepository == null){
                    logger.error("Failed to create bot repository, exiting");
                    System.exit(-1);
                }

                messageHandler = new MessageHandler(botRepository, translationService);

                logger.info("MessageHandler regitered, listening for messages...");
                session.addMessagePostedListener(messageHandler);

                if(commandLine.hasOption("alertUserName")){
                    alertUser(true, session, commandLine.getOptionValue("alertUserName"));
                }
            }
        });

        slackSession.addSlackDisconnectedListener((event, session) -> {
            if(event.getDisconnectedPersona().getId().equals(session.sessionPersona().getId())){
                logger.info("Bot has disconnected from slack, botId: " + session.sessionPersona().getId());
                logger.info("MessageHandler unregistered...");
                session.removeMessagePostedListener(messageHandler);

                if(commandLine.hasOption("alertUserName")){
                    alertUser(false, session, commandLine.getOptionValue("alertUserName"));
                }
            }
        });

        slackSession.connect();
    }

    private static void alertUser(boolean hasConencted, SlackSession session, String userName){
        SlackUser user = session.findUserByUserName(userName);

        if(user!=null){
            String message = "hasConencted: " + hasConencted + "\n" + BotInfo.getInfo("\n");
            session.sendMessageToUser(user, new SlackPreparedMessage.Builder().withMessage(message).build());
            logger.debug("Failed to send hasConnected " + hasConencted + " alert to userName " + userName);
        } else {
            logger.debug("Failed to send hasConnected " + hasConencted + " alert to userName " + userName);
        }
    }

    private static SlackSession initSlackSession(String slackApiKey){
        SlackSession slackSession;

        //Set proxy if needed
        if(System.getProperties().containsKey("http.proxyHost") && System.getProperties().containsKey("http.proxyPort")){
            String proxyAddress = System.getProperty("http.proxyHost");
            int port = Integer.parseInt(System.getProperty("http.proxyPort"));

            slackSession = SlackSessionFactory
                    .getSlackSessionBuilder(slackApiKey)
                    .withAutoreconnectOnDisconnection(true)
                    .withConnectionHeartbeat(5000, TimeUnit.MILLISECONDS)
                    .withProxy(Proxy.Type.HTTP, proxyAddress, port)
                    .build();

        } else {
            slackSession = SlackSessionFactory
                    .createWebSocketSlackSession(slackApiKey);
        }

        return slackSession;
    }

    private static BotRepository initBotRepository(SlackSession slackSession, CommandLine commandLine){
        BotRepository botRepository;

        if(commandLine.hasOption("googleCloudProjectName") && commandLine.hasOption("googleCloudServiceAccountJsonFile")){
            logger.info("Attempting to init Google Cloud Storage bot repository...");

            String filePath = commandLine.getOptionValue("googleCloudServiceAccountJsonFile");
            String googleCloudProjectName = commandLine.getOptionValue("googleCloudProjectName");

            try {
                botRepository = new GoogleCloudDataStoreBotRepository(
                        ServiceAccountCredentials.fromStream(new FileInputStream(filePath)),
                        googleCloudProjectName,
                        slackSession.getTeam().getId());

            } catch (IOException e) {
                logger.error("Failed to create Google Cloud Storage repository");
                return null;
            }
        } else {
            logger.info("Google Cloud Storage options missing, using in memory bot repository...");
            botRepository = new DefaultBotRepository();
        }

        return botRepository;
    }


}