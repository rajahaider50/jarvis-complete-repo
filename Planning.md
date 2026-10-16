# PLANNING (steps for the CURRENT task in Task.md)
1. [ ] Audit TaskStateStore/CodeAgent lifecycle, Android manifest, MainActivity startup and CodeScreen status controls; choose callback/service boundary without new dependencies.
2. [ ] Extend durable task state with `waiting_network`, pause reason and resumable run metadata; keep original goal/project identity immutable.
3. [ ] Add a process-level ConnectivityManager NetworkCallback and expose online/return events to coding mode; avoid duplicate auto-resume launches.
4. [ ] Make CodeAgent distinguish network loss/cancellation from provider failure, persist state before/after requests, and auto-resume on network return or explicit Continue.
5. [ ] Add existing-style Code screen network-waiting status/Continue action, add background survival foundation if safe, then build, test, update FEATURES/Complete/Pending/Handoff/CHANGES and create a changed-files ZIP.
