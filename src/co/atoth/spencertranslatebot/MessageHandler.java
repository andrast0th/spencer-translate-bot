package co.atoth.spencertranslatebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpencerTranslateBot.class);

    private static String helpMsg = getHelpMsg();
    private static boolean isOn = true;
    private static int minCert = 30;

    public String getTranslation(String sender, String message){
        if(isOn){

            String newMessage = TranslateUtil.translateIfNeeded(message, minCert);

            if(!newMessage.equals(message)){
                return "*" +  sender +":* "+ newMessage;
            }
        }
        return null;
    }

    public String parseCommands(String botUserId, String message){
        String target = "<@" + botUserId + ">";
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