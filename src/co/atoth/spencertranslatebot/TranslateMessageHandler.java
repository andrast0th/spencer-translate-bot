package co.atoth.spencertranslatebot;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

import java.text.DecimalFormat;

/**
 * Samples showing how to listen to message events
 */
public class TranslateMessageHandler {

    private static final DecimalFormat df = new DecimalFormat("#.00");

    private static String helpMsg = getHelpMsg();
    private static boolean isOn = true;
    private static int minCert = 30;

    public static void onEvent(String content, SlackChannel channel, SlackUser sender, SlackSession session){
        // if I'm only interested on a certain channel :
        // I can filter out messages coming from other channels
        SlackChannel theChannel = session.findChannelByName(SpencerTranslateBot.CHANNEL_NAME);
        if (!theChannel.getId().equals(channel.getId())) {
            return;
        }

        // How to avoid message the bot send (yes it is receiving notification for its own messages)
        // session.sessionPersona() returns the user this session represents
        if (sender != null && session.sessionPersona().getId().equals(sender.getId())) {
            return;
        }

        String messageContent = content;

        String returnMessage = parseCommands(messageContent, session.sessionPersona().getId());
        if(returnMessage != null){

            session.sendMessage(channel, returnMessage);
        } else if(isOn && sender != null){

            //Check if romanian
            float cert = TranslateUtil.isRomanian(messageContent, minCert / 100);
            if(cert > 0){
                String reply = "Detected RO ("+df.format(cert*100)+"%)";
                reply += "\n*" +  sender.getUserName() +":* "+ TranslateUtil.translateRomanian(messageContent);
                session.sendMessage(channel, reply);
            } else {
                System.out.println("NOT RO: " + messageContent);
            }
        }

    }

    private static String parseCommands(String message, String botId){
        String target = "<@" + botId + ">";
        if(message.contains(target)){
            message = message.replace(target, "").trim();
            String[] cmds = message.split("\\s+");
            try {
                if(parseOnOffStatus(cmds) != null){
                    return parseOnOffStatus(cmds);
                }
                if(parseHelp(cmds) != null){
                    return parseHelp(cmds);
                }
                if(parseMinCert(cmds) != null){
                    return parseMinCert(cmds);
                }
            } catch (Exception ex) {
                //Go away
            }
        }
        return null;
    }

    private static String parseMinCert(String[] cmds){
        if(cmds.length == 2){
            if("setMinCert".equals(cmds[0])){
                int newMinCert = Integer.parseInt(cmds[1]);
                if(newMinCert >= 0 && newMinCert <= 100){
                    minCert = newMinCert;
                    return "*setMinCert*\nNew minimum certainty " + minCert;
                }
            }
        }
        return null;
    }

    private static String parseHelp(String[] cmds){
        if(cmds.length == 1) {
            if ("help".equals(cmds[0])) {
                return "*help*\n" + helpMsg;
            }
        }
        return null;
    }

    private static String parseOnOffStatus(String[] cmds){
        if(cmds.length == 1){
            if("on".equals(cmds[0])){
               isOn = true;
                return ">*on*";
            }
            else if("off".equals(cmds[0])){
                isOn = false;
                return ">*off*";
            }
            else if("status".equals(cmds[0])){
                return ">*status*\nactive: " + isOn + ", minimum certainty: " + minCert + "%";
            }
        }
        return null;
    }

    private static String getHelpMsg(){
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append(">status, \n");
        helpMsg.append(">on, \n");
        helpMsg.append(">off, \n");
        helpMsg.append(">setMinCert (INT range (0,100) \n");
        return helpMsg.toString();
    }
}