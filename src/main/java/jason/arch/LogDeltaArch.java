package jason.arch;

import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.Literal;
import jason.asSyntax.PlanBody;
import jason.asSyntax.SourceInfo;
import jason.asSyntax.Trigger;
import jason.bb.BeliefBase;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogDeltaArch extends AgArch implements GoalListener {

    private boolean initialized = false;

    private Set<String> oldBeliefs = new HashSet<>();

    private final Set<IntendedMeans> ims = new HashSet<>();
    private final Queue<String> goalStatusQueue = new LinkedList<>();

    private FileWriter log;

    public void init() {
        getTS().addGoalListener(this);
        setupLogging();
    }

    private void setupLogging() {
        String userLogPath = getTS().getSettings().getUserParameter("debug-log-path");
        if (userLogPath == null) userLogPath = "./debug-log";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Date date = new Date();
        File logPath = new File(userLogPath + "/" + formatter.format(date) + "/");
        //noinspection ResultOfMethodCallIgnored
        logPath.mkdirs();
        File logFile  = new File(logPath.getAbsolutePath() + "/" + getAgName() + ".log");
        try {
            //noinspection ResultOfMethodCallIgnored
            logFile.createNewFile();
            log = new FileWriter(logFile);
            log.append("{platform: \"jason\"}").append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        if (!initialized) {
            initialized = true;
            JSONObject json = new JSONObject();
            handleBeliefs(json, getBeliefBase());
        }
    }

    @Override
    public void reasoningCycleStarting() {
        super.reasoningCycleStarting();
        initialize();

        System.out.println("Cycle " + getCycleNumber());
    }

    @Override
    public void reasoningCycleFinished() {
        super.reasoningCycleFinished();

        TransitionSystem    ts = getTS();
        Circumstance        c = ts.getC();
        Iterator<Intention> intentions = c.getAllIntentions();
        BeliefBase          bb = getBeliefBase();
        Intention           selectedIntention = c.getSelectedIntention();

        JSONObject json = new JSONObject();

        boolean changes = handleIntentions(json, selectedIntention, intentions);
        changes |= handleBeliefs(json, bb);

        if (changes) {
            logChanges(getCycleNumber(), json);
        }

        //        if (c.getSelectedEvent() != null) System.out.println("Event: " + c.getSelectedEvent().getTrigger());
        //        if ( c.getRelevantPlans() != null ) System.out.println("Relevant plans: " + c.getRelevantPlans().size());
    }

    private void logChanges(int cycle, JSONObject json) {
        try {
            log.append(String.valueOf(cycle)).append(" ").append(json.toString(0)).append("\n");
            log.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BeliefBase getBeliefBase() {
        return getTS().getAg().getBB();
    }

    private boolean handleBeliefs(JSONObject json, BeliefBase bb) {
        Set<String> currentBeliefs = new HashSet<>();
        for (Literal belief : bb) currentBeliefs.add(belief.toString());

        Set<String> addedBeliefs = new HashSet<>(currentBeliefs);
        addedBeliefs.removeAll(oldBeliefs);
        Set<String> removedBeliefs = new HashSet<>(oldBeliefs);
        removedBeliefs.removeAll(currentBeliefs);
        oldBeliefs = currentBeliefs;

        if (!addedBeliefs.isEmpty()) json.put("B+", addedBeliefs);
        if (!removedBeliefs.isEmpty()) json.put("B-", removedBeliefs);

        return !addedBeliefs.isEmpty() || !removedBeliefs.isEmpty();
    }

    private boolean handleIntentions(JSONObject json, Intention selectedIntention, Iterator<Intention> intentions) {
        if (selectedIntention == null) return false;

        json.put("SI", selectedIntention.getId());

        IntendedMeans intent = selectedIntention.peek();
        if (intent != null) {
            PlanBody instruction = selectedIntention.peek().getCurrentStep().getHead();
            json.put("In", instruction.getSrcInfo().getSrcLine() + ": " + instruction);
        }

        List<String> imsAdded = new ArrayList<>();
        List<String> imsRemoved = new ArrayList<>();

        while (intentions.hasNext()) {
            Intention intention = intentions.next();
            for (IntendedMeans im : intention) {
                if (ims.add(im)) {
                    SourceInfo src = im.getCurrentStep().getSrcInfo();
                    imsAdded.add(src.getSrcFile() + ": " + src.getSrcLine());
                }
            }
        }
        for (IntendedMeans im : new HashSet<>(ims)) {
            if (im.isFinished()) {
                ims.remove(im);
                imsRemoved.add(goalStatusQueue.poll() + ": " + im);
            }
        }

        if (!imsAdded.isEmpty()) json.put("IM+", imsAdded);
        if (!imsRemoved.isEmpty()) json.put("IM-", imsRemoved);

        return true;
    }

    /*
     * Called externally, part of GoalListener. Remembers the state for when the corresponding IMs are found later.
     */
    public void goalFinished(Trigger goal, GoalStates result) {
        goalStatusQueue.add(result != null ? result.toString() : "null");
    }

    /*
     * @FIXME for testing, remove later!
     */
    public void act(ActionExec action) {
        getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());
        action.setResult(true);
        actionExecuted(action);
    }
}
