package jason.env.blocks;

public class Block {

    public final String id;
    public final String colour;

    private boolean pickedUp = false;

    public Block(String id, String color) {
        this.id = id;
        this.colour = color;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public void pickUp() {
        pickedUp = true;
    }

    public void putDown() {
        pickedUp = false;
    }
}
