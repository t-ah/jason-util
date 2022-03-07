+task(Id, Colour) <-
    wait;
    !prepared;
    !holding(Colour);
    !processed;
    !delivered;
    !deliveryChecked(Id).

+!prepared : task(Colour) & holding(Block) & not colour(Block, Colour) <-
    putDown;
    !prepared.

+!prepared <-
    !charged;
    !reset.

+!charged : energy(MyEnergy) & MyEnergy < 80 <-
    recharge;
    !charged.

+!charged : energy(MyEnergy) <-
    .print("My energy is ", MyEnergy).

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
    wait.

+!processed : not packaging <-
    wait.

+!processed : packaging <-
    goto(packing);
    putDown;
    activate.

+!delivered <-
    goto(dropzone);
    putDown.

+!deliveryChecked(Id) : delivered(Id) <-
    ?delivered(Id).

+!deliveryChecked(Id) <-
    .print("I am Error.").

+!reset <-
    .abolish(visited(_)).