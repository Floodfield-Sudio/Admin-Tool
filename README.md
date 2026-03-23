# 🛡 AdminTools — Anti-Cheat & Admin Panel

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange)](https://minecraftforge.net)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-Noncommercial-red)](LICENSE)

Mod Forge 1.20.1 destiné aux **administrateurs de serveurs Minecraft**.  
Le mod est **optionnel côté client** : les joueurs normaux n'ont pas besoin de l'installer.

---

## ✨ Fonctionnalités

### 🔭 ESP (client admin uniquement)
| Feature | Description |
|---|---|
| **Ore X-Ray ESP** | Affiche les minerais à travers les blocs dans un rayon de 64 blocs, colorés par type |
| **Entity ESP** | Affiche les joueurs et mobs à travers les murs (bleu = joueur, rouge = hostile, vert = passif) |

### 👤 Outils Admin
| Feature | Description |
|---|---|
| **Vanish** | Rend l'admin invisible + indicateur `[VANISH]` clignotant sur son écran |
| **NoClip Admin** | Passe en spectateur (noclip natif), restaure le mode précédent à la désactivation |
| **Alertes Anti-Cheat** | Reçoit les notifications de violation en temps réel |

### 🚨 Détections Anti-Cheat (serveur)
| Détection | Seuil | Description |
|---|---|---|
| **Fly** | 80 ticks (4s) en l'air | Détecte le vol illégal |
| **NoClip** | 3 détections consécutives | Détecte les déplacements dans les blocs solides |
| **X-Ray** | 35% ores / roche hôte | Détecte les taux de minage suspects |

### 📊 Top Luck
Classement des joueurs par ratio **minerais / roche hôte** (pierre, netherrack, end stone).
- Détection automatique des ores de mods (Create, Mekanism, Thermal Series, etc.)
- Ratio correct par dimension (Overworld / Nether / End)
- Vue détaillée : barres par type de minerai + liste de tous les blocs cassés

### 🎒 InvSee
Inspection de l'inventaire complet d'un joueur en temps réel (lecture seule).

---

## 🎮 Utilisation

### Panel admin — touche `K`
Ouvre un panel avec 3 onglets :
- **Features** — toggles pour activer/désactiver chaque fonctionnalité
- **Joueurs** — liste des joueurs en ligne, avec boutons TP / InvSee / Check
- **Violations** — log scrollable des alertes anti-cheat

### Commandes `/at`
```
/at                               → Statut de ses features
/at status [joueur]               → Statut d'un joueur op
/at toggle <feature> [joueur]     → Basculer une feature
/at on|off <feature> [joueur]     → Forcer une feature
/at tp <joueur>                   → Se téléporter au joueur
/at invsee <joueur>               → Voir l'inventaire (GUI)
/at topluck [reset <joueur>]      → Classement minerais (GUI)
/at check <joueur>                → Infos rapides (mode, pos, ping)
/at help                          → Aide complète
```

**Noms de features :**
`ore_esp` · `entity_esp` · `vanish` · `noclip` · `alerts` · `fly_detect` · `noclip_detect` · `xray_detect`

> ⚠️ Requiert **op level 2** minimum.

---

## 📦 Installation

### Serveur
1. Copier le `.jar` dans le dossier `mods/` du serveur
2. Redémarrer le serveur
3. **Les joueurs n'ont pas besoin d'installer le mod** pour se connecter

### Admin (optionnel, pour panel et ESP)
1. Copier le même `.jar` dans le dossier `mods/` de son client Forge
2. Se connecter au serveur — le panel `[K]` et les ESP sont disponibles

---

## 🔨 Compilation

Prérequis : **Java 17**, **Gradle 8.x**

```bash
git clone https://github.com/Floodfield-Sudio/admintools.git
cd admintools
./gradlew build
# Le .jar se trouve dans build/libs/
```

Ou utiliser **DevStudio Pro** :
1. Ouvrir le dossier `admintools/` comme projet
2. Onglet ⛏ Minecraft → Forge 1.20.1
3. Cliquer 🔨 Build

---

## 🏗 Structure du code

```
src/main/java/com/admintools/
├── AdminToolsMod.java          ← Entrée principale @Mod
├── AdminFeature.java           ← Enum des 8 features
├── AdminToolsState.java        ← État serveur (features, vanish, noclip)
├── TopLuckEntry.java           ← DTO neutre pour le classement
├── client/
│   ├── AdminPanel.java         ← GUI panel admin (touche K)
│   ├── ClientAdminState.java   ← État client (features actives)
│   ├── ClientPacketHandler.java← Proxy pour ouvrir les écrans depuis les packets
│   ├── InvSeeScreen.java       ← Écran inventaire (lecture seule)
│   ├── TopLuckScreen.java      ← Écran classement Top Luck
│   ├── VanishHUD.java          ← Indicateur [VANISH] clignotant
│   └── render/
│       ├── ESPRenderer.java    ← Rendu ore/entity ESP (through walls)
│       └── ESPRenderType.java  ← RenderType sans depth test
├── server/
│   ├── AdminCommand.java       ← Commande /at
│   ├── TopLuckTracker.java     ← Suivi ratio minerais par joueur
│   └── anticheat/
│       ├── AntiCheatManager.java ← Détections Fly/NoClip/XRay
│       └── ViolationType.java
├── events/
│   ├── ServerEvents.java       ← Login/Logout/Tick/BlockBreak
│   └── ClientEvents.java       ← Touches, ESP, HUD
└── network/
    ├── NetworkHandler.java     ← Canal SimpleChannel (optionnel côté client)
    └── packets/
        ├── C2STogglePacket.java
        ├── C2SInvSeePacket.java
        ├── S2CStatePacket.java
        ├── S2CViolationPacket.java
        ├── S2CInvSeePacket.java
        └── S2CTopLuckPacket.java
```

---

## 🔮 Compatibilité

| Version MC | Loader | Statut |
|---|---|---|
| 1.20.1 | Forge 47.x | ✅ Supporté |
| 1.21.1 | NeoForge | 🔜 Prévu |

---

## 🤝 Contribuer

Les contributions sont les bienvenues !

1. **Fork** le dépôt
2. Crée une branche : `git checkout -b feature/ma-feature`
3. Commit : `git commit -m "feat: description"`
4. Push : `git push origin feature/ma-feature`
5. Ouvre une **Pull Request**

Merci de respecter le style de code existant et d'inclure une description claire des changements.

---

## 📜 Licence

Ce mod est distribué sous la licence **Polyform Noncommercial 1.0.0**.

✅ **Autorisé :**
- Usage personnel, sur ton propre serveur (gratuit)
- Modification et redistribution non-commerciale
- Usage dans un contexte associatif ou éducatif

❌ **Interdit :**
- Vendre le mod ou l'accès à celui-ci
- L'inclure dans un produit commercial
- L'utiliser dans un service payant

Voir [LICENSE](LICENSE) pour les détails complets.

---

## 📬 Contact

Ouvre une [issue](../../issues) pour les bugs ou suggestions.
[Site Web](https://floodfield-sudio.github.io/FFS.index/)
