# jason-util

## A Jason architecture realising incremental agent state logging.

Located in `arch`. Will set log level to FINE.

### Logging output format

The first line is a JSONObject containing general information about the agent.

- src: str
- name: str
- entity: str
- platform: str
- details: obj
  - plans: obj
    - keys=plan labels
      - file: str
      - line: int
      - trigger: str
      - ctx: str
      - body: str

Each other line of the output file is a JSONObject representing the changes made in one agent cycle.

- nr: int (number of the agent cycle)
- I+: int (id of the new intention)
- I-: arr of ints (ids of finished intentions)
- U: str (current unifier for selected intention SI)
- IM+: arr (new intended means)
  - id: int
  - file: str
  - line: int
  - i: int (intention the IM belongs to)
  - trigger: str (unified)
  - ctx: str (context, unified)
  - plan: str (label)
- IM-: arr of objs (intended means finished)
  - id: int
  - res: str (result, "achieved", "np"=no plan (combined with id=-1), "failed")
  - reason: obj (only if "failed" and root cause of failure, i.e. if failed due to child IM failing, no reason supplied)
    - error_msg: str
    - code_line: str(!)
    - code_src: str
    - type: str (e.g. "pf_nr"=plan_failure_no_recovery)
    - error: str (error type supplied by Jason)
- SI: int (id of selected intention)
- SE: int (id of selected event)
- I: obj (instruction that was run)
  - file: str
  - line: int
  - im: int (id of intended means on top of the intention)
  - instr: str (actual instruction, unified)
- B+: arr (new beliefs)
  - str entries (belief text, unified)
- B-: inverse to B+
- E+: arr of objs (new events)
  - id: int
  - t: str (event content)
  - src: str ("B"=belief, "G"=goal)
  - nf: true (indicates new focus, event may lead to a new intention)
- A+: intention for which a pending action was added
- A-: intention for which the latest pending action finished executing (never refers to A+ from the same cycle)

## Some examples

Run examples with `gradle` or `gradle [...]`.
