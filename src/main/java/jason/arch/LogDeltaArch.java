package jason.arch;

import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LogDeltaArch extends AgArch implements GoalListener, CircumstanceListener {

    private boolean initialized = false;
    private FileWriter log;

    private Set<String> oldBeliefs = new HashSet<>();

    private final Set<Integer> knownIntentions = new HashSet<>();
    private final Set<Intention> unfinishedIntentions = new HashSet<>();
    private final Queue<String> goalStatusQueue = new LinkedList<>();

    private long imCounter = 1;
    private final Map<IntendedMeans, Long> ims = new HashMap<>();
    private final List<JSONObject> newIMs = new ArrayList<>();

    private final List<Event> newEvents = new ArrayList<>();


    @Override
    public void init() {
        getTS().addGoalListener(this);
        getTS().getC().addEventListener(this);
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
            System.out.println(srcFile.exists());
            try {
                Files.copy(srcFile.toPath(),
                        new File(logPath.getAbsolutePath() + "/src/" + agentSrc.substring(5)).toPath());
            } catch (IOException ignored) {}
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
        super.reasoningCycleStarting();
        initialize();

        // FIXME for debugging the debugger only, remove later
        System.out.println("Cycle " + getCycleNumber());
    }

    @Override
    public void reasoningCycleFinished() {
        super.reasoningCycleFinished();
        logState(getCycleNumber());
    }

    @Override
    public void intentionAdded(Intention i) {  // also called when new IMs are pushed
        var currentIMs = new ArrayList<IntendedMeans>();
        for (var im : i)
            currentIMs.add(im);
        var im = currentIMs.get(0);
        IntendedMeans parentIM = null;
        if (currentIMs.size() > 1) {
            parentIM = currentIMs.get(1);
        }
        if (!this.ims.containsKey(im)) {
            this.ims.put(im, imCounter);
            SourceInfo src = im.getPlan().getSrcInfo();
            JSONObject imData = new JSONObject();
            imData.put("i", i.getId());
            imData.put("id", imCounter);
            imData.put("file", src.getSrcFile());
            imData.put("line", src.getSrcLine());
            imData.put("plan", im.getPlan().getLabel());
            imData.put("trigger", im.getTrigger());
            String ctx = im.getPlan().getContext() == null? "T" :
                    im.getPlan().getContext().capply(im.getUnif()).toString();
            imData.put("ctx", ctx);
            if (parentIM != null) {
                imData.put("parent", ims.get(parentIM));
            }
            this.newIMs.add(imData);
            imCounter++;
        }
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

        boolean changes = handleIntentions(json, selectedIntention, currentIntentions);
        changes |= handleBeliefs(json, bb);
        changes |= handleEvents(json, selectedEvent);

        if (changes) {
            logChanges(cycle, json);
        }
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

    private boolean handleEvents(JSONObject json, Event selectedEvent) {
        if (selectedEvent != null) {
            json.put("SE", getEventIdentifier(selectedEvent, true));
        }
        boolean loggedEvents = false;
        if (!newEvents.isEmpty()) {
            json.put("E+", newEvents.stream().map(event -> getEventIdentifier(event, false))
                                             .collect(Collectors.toList()));
            newEvents.clear();
            loggedEvents = true;
        }
        return loggedEvents || selectedEvent != null;
    }

    private String getEventIdentifier(Event e, boolean useIntentionID) {
        var intention = e.getIntention();
        var id = "B";
        if (intention != null) {
            if (useIntentionID)
                id = String.valueOf(intention.getId());
            else
                id = String.valueOf(ims.get(intention.peek()));
        }
        return id + ": " + e.getTrigger();
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

    private boolean handleIntentions(JSONObject json, Intention selectedIntention, Set<Intention> currentIntentions) {
        boolean changes = false;

        for (Intention i: new ArrayList<>(unfinishedIntentions)) {
            if (!currentIntentions.contains(i) && !i.isFinished()) { // if a plan fails but the trigger was a belief and not a goal
                json.put("UI", i.getId());
                unfinishedIntentions.remove(i);
                changes = true;
            }
        }

        if (selectedIntention == null)
            return changes;
        json.put("SI", selectedIntention.getId());

        IntendedMeans intent = selectedIntention.peek();
        if (intent != null) {
            PlanBody instruction = selectedIntention.peek().getCurrentStep().getHead();
            JSONObject intentionData = new JSONObject();
            intentionData.put("file", instruction.getSrcInfo().getSrcFile());
            intentionData.put("line", instruction.getSrcInfo().getSrcLine());
            intentionData.put("instr", instruction);
            intentionData.put("im", ims.get(intent));
            json.put("I", intentionData);
        }
        else {
            json.put("I-", selectedIntention.getId());
        }

        List<JSONObject> imsRemoved = new ArrayList<>();

        for (var intention: currentIntentions) {
            if (knownIntentions.add(intention.getId())) {
                unfinishedIntentions.add(intention);
                json.put("I+", intention.getId());
            }
        }

        for (IntendedMeans im : new HashSet<>(ims.keySet())) {
            if (im.isFinished()) {
                long id = ims.remove(im);
                JSONObject removedIM = new JSONObject();
                removedIM.put("id", id);
                removedIM.put("res", goalStatusQueue.poll());
                // TODO maybe also add unifier
                imsRemoved.add(removedIM);
            }
        }

        if (!newIMs.isEmpty()) {
            json.put("IM+", newIMs);
            newIMs.clear();
        }
        if (!imsRemoved.isEmpty())
            json.put("IM-", imsRemoved);

        return true;
    }

    /*
     * Called externally, part of GoalListener. Remembers the state for when the corresponding IMs are found later.
     */
    @Override
    public void goalFinished(Trigger goal, GoalStates result) {
        goalStatusQueue.add(result != null ? result.toString() : "null");
//        System.out.println("Goal finished: " + goal + " " + result);
    }

    public void goalFailed(Trigger goal, Term reason) {
        System.out.println("Goal failed: " + goal + " " + reason);
    }

    public void eventAdded(Event e) {
        newEvents.add(e);
    }

//    @Override
//    public void act(ActionExec action) {
//        getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());
//        super.act(action);
//    }
}