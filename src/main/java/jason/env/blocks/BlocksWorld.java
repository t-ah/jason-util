package jason.env.blocks;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class BlocksWorld extends Environment {

    private BlocksModel model;

    @Override
    public void init(String[] args) {
        var params = new BlocksWorldParameters();
        if (args.length > 0) {
            try {
                var json = new JSONObject(Files.readString(Path.of(args[0])));
                params.numOfRobots = json.optInt("robots", params.numOfRobots);
                params.numOfCommonRooms = json.optInt("rooms", params.numOfCommonRooms);
                params.packagingProbability = json.optInt("packaging", params.packagingProbability);
                params.maxBlocks = json.optInt("maxBlocks", params.maxBlocks);
                params.startEnergy = json.optInt("startEnergy", params.startEnergy);
                params.rechargeEnergy = json.optInt("rechargeEnergy", params.rechargeEnergy);
                params.maxEnergy = json.optInt("maxEnergy", params.maxEnergy);
                params.energyCost = json.optInt("energyCost", params.energyCost);
                var confColours = json.optJSONArray("colours");
                if (confColours != null) {
                    params.colours = new ArrayList<>();
                    for (int i = 0; i < confColours.length(); i++)
                        params.colours.add(confColours.getString(i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("No environment config specified.");
        }
        model = new BlocksModel(params);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        if (!model.isRegistered(agent))
            model.registerAgent(agent);

        System.out.printf("Action %s received from agent %s.\n", action.toString(), agent);

        if (!List.of("recharge", "wait").contains(action.getFunctor())  && !model.consumeEnergy(agent)) {
            return false;
        }

        boolean result = switch (action.getFunctor()) {
            case "wait" -> true;
            case "putDown" -> model.actPutDown(agent);
            case "pickUp" -> model.actPickUp(agent);
            case "gotoBlock" -> model.actGotoBlock(agent, action.getTerms());
            case "goto" -> model.actGoto(agent, action.getTerms());
            case "activate" -> model.actActivate(agent);
            case "recharge" -> model.actRecharge(agent);
            default -> false;
        };
        this.actionExecuted();
        return result;
    }

    void updatePercepts() {
        clearAllPercepts();
        addPercept(makePercept("blocksworld"));
        var lastDeliveredTask = model.getLastDeliveredTask();
        if (lastDeliveredTask != null && !lastDeliveredTask.equals(""))
            addPercept(makePercept("delivered", lastDeliveredTask));
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
            addPercept(robot.getAgent(), makePercept("energy", robot.getEnergy()));
            if (robot.atBlock() != null)
                addPercept(robot.getAgent(), makePercept("atBlock", robot.atBlock().id));
            if (robot.isHolding()) {
                var heldBlock = robot.getBlock();
                addPercept(robot.getAgent(), makePercept("colour", heldBlock.id, heldBlock.colour));
                addPercept(robot.getAgent(), makePercept("holding", heldBlock.id));
                if (heldBlock.isPackaged())
                    addPercept(makePercept("packaged", heldBlock.id));

            }
            if (robot.getRoom() != null) {
                addPercept(robot.getAgent(), makePercept("at", robot.getRoom().getName()));
                for (Block block : robot.getRoom().getBlocks()) {
                    addPercept(robot.getAgent(), makePercept("colour", block.id, block.colour));
                    if (block.isPackaged())
                        addPercept(makePercept("packaged", block.id));
                }
            }
        }
    }

    private static Literal makePercept(String functor) {
        return Literal.parseLiteral(functor);
    }

    private static Literal makePercept(String functor, Object param) {
        return Literal.parseLiteral(String.format("%s(%s)", functor, param));
    }

    private static Literal makePercept(String functor, Object param1, Object param2) {
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
}