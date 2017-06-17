package co.atoth.spencertranslatebot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class BotInfo {

    private static final Logger logger = LoggerFactory.getLogger(BotInfo.class);

    public static String getInfo(String delimiter){
        Map<String, String> buildInfoMap = BotInfo.getAttributesFromManifest();

        String buildInfo = buildInfoMap
                .keySet()
                .stream()
                .map(key -> key +": "+buildInfoMap.get(key))
                .collect(Collectors.joining(delimiter));

        if(buildInfo != null && buildInfo.trim().length() != 0){
            buildInfo += delimiter;
        }
        buildInfo += "Host: " + BotInfo.getHostName();

        return buildInfo;
    }

    public static Map<String, String> getAttributesFromManifest(){

        Map<String, String> result = new LinkedHashMap<>();

        Class clazz = BotInfo.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();

        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest = null;
            try {
                manifest = new Manifest(new URL(manifestPath).openStream());
                Attributes attr = manifest.getMainAttributes();
                attr.forEach((key, value) -> result.put(key.toString(), value.toString()));

            } catch (IOException e) {
                logger.error("Error while reading jar manifest: ", e);
            }
        }
        return result;
    }


    public static String getHostName(){
        String hostname;
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        }
        catch (UnknownHostException ex){
            hostname = "unknown";
        }
        return hostname;
    }

}
