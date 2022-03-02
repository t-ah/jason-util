// initial goal
!deliver.

// If done then stop
+!deliver : nextColour(done) <-
    +done.

// select a block of the right colour and go get and deliver it
+!deliver : colour(B, C) & nextColour(C) & not holding(_) <- // this is where the bug is: holding(B) vs. holding(_)
    gotoBlock(B);
    pickUp;
    !deliver.

+!deliver : holding(B) & colour(B,C) & nextColour(C) <-
    goto(dropzone);
    putDown;
    !reset;
    !deliver.

// if holding a block that is not the next colour required then put it down (this may occur if e.g. someone else delivers a block, so the next colour changes)
+!deliver : holding(B) & colour(B,C) & not nextColour(C) <-
    putDown;
    !reset;
    !deliver.

// if I know of a place that I’ve not yet visited then go there (explore)
+!deliver : place(P) & not visited(P) <-
    goto(P);
    +visited(P);
    !deliver.

-!delivers <-
    .print("I'm totally out of ideas.").

+!reset <-
    .abolish(visited(_)).