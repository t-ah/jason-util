package jason.env.blocks;

public class Robot {

    private final int id;
    private String agent = null;

    private Block heldBlock = null;
    private Block atBlock = null;
    private Room room;

    public Robot(int id, Room room){
        this.id = id;
        this.room = room;
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
}