+nextColour(C) <-
    !prepared;
    !holding(C);
    !delivered.

+!prepared : nextColour(C) & holding(B) & not colour(B,C) <-
    putDown;
    !reset.

+!prepared <-
    !reset.

+!holding(C) : holding(B) & colour(B,C) <-
    .print("That's good.").

+!holding(C) : colour(B,C) <-
    gotoBlock(B);
    pickUp.

+!holding(C) : not colour(_,C) <-
    !found(C);
    ?colour(B,C);
    gotoBlock(B);
    pickUp.

+!found(C) : not colour(_,C) & place(P) & not visited(P) <-
    goto(P);
    +visited(P);
    !found(C).

+!found(C) : colour(_,C) <-
    .wait(10).

+!delivered : holding(_) <-
    goto(dropzone);
    putDown.

+!reset <-
    .abolish(visited(_)).