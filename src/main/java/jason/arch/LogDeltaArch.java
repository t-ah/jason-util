package jason.arch;

import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.Literal;
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
    private Map<Integer, String> oldIntentions = new HashMap<>();

    private final Set<IntendedMeans> ims = new HashSet<>();
    private int lastSelectedIntention = 0;
    private Queue<String> goalStatusQueue = new LinkedList<>();

    private final Map<Integer, String> instructionForIntention = new HashMap<>();

    private FileWriter log;

    public void init() throws Exception {
        getTS().addGoalListener(this);
        setupLogFiles();
    }

    private void setupLogFiles() {
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

        getTS().getC().getAllIntentions().forEachRemaining( intention -> {
            IntendedMeans im = intention.peek();
            if (im != null)
                instructionForIntention.put(intention.getId(), intention.peek().getCurrentStep().getHead().toString());
        });
    }

    @Override
    public void reasoningCycleFinished() {
        super.reasoningCycleFinished();

        TransitionSystem    ts = getTS();
        Circumstance        c = ts.getC();
        Iterator<Intention> intentions = c.getAllIntentions();
        BeliefBase          bb = getBeliefBase();
        Intention           selectedIntention = c.getSelectedIntention();
        Agent               ag = ts.getAg();
        String              aslFile = ag.getASLSrc();

        JSONObject json = new JSONObject();

        boolean changes = handleIntentions(json, selectedIntention, intentions);
        changes |= handleBeliefs(json, bb);

        if (changes) {
            json.put("cycle", getCycleNumber());
            logChanges(json);
        }

        //        if (c.getSelectedEvent() != null) System.out.println("Event: " + c.getSelectedEvent().getTrigger());
        //        if ( c.getRelevantPlans() != null ) System.out.println("Relevant plans: " + c.getRelevantPlans().size());

        /*
        TODO:
        - store int.means by intention (graph)
        - check if finished, and store result
        - where to get result?
        - also options
        - store instructions before cycle for each intention, read instruction from storage after depending on which intention selected
           - how to determine failure?
        - getSrcInfo
        - link deltas to intendedmeans, which is linked to intention
         */
    }

    private void logChanges(JSONObject json) {
        try {
            log.append(json.toString(0) + "\n");
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
        boolean changes = false;
        if (selectedIntention != null) {
            changes = true;
            json.put("SI", selectedIntention.getId());
            lastSelectedIntention = selectedIntention.getId();

            IntendedMeans intent = selectedIntention.peek();
            if (intent != null) {
                System.out.println("--- INSTRUCTION: " + instructionForIntention.get(selectedIntention.getId()));
                System.out.println("Selected: " + selectedIntention.getId());
                // TODO log instruction
            }
        }
        else {
            lastSelectedIntention = 0;
        }

        intentions.forEachRemaining(intention -> {
            for (IntendedMeans im : intention) {
                if (ims.add(im)) {
                    // TODO log start of IM with intention ID
                    System.out.println("New IM " + im.getTrigger() + " for intention " + intention.getId());
                }
            }
        });
        for (IntendedMeans im : new HashSet<>(ims)) {
            if (im.isFinished()) {
                ims.remove(im);
                //TODO log end of IM
                System.out.println("IM ended " + im.getTrigger());
            }
        }

        // TODO result to json object
        return changes;
    }

    /*
     * Called externally, part of GoalListener. Remembers the state for when the corresponding IMs are found later.
     */
    public void goalFinished(Trigger goal, GoalStates result) {
        goalStatusQueue.add(result.toString());
    }
}
