package jason.env.blocks;

public class Block {

    public final String id;
    public final String colour;

    private boolean pickedUp = false;
    private boolean packaged = false;

    public Block(String id, String color) {
        this.id = id;
        this.colour = color;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public boolean isPackaged() {
        return packaged;
    }

    public void pickUp() {
        pickedUp = true;
    }

    public void putDown() {
        pickedUp = false;
    }

    public void packageBlock() {
        this.packaged = true;
    }
}
