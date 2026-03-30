# 🛡 AdminTools — Anti-Cheat & Admin Panel

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange)](https://minecraftforge.net)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-Noncommercial-red)](LICENSE)
[![Client Optional](https://img.shields.io/badge/Client-Optionnel-blue)](#installation)

Mod Forge 1.20.1 destiné aux **administrateurs de serveurs Minecraft**.  
Le mod est **optionnel côté client** : les joueurs normaux n'ont pas besoin de l'installer.

---

## ✨ Fonctionnalités

### 🔭 Ore X-Ray ESP
- Affiche les minerais à travers les blocs avec des boîtes colorées
- **Rayon configurable** : de 8 à 128 blocs (défaut : 64), par pas de 8
- **Toggle par type de minerai** : activer/masquer individuellement chaque ore
- **Couleurs distinctes automatiques** pour chaque mod :
  - Ores vanilla : couleurs fixes (diamant = cyan, or = jaune, redstone = rouge…)
  - Ores moddés (Create, Mekanism, Thermal…) : couleurs générées par rotation HSV (golden angle 137.5°) — chaque ore a une teinte unique et saturée, jamais le même violet générique
- Accessible via **⚙ Config X-Ray** dans le panel `[K]`

| Ore vanilla | Couleur |
|---|---|
| Diamant | 🔵 Cyan `#00E5FF` |
| Émeraude | 🟢 Vert vif `#00FF40` |
| Or + Or Nether | 🟡 Jaune `#FFD700` |
| Fer | 🟤 Brun `#CC9966` |
| Ancient Debris | 🟠 Orange foncé `#CC4811` |
| Redstone | 🔴 Rouge `#FF1414` |
| Lapis | 💙 Bleu roi `#2244EE` |
| Charbon | ⚫ Gris `#474747` |
| Cuivre | 🟤 Cuivré `#D18030` |
| Quartz (Nether) | ⬜ Blanc `#F3F3F3` |

### 👁 Entity ESP
Affiche les entités vivantes à travers les murs :
- 💙 Bleu — Joueurs
- 🔴 Rouge — Mobs hostiles
- 💚 Vert — Mobs passifs

### 👤 Outils Admin
| Feature | Description |
|---|---|
| **Vanish** | Invisibilité + indicateur `[VANISH]` clignotant sur l'écran de l'admin |
| **NoClip Admin** | Passe en spectateur (noclip natif), restaure le mode précédent à la désactivation |
| **Alertes Anti-Cheat** | Reçoit les violations en temps réel dans le panel et le chat |

### 🚨 Détections Anti-Cheat (serveur)
| Détection | Seuil | Description |
|---|---|---|
| **Fly** | 80 ticks (4 s) en l'air | Vol illégal — ignorer si elytra/eau/échelle |
| **NoClip** | 3 ticks consécutifs dans un bloc solide | Déplacement dans la matière |
| **X-Ray** | ≥ 35 % ores / roche hôte | Taux de minage suspect |

### 📊 Top Luck
Classement des joueurs par ratio **minerais / roche hôte** — beaucoup plus précis que ores/total car insensible au bois/terre creusés.

- **Roche hôte par dimension** :
  - Overworld → pierre, deepslate, granite, diorite, andesite, tuff, calcite…
  - Nether → netherrack, basalt, blackstone, soul sand…
  - End → end stone, end stone bricks
- **Détection automatique des ores de mods** (Create, Mekanism, Thermal Series, etc.)
- **Onglet Ores** : barres de progression par groupe de minerai (% / roche hôte)
- **Onglet Tous les blocs** : liste complète de tous les blocs cassés par le joueur, triée par count, avec code couleur (ores = vert, roche = gris, reste = bleu)

### 🎒 InvSee
Inspection de l'inventaire complet d'un joueur en lecture seule :
- 36 slots de l'inventaire principal
- 4 slots d'armure
- Slot hors-main (offhand)
- Tooltip au survol de chaque item

---

## 🎮 Utilisation

### Panel admin — touche `K`

**3 onglets :**

- **Features** — 8 toggles ON/OFF + bouton **⚙ Config X-Ray**
- **Joueurs** — liste des joueurs en ligne, clic pour sélectionner puis :
  - **✈ TP** → se téléporter au joueur
  - **🎒 InvSee** → ouvrir l'inventaire (GUI)
  - **🔍 Check** → infos rapides dans le chat
- **Violations** — log scrollable des alertes anti-cheat (couleur par type : rouge = Fly, orange = NoClip, jaune = X-Ray)

### ⚙ Config X-Ray ESP (depuis le panel)

- **Rayon** : boutons `−` / `+` pour ajuster de 8 à 128 blocs
- **Grille des ores** : clic sur un ore pour l'activer/masquer
- Carré coloré = visible, carré barré = masqué
- Boutons **Tout activer**, **Tout masquer**, **Reset**

### Commandes `/at`
```
/at                               → Statut de ses features
/at status [joueur]               → Statut d'un admin (soi ou autre)
/at toggle <feature> [joueur]     → Basculer une feature
/at on|off <feature> [joueur]     → Forcer une feature
/at tp <joueur>                   → Se téléporter au joueur
/at invsee <joueur>               → Voir l'inventaire (GUI)
/at topluck [reset <joueur>]      → Classement minerais (GUI)
/at check <joueur>                → Infos rapides (mode, pos, vol, ping)
/at help                          → Aide complète
```

**Noms de features :**
`ore_esp` · `entity_esp` · `vanish` · `noclip` · `alerts` · `fly_detect` · `noclip_detect` · `xray_detect`

> ⚠️ Requiert **op level 2** minimum.

---

## 📦 Installation

### Serveur (obligatoire)
1. Copier `admintools-1.0.0.jar` dans le dossier `mods/` du serveur
2. Redémarrer

### Joueurs normaux
Rien à faire. **Aucune installation requise** pour se connecter.

### Admins (optionnel, pour panel et ESP)
1. Copier le même `.jar` dans `mods/` de son client Forge
2. Se connecter — le panel `[K]`, l'ESP et l'InvSee sont disponibles

> Si un admin n'a pas le mod côté client, il peut quand même utiliser `/at` dans le chat pour TP, InvSee, TopLuck, etc.

---

## 🔨 Compilation

Prérequis : **Java 17**, **Gradle 8.x**

```bash
git clone https://github.com/Floodfield-Sudio/Admin-Tool.git
cd Admin-Tool
./gradlew build
# Le .jar apparaît dans build/libs/
```

Ou avec **DevStudio Pro** :
1. Ouvrir le dossier comme projet
2. Onglet ⛏ Minecraft → Forge 1.20.1
3. Cliquer **🔨 Build**

---

## 🏗 Structure du projet

```
src/main/java/com/admintools/
├── AdminToolsMod.java              ← @Mod principal
├── AdminFeature.java               ← Enum des 8 features
├── AdminToolsState.java            ← État serveur
├── TopLuckEntry.java               ← DTO classement Top Luck
├── client/
│   ├── AdminPanel.java             ← GUI panel (touche K) — 3 onglets
│   ├── OreESPSettingsScreen.java   ← GUI config X-Ray ESP
│   ├── ClientAdminState.java       ← État client
│   ├── ClientPacketHandler.java    ← Proxy ouverture écrans
│   ├── InvSeeScreen.java           ← Écran inventaire
│   ├── TopLuckScreen.java          ← Écran Top Luck
│   ├── VanishHUD.java              ← Indicateur [VANISH]
│   └── render/
│       ├── ESPRenderer.java        ← Rendu ore/entity ESP
│       ├── ESPRenderType.java      ← RenderType sans depth test
│       └── OreESPSettings.java     ← Réglages rayon + toggle/couleur par ore
├── server/
│   ├── AdminCommand.java           ← Commande /at
│   ├── TopLuckTracker.java         ← Ratio minerais/roche hôte
│   └── anticheat/
│       ├── AntiCheatManager.java
│       └── ViolationType.java
├── events/
│   ├── ServerEvents.java
│   └── ClientEvents.java
└── network/
    ├── NetworkHandler.java         ← Canal optionnel côté client
    └── packets/                    ← 6 packets C2S/S2C
```

---

## 🔮 Compatibilité

| Version MC | Loader | Statut |
|---|---|---|
| 1.20.1 | Forge 47.x | ✅ Stable |
| 1.21.1 | NeoForge | 🔜 Prévu |

---

## 🤝 Contribuer

1. Fork → branche `feat/ma-feature` → PR
2. Lire [CONTRIBUTING.md](CONTRIBUTING.md)

---

## 📜 Licence

**Polyform Noncommercial 1.0.0** — usage non-commercial uniquement.

✅ Usage perso, serveur communautaire, modification, redistribution non-commerciale  
❌ Vente, service payant, produit commercial

Voir [LICENSE](LICENSE) pour les détails.
