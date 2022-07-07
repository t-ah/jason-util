package jason.arch.helpers;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class Failure {

    public static final String PLAN_FAILURE_NO_RECOVERY = "pf_nr";
    public static final String NO_PLAN = "np";
    public static final String UNKNOWN = "?";

    public enum FailureType {
        PLAN_FAILURE_NO_RECOVERY(Failure.PLAN_FAILURE_NO_RECOVERY),
        NO_PLAN(Failure.NO_PLAN), // no applicable or no relevant plan
        UNKNOWN(Failure.UNKNOWN);

        private final String name;

        FailureType(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    private final static List<String> planFailureKeys = List.of("error", "error_msg", "code_src", "code_line");
    private final static Map<String, Pattern> planFailurePatterns = new HashMap<>();

    static {
        for (String key : planFailureKeys)
            planFailurePatterns.put(key, Pattern.compile(key + "\\((.*?)\\)"));
    }

    /*
     * "No failure" : Plan failed, no recovery plan
     * "Found" (... a goal for which there is no applicable plan)
     */
    public static FailureType getErrorType(String input) {
        if (input.startsWith("No failure"))
            return FailureType.PLAN_FAILURE_NO_RECOVERY;
        if (input.startsWith("Found"))
            return FailureType.NO_PLAN;
        return FailureType.UNKNOWN;
    }

    public static JSONObject parse(String input) {

        var type = getErrorType(input);
        var result = new JSONObject().put("type", type.toString());

        if (type == FailureType.PLAN_FAILURE_NO_RECOVERY)
            planFailureKeys.forEach(k -> result.put(k, parseWithPattern(planFailurePatterns.get(k), input)));

        return result;
    }

    private static String parseWithPattern(Pattern pattern, String input) {
        var matcher = pattern.matcher(input);
        if (matcher.find())
            return matcher.group(1).replace("\"", "");
        return "";
    }
}
