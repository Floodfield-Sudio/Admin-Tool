# 🤝 Contribuer à AdminTools

Merci de ton intérêt ! Voici comment contribuer proprement.

## 🐛 Signaler un bug

1. Vérifie que le bug n'est pas déjà signalé dans les [Issues](../../issues)
2. Ouvre une nouvelle issue avec :
   - La version du mod (`admintools-X.X.X.jar`)
   - La version de Forge
   - Les logs complets (`logs/latest.log` ou le crash report)
   - Les étapes pour reproduire le bug

## 💡 Proposer une fonctionnalité

Ouvre une issue avec le tag `enhancement` en décrivant :
- Ce que tu veux faire
- Pourquoi c'est utile pour les admins de serveur

## 🔧 Soumettre une Pull Request

### Prérequis
- Java 17
- Gradle 8.x (ou utiliser DevStudio Pro)

### Étapes

```bash
# 1. Fork puis clone
git clone https://github.com/Floodfield-Sudio/admintools.git
cd admintools

# 2. Créer une branche descriptive
git checkout -b fix/crash-serveur-dedie
# ou
git checkout -b feat/nouvelle-detection

# 3. Compiler et tester
./gradlew build

# 4. Commit (format conventionnel)
git commit -m "fix: crash NoSuchFieldError sur serveur dédié"
git commit -m "feat: ajout détection KillAura"
git commit -m "docs: mise à jour README"

# 5. Push et PR
git push origin ma-branche
```

### Conventions de code
- **Encodage** : UTF-8 (forcé dans `build.gradle`)
- **Pas de code client dans les classes communes** : utiliser `DistExecutor` ou `ClientPacketHandler`
- **Pas de `@OnlyIn` sur des champs de classes chargées côté serveur** — créer une classe dédiée à la place
- Commenter en **français ou anglais**, les deux sont acceptés

### Ce qu'on accepte avec plaisir
- Corrections de bugs
- Nouvelles détections anti-cheat
- Améliorations de l'UI du panel
- Support de nouvelles versions Minecraft/Forge/NeoForge
- Traductions

### Ce qu'on n'accepte pas
- Toute fonctionnalité destinée à faciliter la triche
- Code obfusqué ou sans commentaires
- Dépendances externes non justifiées

---

En soumettant une PR, tu acceptes que ta contribution soit distribuée sous la même licence que le projet (Polyform Noncommercial 1.0.0).
