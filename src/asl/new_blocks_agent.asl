!wait.
+!wait <-
    wait.

+place(R) <- wait.

+task(Id,_) <-
    .print("new task: ", Id);
    wait.

+task(Id, Colour) : not busy <-
    !completeTask(Id, Colour).

+task(Id, Colour) : busy <-
    +openTask(Id, Colour).

+!completeTask(Id, Colour) <-
    +busy;
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

+!reset <-
    .abolish(visited(_)).

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
    ?delivered(Id);
    -busy.

+!deliveryChecked(_) <-
    .print("I am Error.").