# Simulation d'un Pont Mobile Bidirectionnel en Java (Multithreading)

Projet réalisé dans le cadre de la ressource **R3.05 - Programmation Système** (BUT 2ème année Informatique).

## Présentation du Projet
Ce projet simule le fonctionnement d'un pont mobile régulant la circulation simultanée de **véhicules terrestres** (dans les deux sens) et de **bateaux** (passant sous le pont). 

L'objectif principal est de gérer la concurrence, l'exclusion mutuelle et d'éviter les situations de blocage (**Deadlock**) grâce aux mécanismes de synchronisation native de Java (`synchronized`, `wait()`, `notifyAll()`).

---

## Architecture & Modélisation

Chaque entité du système est exécutée sur un **Thread** indépendant :
* **Vehicules (`Vehicle`)** : Traversent le pont selon leur taille et leurs règles de priorité.
* **Bateaux (`Boat`)** : Passent sous le pont en bloquant la circulation routière.
* **Le Pont (`Bridge`)** : Sert de **Moniteur partagé** qui gère la réservation atomique des segments et les transitions d'états.

---

## Règles de Circulation & Fonctionnalités

### 1. Gestion des Segments du Pont
Le pont est découpé en **$N$ segments consécutifs** ($S_1, S_2, \dots, S_N$).
*  **Voitures** : Occupent **1 segment** à la fois.
*  **Camions** : Occupent **2 segments consécutifs** simultanément.
*  **Convois Exceptionnels** : Occupent **4 segments consécutifs** et **bloquent immédiatement les nouvelles entrées en sens opposé** à leur arrivée.

### 2. Gestion des Bateaux & États du Pont
Le pont dispose d'un automate à **4 états** :
`FERMÉ` ➔ `OUVERTURE` ➔ `OUVERT` ➔ `FERMETURE` ➔ `FERMÉ`

* **File FIFO** : Les bateaux s'inscrivent dans une file d'attente globale et traversent un par un dans leur ordre d'arrivée.
* **Évacuation sécurisée** : Dès qu'un bateau demande le passage, l'accès aux nouveaux véhicules est bloqué (`OUVERTURE`). Le pont attend l'évacuation totale des véhicules engagés avant de s'ouvrir aux bateaux.
* **Chrono / Delay** : Un délai minimal d'**1 minute** (via `Timer` / `TimerTask`) est respecté entre deux ouvertures complètes du pont.

### 3. Véhicules Prioritaires (Bande d'Urgence)
*  **Ambulances / Pompiers** : Lors de l'arrivée d'un véhicule prioritaire, les autres véhicules s'interrompent et se déportent temporairement sur la **bande d'urgence** en libérant la voie principale.
* Le véhicule prioritaire traverse, puis les véhicules en attente se ré-insèrent sur le pont.

---

##  Structure des Fichiers

```text
src/main/
├── Bridge.java      # Moniteur partagé (Gestion des verrous, segments et états)
├── Vehicle.java     # Class Thread représentant les véhicules (Voiture, Camion, Convoi, Ambulance)
├── Boat.java        # Class Thread représentant les bateaux
├── Sens.java        # Enum (GAUCHE_VERS_DROITE, DROITE_VERS_GAUCHE)
├── SensBoat.java    # Enum (NORD_VERS_SUD, SUD_VERS_NORD)
└── Main.java        # Point d'entrée & Scénario de démonstration
