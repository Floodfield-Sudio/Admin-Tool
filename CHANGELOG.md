# Changelog

Toutes les modifications notables sont documentées ici.
Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/).

---

## [1.0.0] — 2024

### Ajouté
- **Ore X-Ray ESP** : affiche les minerais à travers les blocs (rayon 64 blocs)
  - Détection automatique des ores moddés (Create, Mekanism, Thermal, etc.)
  - Couleurs par type de minerai
- **Entity ESP** : affiche joueurs/mobs à travers les murs
- **Vanish** : invisibilité admin + indicateur `[VANISH]` clignotant sur l'écran
- **NoClip Admin** : passage en spectateur avec restauration du mode précédent
- **Alertes Anti-Cheat** : notifications en temps réel des violations
- **Détection Fly** : signale les vols illégaux (seuil 80 ticks)
- **Détection NoClip** : signale les déplacements dans les blocs solides
- **Détection X-Ray** : signale les taux de minage suspects (seuil 35% roche hôte)
- **Top Luck** : classement des ratios minerais/roche hôte par dimension
  - Onglet Ores : barres par groupe de minerai
  - Onglet Tous les blocs : liste de tous les blocs cassés
- **InvSee** : inspection de l'inventaire complet d'un joueur (lecture seule)
- **Panel admin** (touche `K`) : 3 onglets Features / Joueurs / Violations
- **Commande `/at`** : status, toggle, on/off, tp, invsee, topluck, check, help
  - Cible optionnelle : `/at toggle <feature> [joueur]`
- **Mod optionnel côté client** : les joueurs normaux n'ont pas besoin du mod

---

## À venir

- [ ] Support NeoForge 1.21.1
- [ ] Détection Speed/KillAura
- [ ] Historique persistant des violations (fichier log)
- [ ] Commande `/at ban` / `/at kick` intégrée
