# PLANNING (steps for the CURRENT task in Task.md)
1. [ ] Existing CodeAgent/Workspace run lifecycle map کرنا اور durable state schema بنانا (`code/Workspace.kt`, نئی task-state file)۔
2. [ ] Original goal، plan، decisions، completed files، current file اور conversation کے لیے local JSON persistence شامل کرنا۔
3. [ ] CodeAgent میں start/step/file-write/finish/error کے checkpoints شامل کرنا؛ cancellation یا failure پر state محفوظ رکھنا۔
4. [ ] App start یا Code screen میں unfinished task کی resume availability دکھانا، existing UI components کے ساتھ۔
5. [ ] Self-check، GitHub Actions build، tracking/CHANGES/Handoff update، اور صرف بدلی ہوئی فائلوں کی ZIP تیار کرنا۔
