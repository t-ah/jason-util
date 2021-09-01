// Agent sample_agent

/* Initial beliefs and rules */

/* Initial goals */

!start(3).

/* Plans */

+!start(X) : true <-
    !hi(1);
    !hi(2);
    !hi(X);
    ?hi(Y);
    !hi(Y);
    !do.

+!do : true <-
    +do("OK").

+!hi(H) : H == 3 <-
    .fail.

+!hi(H) : true <-
    +test(H);
    +hello(H);
    +hi(H).

-!hi(H) : true <-
    +failed(H).