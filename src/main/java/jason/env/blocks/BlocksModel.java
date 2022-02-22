package jason.env.blocks;

import jason.asSyntax.Term;

import java.util.*;

public class BlocksModel {

    public final static String DROPZONE = "dropzone";
    public final static String CORRIDOR = "corridor";

    private final Random RNG = new Random(17);

    private int totalBlockCount = 0;
    private int currentBlockCount = 0;
    private final int maxBlockCount = 32;
    private final static String[] colours = new String[]{"green", "blue", "yellow", "red", "purple"};
    private String nextColour = getRandomColour();

    private final Map<String, Room> rooms = new HashMap<>();
    private final List<Room> spawnRooms = new ArrayList<>();
    private final Room corridor;

    private final Map<Integer, Robot> robots = new HashMap<>();
    private final Map<String, Robot> agentToRobot = new HashMap<>();


    public BlocksModel(int numberOfRobots, int numberOfCommonRooms) {
        for (int i = 0; i < numberOfRobots; i++)
            createRobot();
        for (int i = 0; i < numberOfCommonRooms; i++) {
            var room = createRoom(null);
            spawnRooms.add(room);
            generateBlock(room);
        }
        createRoom(DROPZONE);
        corridor = createRoom(CORRIDOR);
        determineNewColour();
    }

    public String getNextColour() {
        return this.nextColour;
    }

    private Robot createRobot() {
        var id = this.robots.size();
        var robot = new Robot(id, corridor);
        this.robots.put(id, robot);
        return robot;
    }

    private Room createRoom(String name) {
        if (name == null)
            name = String.format("room%d", this.rooms.size());
        var room = new Room(name);
        this.rooms.put(name, room);
        return room;
    }

    public void generateBlock(String colour) {
        var room = spawnRooms.get(RNG.nextInt(spawnRooms.size()));
        generateBlock(room, colour);
    }

    public void generateBlock(Room room) {
        var colour = getRandomColour();
        generateBlock(room, colour);
    }

    public void generateBlock(Room room, String colour) {
        currentBlockCount++;
        var block = new Block(String.format("block%d", this.totalBlockCount++), colour);
        room.putDown(block);
    }

    public void registerAgent(String agentName) {
        var optionalRobot = this.robots.values().stream().filter(Robot::isFree).findAny();
        Robot robot = null;
        if (optionalRobot.isEmpty())
            robot = createRobot();
        else
            robot = optionalRobot.get();
        registerAgent(agentName, robot.getId());
    }

    private void registerAgent(String agentName, int robotNo) {
        if (this.isRegistered(agentName)) return;
        var robot = this.robots.get(robotNo);
        if (robot == null || robot.getAgent() != null) return;
        robot.registerAgent(agentName);
        this.agentToRobot.put(agentName, robot);
    }

    public boolean isRegistered(String agentName) {
        return this.agentToRobot.containsKey(agentName);
    }

    private String getRandomColour() {
        return colours[RNG.nextInt(colours.length)];
    }

    private void determineNewColour() {
        this.nextColour = getRandomColour();
        generateBlock(this.nextColour);
        System.out.printf("The new colour is %s.\n", this.nextColour);
    }

    public boolean actPutDown(String agent) {
        var robot = agentToRobot.get(agent);
        var block = robot.putDown();
        if (block == null)
            return false;
        if (DROPZONE.equals(robot.getRoom().getName())) {
            if (nextColour.equals(block.colour)) {
                System.out.printf("Delivered block of colour %s\n", block.colour);
                determineNewColour();
            }
            currentBlockCount--;
        }
        else
            robot.getRoom().putDown(block);
        return true;
    }

    public boolean actPickUp(String agent) {
        var robot = agentToRobot.get(agent);
        return robot.pickUp() != null;
    }

    public boolean actGotoBlock(String agent, List<Term> terms) {
        if (terms == null || terms.size() != 1)
            return false;
        var robot = agentToRobot.get(agent);
        var blockName = terms.get(0).toString();
        var block = robot.getRoom().getBlock(blockName);
        if (block == null)
            return false;
        robot.gotoBlock(block);
        return true;
    }

    public boolean actGoto(String agent, List<Term> terms) {
        if (terms == null || terms.size() != 1)
            return false;
        var robot = agentToRobot.get(agent);
        var roomName = terms.get(0).toString();
        var room = rooms.get(roomName);
        if (room == null)
            return false;
        robot.setRoom(room);
        return true;
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms.values());
    }

    public List<Robot> getRobots() {
        return new ArrayList<>(robots.values());
    }

    public void generateBlocks() {
        if (currentBlockCount < maxBlockCount) {
            generateBlock(spawnRooms.get(RNG.nextInt(spawnRooms.size())));
        }
    }
}