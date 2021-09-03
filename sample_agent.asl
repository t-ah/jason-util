// Agent sample_agent

/* Initial beliefs and rules */

/* Initial goals */

!start(3).
!do.
!short(0).

/* Plans */

+!start(X) : true <-
    !hi(1);
    !hi(2);
    !!hi(20);
    !hi(X);
    ?hi(Y);
    !hi(Y);
    !do.

+!do : true <-
    +do("OK").

+!hi(H) : H == 3 <-
    .fail.

+!hi(H) : true <-
    .bla(1);
    .bla(2);
    test(a);
    test(b);
    +test(H);
    +hello(H);
    +hi(H).

-!hi(H) : true <-
    +failed(H).

+!short(S) : true <-
    short(S).