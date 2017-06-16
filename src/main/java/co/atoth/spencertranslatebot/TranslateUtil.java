package co.atoth.spencertranslatebot;

import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;

public class TranslateUtil {

    private static final DecimalFormat df = new DecimalFormat("#");
    private static final Logger logger = LoggerFactory.getLogger(TranslateUtil.class);

    public enum Lang {
        IW("il"), RO("ro");

        public String country;
        private Lang(String country){
            this.country = country;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public static final Lang[] translatedLanguages = new Lang[]{Lang.IW, Lang.RO};
    public static final String TRANSLATE_TO_LANG = "en";

    public static String translateIfNeeded(String message, int minimumConfidence){

        Translate translate = createTranslateService();

        //Check romanian or hebrew
        List<Detection> detections = translate.detect(ImmutableList.of(message));

        logger.debug("Detecting lang for message: " + message);
        for (Detection detection : detections) {
            String lang = detection.getLanguage();
            float conf = detection.getConfidence() * 100;
            String confStr = df.format(conf) + "%";

            logger.debug("Detected lang " + lang + " with confidence " + conf);
            for(Lang supportedLang : translatedLanguages){
                if(supportedLang.toString().equals(lang) && conf >= minimumConfidence) {
                    String translation = translateText(message, supportedLang.toString(), TRANSLATE_TO_LANG);
                    translation = Jsoup.parse(translation).text();
                    return "*" + getFlagForLang(supportedLang.country) + " " + confStr + "*\n" + translation;
                }
            }
        }

        return null;
    }

    public static String getFlagForLang(String lang){
        return ":flag-"+lang+":";
    }

    public static String translateText(
            String sourceText,
            String sourceLang,
            String targetLang){

        Translate translate = createTranslateService();

        TranslateOption srcLang = TranslateOption.sourceLanguage(sourceLang);
        TranslateOption tgtLang = TranslateOption.targetLanguage(targetLang);

        Translation translation = translate.translate(sourceText, srcLang, tgtLang);

        return translation.getTranslatedText();
    }

    public static Translate createTranslateService() {
        return TranslateOptions
                .newBuilder()
                .setApiKey(SpencerTranslateBot.GOOGLE_API_KEY)
                .build()
                .getService();
    }

}