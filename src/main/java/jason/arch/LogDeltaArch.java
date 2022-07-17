package jason.arch;

import jason.arch.helpers.Failure;
import jason.arch.helpers.IntentionSelectedListener;
import jason.arch.helpers.LogHandler;
import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.Literal;
import jason.asSyntax.Plan;
import jason.asSyntax.SourceInfo;
import jason.asSyntax.Trigger;
import jason.bb.BeliefBase;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Incrementally logs an agent's state.
 * (There must be easier ways to solve some of these issues...)
 */
public class LogDeltaArch extends AgArch implements GoalListener, CircumstanceListener, IntentionSelectedListener {

    private final LogHandler agentLogHandler = new LogHandler(this);
    private boolean initialized = false;
    private FileWriter log;

    private Set<String> oldBeliefs = new HashSet<>();

    private final Set<Integer> knownIntentions = new HashSet<>();
    private final Set<Intention> unfinishedIntentions = new HashSet<>();

    private int imCounter = 1;
    private int eventCounter = 1;
    private final Map<IntendedMeans, Integer> ims = new HashMap<>();
    private final Map<Integer, IntendedMeans> idToIM = new HashMap<>();
    private final List<JSONObject> newIMs = new ArrayList<>();
    private final Map<Trigger, Integer> triggerToIMID = new HashMap<>();

    private final List<Event> newEvents = new ArrayList<>();
    private final Map<Event, Integer> unhandledEvents = new HashMap<>();

    private final List<JSONObject> imResults = new ArrayList<>();
    private JSONObject imResultFailedRoot = null;

    private JSONObject latestInstruction = null;


    @Override
    public void init() {
        getTS().addGoalListener(this);
        getTS().getC().addEventListener(this);
        getTS().getLogger().addHandler(this.agentLogHandler);
        getTS().getLogger().setLevel(Level.FINE);
        setupLogging();
    }

