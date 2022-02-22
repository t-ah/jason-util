package jason.env.blocks;

import jason.asSyntax.*;
import jason.environment.Environment;


public class BlocksWorld extends Environment {

    private BlocksModel model;

    @Override
    public void init(String[] args) {
        int numOfRobots = 1;
        int numOfCommonRooms = 5;
        if (args.length > 0)
            numOfRobots = parseInt(args[0], numOfRobots);
        if (args.length > 1)
            numOfCommonRooms = parseInt(args[1], numOfCommonRooms);
        model = new BlocksModel(numOfRobots, numOfCommonRooms);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        if (!model.isRegistered(agent))
            model.registerAgent(agent);

        System.out.printf("Action %s received from agent %s.\n", action.toString(), agent);

        boolean result = false;
        switch (action.getFunctor()) {
            case "putDown" -> result = model.actPutDown(agent);
            case "pickUp" -> result = model.actPickUp(agent);
            case "gotoBlock" -> result = model.actGotoBlock(agent, action.getTerms());
            case "goto" -> result = model.actGoto(agent, action.getTerms());
        }
        this.actionExecuted();
        return result;
    }

    void updatePercepts() {
        clearAllPercepts();
        addPercept(makePercept("world", "blocks"));
        addPercept(makePercept("nextColour", model.getNextColour()));
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