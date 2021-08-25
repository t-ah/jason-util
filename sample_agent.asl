// Agent sample_agent in project logtest

/* Initial beliefs and rules */

/* Initial goals */

!start.

/* Plans */

+!start : true <-
    .print("hello world.");
    !do.

+!do : true <-
    .print("do OK.").