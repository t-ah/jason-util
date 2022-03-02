+task(_, Colour) <-
    !prepared;
    !holding(Colour);
    !processed;
    !delivered.

+!prepared : task(Colour) & holding(Block) & not colour(Block, Colour) <-
    putDown;
    !reset.

+!prepared <-
    !reset.

+!holding(Colour) : holding(Block) & colour(Block, Colour) <-
    .print("That's good.").

+!holding(Colour) : colour(Block,Colour) <-
    gotoBlock(Block);
    pickUp.

+!holding(Colour) : not colour(_, Colour) <-
    !found(Colour);
    ?colour(Block, Colour);
    gotoBlock(Block);
    pickUp.

+!found(Colour) : not colour(_, Colour) & place(Place) & not visited(Place) <-
    goto(Place);
    +visited(Place);
    !found(Colour).

+!found(Colour) : colour(_, Colour) <-
    .wait(10).

+!processed : not packaging <-
    .wait(10).

+!processed : packaging <-
    goto(packing);
    putDown;
    activate.

+!delivered <-
    goto(dropzone);
    putDown.

+!reset <-
    .abolish(visited(_)).