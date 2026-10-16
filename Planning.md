# PLANNING (steps for the CURRENT task in Task.md)
1. [ ] Existing ChatStore/OpenAiCompat/CodeAgent retry paths اور Prefs persistence map کرنا؛ router data model منتخب کرنا۔
2. [ ] Local model health، cooldown، latency/error، daily usage اور configurable limits کا persistence شامل کرنا۔
3. [ ] تمام eligible provider models کے لیے score/selection router بنانا؛ task needs/tools اور context constraints apply کرنا۔
4. [ ] Chat اور Code provider requests کو router سے جوڑنا؛ retryable failure پر cooldown، log اور next-best failover شامل کرنا۔
5. [ ] Router log/status کو موجودہ console/logging flow میں شامل کرنا، پھر Actions build، tracking/CHANGES/Handoff update اور changed-files ZIP تیار کرنا۔
