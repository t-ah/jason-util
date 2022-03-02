package jason.env.blocks;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class BlocksWorld extends Environment {

    private BlocksModel model;

    private int numOfRobots = 1;
    private int numOfCommonRooms = 5;
    private int packagingProbability = 50;

    @Override
    public void init(String[] args) {
        if (args.length > 0) {
            try {
                var json = new JSONObject(Files.readString(Path.of(args[0])));
                numOfRobots = json.getInt("robots");
                numOfCommonRooms = json.getInt("rooms");
                packagingProbability = json.getInt("packaging");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("No environment config specified.");
        }
        model = new BlocksModel(numOfRobots, numOfCommonRooms, packagingProbability);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        if (!model.isRegistered(agent))
            model.registerAgent(agent);

        System.out.printf("Action %s received from agent %s.\n", action.toString(), agent);

        boolean result = switch (action.getFunctor()) {
            case "putDown" -> model.actPutDown(agent);
            case "pickUp" -> model.actPickUp(agent);
            case "gotoBlock" -> model.actGotoBlock(agent, action.getTerms());
            case "goto" -> model.actGoto(agent, action.getTerms());
            case "activate" -> model.actActivate(agent);
            default -> false;
        };
        this.actionExecuted();
        return result;
    }

    void updatePercepts() {
        clearAllPercepts();
        addPercept(makePercept("world", "blocks"));
        var task = model.getCurrentTask();
        if (task != null) {
            addPercept(makePercept("task", task.id(), task.color()));
            if (task.packaging())
                addPercept(makePercept("packaging"));
        }
        for (Room room : model.getRooms()) {
            addPercept(makePercept("place", room.getName()));
        }
        for (Robot robot : model.getRobots()) {
            if (robot.atBlock() != null)
                addPercept(robot.getAgent(), makePercept("atBlock", robot.atBlock().id));
            if (robot.isHolding()) {
                var heldBlock = robot.getBlock();
                addPercept(robot.getAgent(), makePercept("colour", heldBlock.id, heldBlock.colour));
                addPercept(robot.getAgent(), makePercept("holding", heldBlock.id));
            }
            if (robot.getRoom() != null) {
                addPercept(robot.getAgent(), makePercept("at", robot.getRoom().getName()));
                for (Block block : robot.getRoom().getBlocks())
                    addPercept(robot.getAgent(), makePercept("colour", block.id, block.colour));
            }
        }
    }

    private static Literal makePercept(String functor) {
        return Literal.parseLiteral(functor);
    }

    private static Literal makePercept(String functor, String param) {
        return Literal.parseLiteral(String.format("%s(%s)", functor, param));
    }

    private static Literal makePercept(String functor, String param1, String param2) {
        return Literal.parseLiteral(String.format("%s(%s,%s)", functor, param1, param2));
    }

    private void actionExecuted() {
        model.generateBlocks();
        updatePercepts();
        try {
            Thread.sleep(100);
        } catch (Exception ignored) {}
        informAgsEnvironmentChanged();
    }

    private static Integer parseInt(String intString, Integer defaultValue) {
        if (intString == null) return null;
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}