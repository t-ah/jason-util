package jason.env.blocks;

public class Robot {

    private final int id;
    private String agent = null;

    private static int MAX_ENERGY = 100;
    private static int RECHARGE_ENERGY = 10;
    private int energy;

    private Block heldBlock = null;
    private Block atBlock = null;
    private Room room;

    public Robot(int id, Room room, int energy){
        this.id = id;
        this.room = room;
        this.energy = energy;
    }

    public static void setMaxEnergy(int maxEnergy) {
        Robot.MAX_ENERGY = maxEnergy;
    }

    public static void setRechargeEnergy(int rechargeEnergy) {
        Robot.RECHARGE_ENERGY = rechargeEnergy;
    }

    public int getId() {
        return this.id;
    }

    public void registerAgent(String agentName) {
        if (this.agent == null)
            this.agent = agentName;
    }

    public String getAgent() {
        return this.agent;
    }

    public boolean isFree() {
        return this.agent == null;
    }

    public boolean isHolding() {
        return this.heldBlock != null;
    }

    public Block getBlock() {
        return this.heldBlock;
    }

    public Block putDown() {
        var result = this.heldBlock;
        if (result != null)
            result.putDown();
        this.heldBlock = null;
        return result;
    }

    /**
     * @return the block that was picked up (or null if nothing happened)
     */
    public Block pickUp() {
        if (heldBlock != null)
            return null;
        if (atBlock != null) {
            if (atBlock.isPickedUp())
                return null;
            if (!this.room.pickUp(atBlock.id))
                    return null;
            heldBlock = atBlock;
            heldBlock.pickUp();
            atBlock = null;
        }
        return heldBlock;
    }

    public void gotoBlock(Block block) {
        this.atBlock = block;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
        this.atBlock = null;
    }

    public Block atBlock() {
        return this.atBlock;
    }

    public void recharge() {
        this.energy = Math.min(this.energy + RECHARGE_ENERGY, MAX_ENERGY);
    }

    public boolean consumeEnergy(int energy) {
        if (energy > this.energy)
            return false;
        this.energy -= energy;
        return true;
    }

    public int getEnergy() {
        return energy;
    }
}