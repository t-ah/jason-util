package jason.arch;

import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.runtime.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogDeltaArch extends AgArch {
    public static void main(String[] a) {
        LogDeltaArch ag = new LogDeltaArch();
        ag.run();
    }

    public LogDeltaArch() {
        super();
    }

    @Override
    public void reasoningCycleStarting() {
        super.reasoningCycleStarting();
        // TODO change here
        System.out.println("Enter cycle");
    }

    @Override
    public void reasoningCycleFinished() {
        super.reasoningCycleFinished();
        // TODO changes here
        System.out.println("Exit cycle");
    }

    public void run() {
        while (isRunning()) {
            // calls the Jason engine to perform one reasoning cycle
            getTS().reasoningCycle();
        }
    }

    // this method just add some perception for the agent
    public List<Literal> perceive() {
        super.perceive();
        return Collections.EMPTY_LIST;
    }

    // this method gets the agent actions
    public void act(ActionExec action) {
        getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());
        // return confirming the action execution was OK
        action.setResult(true);
        actionExecuted(action);
    }

    public boolean canSleep() {
        return true;
    }

    public boolean isRunning() {
        return true;
    }

    public void sleep() {
        try {   Thread.sleep(1000); } catch (InterruptedException e) {}
    }

    public void sendMsg(jason.asSemantics.Message m) throws Exception {
        super.sendMsg(m);
    }

    public void broadcast(jason.asSemantics.Message m) throws Exception {
        super.broadcast(m);
    }

    public void checkMail() {
        super.checkMail();
    }
}
