package jason.env.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {

    private final String name;
    private final Map<String, Block> blocks = new HashMap<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean pickUp(String blockID) {
        return blocks.remove(blockID) != null;
    }

    public void putDown(Block block) {
        if (block == null) return;
        this.blocks.put(block.id, block);
    }

    public Block getBlock(String blockName) {
        return blocks.get(blockName);
    }

    public List<Block> getBlocks() {
        return new ArrayList<>(blocks.values());
    }
}
