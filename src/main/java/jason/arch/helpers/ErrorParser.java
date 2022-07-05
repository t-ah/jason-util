package jason.arch.helpers;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class ErrorParser {

    private final static List<String> keys = List.of("error", "error_msg", "code_src", "code_line");
    private final static Map<String, Pattern> patterns = new HashMap<>();

    static {
        for (String key : keys)
            patterns.put(key, Pattern.compile(key + "\\((.*?)\\)"));
    }


    public static JSONObject parseError(String input) {
        var result = new JSONObject();

        for (String key : keys) {
            result.put(key, parse(patterns.get(key), input));
        }
        return result;
    }

    private static String parse(Pattern pattern, String input) {
        var matcher = pattern.matcher(input);
        if (matcher.find())
            return matcher.group(1).replace("\"", "");
        return "";
    }
}
