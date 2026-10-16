# PLANNING (steps for the CURRENT task in Task.md)
1. [ ] Provider model data اور backward-compatible local cache schema شامل کرنا (`data/CustomProviders.kt`، نیا `FEATURES.md`).
2. [ ] `/models` response سے real pricing/context/tools metadata parse کرنا؛ local catalog fallback اور free/unknown classification شامل کرنا (`data/CustomProviders.kt`).
3. [ ] Provider editor میں Free only، refresh، manual model entry اور grouped Free/Paid/Unverified list شامل کرنا (`ui/screens/ProvidersScreen.kt`).
4. [ ] Chat picker کو provider eligibility/free-only catalog کے مطابق update کرنا (`ui/screens/ChatScreen.kt`، ضرورت ہو تو `data/ChatStore.kt`).
5. [ ] Self-check، GitHub Actions build، tracking/CHANGES/handoff update، اور صرف بدلی ہوئی فائلوں کی ZIP تیار کرنا۔