    private void setupLogging() {
        String agentSrc = getTS().getAg().getASLSrc();
        String userLogPath = getTS().getSettings().getUserParameter("debug-log-path");
        if (userLogPath == null) userLogPath = "./debug-log";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Date date = new Date();
        File logPath = new File(userLogPath + "/" + formatter.format(date) + "/");
        File directories = new File(logPath.getAbsolutePath() + "/src");
        //noinspection ResultOfMethodCallIgnored
        directories.mkdirs();
        File logFile  = new File(logPath.getAbsolutePath() + "/" + getAgName() + ".log");

        JSONObject info = new JSONObject();
        info.put("entity", "agent");
        info.put("platform", "Jason");
        info.put("name", getAgName());
        info.put("src", agentSrc);

        JSONObject details = new JSONObject();
        info.put("details", details);
        JSONObject plans = new JSONObject();
        details.put("plans", plans);
        for (Plan planData : getTS().getAg().getPL()) {
            JSONObject plan = new JSONObject();
            plan.put("trigger", planData.getTrigger());
            plan.put("ctx", planData.getContext());
            plan.put("body", planData.getBody());
            plan.put("file", planData.getSrcInfo().getSrcFile());
            plan.put("line", planData.getSrcInfo().getSrcLine());
            plans.put(planData.getLabel().toString(), plan);
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            logFile.createNewFile();
            log = new FileWriter(logFile);
            log.append(info.toString(0)).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (agentSrc.startsWith("file:")) {
            File srcFile = new File(agentSrc.substring(5));
            File targetFile = new File(logPath.getAbsolutePath() + "/src/" + agentSrc.substring(5));
            try {
                //noinspection ResultOfMethodCallIgnored
                targetFile.getParentFile().mkdirs();
                Files.copy(srcFile.toPath(), targetFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initialize() {
        if (!initialized) {
            initialized = true;
            logState(0);
        }
    }

    @Override
    public void reasoningCycleStarting() {
        System.out.println("Cycle " + getCycleNumber());
        initialize();
        this.imResults.clear();
        this.imResultFailedRoot = null;
        this.latestInstruction = null;
        super.reasoningCycleStarting();
    }

    @Override
    public void reasoningCycleFinished() {
        logState(getCycleNumber());
        super.reasoningCycleFinished();
    }

    @Override
    public void intentionAdded(Intention intention) {  // also called when new IMs are pushed
        var im = intention.peek();
        if (!this.ims.containsKey(im))
            newIntendedMeansAdded(intention);
    }



    private void newIntendedMeansAdded(@NotNull Intention intention) {
        var im = intention.peek();
        int imID = this.imCounter++;

        this.triggerToIMID.put(im.getTrigger(), imID);
        this.ims.put(im, imID);
        this.idToIM.put(imID, im);
        SourceInfo src = im.getPlan().getSrcInfo();

        JSONObject imData = new JSONObject()
                .put("i", intention.getId())
                .put("id", imID)
                .put("file", src.getSrcFile())
                .put("line", src.getSrcLine())
                .put("plan", im.getPlan().getLabel())
                .put("trigger", im.getTrigger());
        if (im.getPlan().getContext() != null)
            imData.put("ctx", im.getPlan().getContext().capply(im.getUnif()).toString());
        this.newIMs.add(imData);
    }

    private void logState(int cycle) {
        TransitionSystem    ts = getTS();
        Circumstance        c = ts.getC();
        Iterator<Intention> intentions = c.getAllIntentions();
        BeliefBase          bb = getBeliefBase();
        Intention           selectedIntention = c.getSelectedIntention();
        Event               selectedEvent = c.getSelectedEvent();

        JSONObject json = new JSONObject();
        var currentIntentions = new HashSet<Intention>();
        intentions.forEachRemaining(currentIntentions::add);

        handleIntentions(json, selectedIntention, currentIntentions);
        handleBeliefs(json, bb);
        handleEvents(json, selectedEvent);

        if (!json.keySet().isEmpty())
            logChanges(cycle, json);
    }

    private void logChanges(int cycle, JSONObject json) {
        json.put("nr", cycle);
        try {
            log.append(json.toString(0)).append("\n");
            log.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BeliefBase getBeliefBase() {
        return getTS().getAg().getBB();
    }

    private void handleEvents(JSONObject json, Event selectedEvent) {
        if (!newEvents.isEmpty()) {
            json.put("E+", newEvents.stream().map(this::eventToJSON)
                                             .collect(Collectors.toList()));
            newEvents.clear();
        }
        if (selectedEvent != null) {
            var eventID = unhandledEvents.remove(selectedEvent);
            json.put("SE", eventID);
        }
    }

    private JSONObject eventToJSON(Event e) {
        var intention = e.getIntention();
        var trigger = e.getTrigger();
        var eventResult = new JSONObject()
                .put("t", trigger)
                .put("id", unhandledEvents.get(e));
        if (!trigger.isGoal())
            eventResult.put("src", "B");
        else if (intention == null)
            eventResult.put("nf", true);

        return eventResult;
    }

    private void handleBeliefs(JSONObject json, BeliefBase bb) {
        Set<String> currentBeliefs = new HashSet<>();
        for (Literal belief : bb) currentBeliefs.add(belief.toString());

        Set<String> addedBeliefs = new HashSet<>(currentBeliefs);
        addedBeliefs.removeAll(oldBeliefs);
        Set<String> removedBeliefs = new HashSet<>(oldBeliefs);
        removedBeliefs.removeAll(currentBeliefs);
        oldBeliefs = currentBeliefs;

        if (!addedBeliefs.isEmpty()) json.put("B+", addedBeliefs);
        if (!removedBeliefs.isEmpty()) json.put("B-", removedBeliefs);
    }

    private void handleIntentions(JSONObject json, Intention selectedIntention, Set<Intention> currentIntentions) {

        for (Intention i: new ArrayList<>(unfinishedIntentions)) {
            if (!currentIntentions.contains(i) && !i.isFinished()) { // if a plan fails but the trigger was a belief and not a goal
                json.put("UI", i.getId());
                unfinishedIntentions.remove(i);
            }
            else if (i.isFinished()) {
                json.put("I-", i.getId());
                unfinishedIntentions.remove(i);
            }
        }

        for (var intention: currentIntentions) {
            if (knownIntentions.add(intention.getId())) {
                unfinishedIntentions.add(intention);
                json.put("I+", intention.getId());
            }
        }

        if (!newIMs.isEmpty()) {
            json.put("IM+", newIMs);
            newIMs.clear();
        }

        handleSelectedIntention(selectedIntention, json);

        handlePotentialFailure();
        if (!imResults.isEmpty())
            json.put("IM-", imResults);
    }

    /**
     * Update failures with data from the Jason log.
     */
    private void handlePotentialFailure() {
        var failure = agentLogHandler.checkMessage();
        while (failure != null) {
            var failureDetails = Failure.parse(failure);
            if (Failure.PLAN_FAILURE_NO_RECOVERY.equals(failureDetails.getString("type"))) {
                if (imResultFailedRoot == null)
                    System.err.println("Failed goal for failure log not found.");
                else {
                    imResultFailedRoot.put("reason", failureDetails)
                                      .put("res", "failed");
                }
            } // other types implicitly handled for now
            failure = agentLogHandler.checkMessage();
        }
    }

    private void handleSelectedIntention(Intention selectedIntention, JSONObject json) {
        if (selectedIntention == null)
            return;
        int id = selectedIntention.getId();

        json.put("SI", id);

        if (latestInstruction != null) {
            json.put("I", latestInstruction);

            var imID = latestInstruction.optInt("im", -1);
            if (imID == -1)
                System.err.println("instruction has no means:: " + latestInstruction);
            else
                json.put("U", idToIM.get(imID).getUnif());
        }
        else
            System.out.println("No instruction for selected intention " + id);
    }

    /*
     * Called by Jason - before(!) the potential log message is written.
     * Most recent goals finish first, may lead to parent goals finishing (failing) as well.
     */
    @Override
    public void goalFinished(Trigger goal, GoalStates result) {
        int imID = triggerToIMID.getOrDefault(goal, -1);
//        System.out.println("Goal finished: " + goal + " " + imID + " " + result);
        String  actualResult;
        if (imID == -1)
            actualResult = Failure.NO_PLAN;
        else if (result == null)
            actualResult = Failure.UNKNOWN;
        else
            actualResult = result.toString();

        var imResult = new JSONObject().put("id", imID).put("res",  actualResult);
        imResults.add(imResult);
        if (actualResult.equals(Failure.UNKNOWN)) // reason will be supplied later
            imResultFailedRoot = imResult;
    }

//    @Override
//    public void goalFailed(Trigger goal, Term reason) {
//        System.out.println("Goal failed: " + goal + " " + reason);
//    }

    @Override
    public void eventAdded(Event e) {
        newEvents.add(e);
        unhandledEvents.put(e, eventCounter++);
    }

    /**
     * Read next instruction once intention has been selected and instruction not yet run (and removed).
     */
    @Override
    public void notifyIntentionSelected() {
        var SI = getTS().getC().getSelectedIntention();
        if (SI != null) {
            var im = SI.peek();
            if (im != null)
                this.latestInstruction = extractInstruction(im);
        }
    }

    /**
     * Extracts information from the first instruction of the given body.
     */
    private JSONObject extractInstruction(IntendedMeans im) {
        int imID = ims.get(im);
        var body = im.getCurrentStep();
        var src = body.getSrcInfo();
        if (src == null) return new JSONObject();
        return new JSONObject()
                .put("im", imID)
                .put("instr", body.getHead())
                .put("file", src.getSrcFile())
                .put("line", src.getSrcLine());
    }
}