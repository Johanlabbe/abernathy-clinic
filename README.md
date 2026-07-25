# 🏥 Abernathy Clinic — Solution en microservices

Outil de **soins préventifs** aidant les médecins à identifier les patients les plus exposés au **diabète de type 2**. L'application recense les données démographiques d'un patient, les notes de consultation, et en déduit un **niveau de risque**.

Projet 9 — parcours *Développeur d'application Java* (OpenClassrooms). Architecture **microservices Spring Boot**, orchestrée par **Docker Compose** et sécurisée par **Keycloak** (OAuth2 / OpenID Connect).

---

## Sommaire

- [Contexte fonctionnel](#contexte-fonctionnel)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Démarrage](#démarrage)
- [Comptes de test](#comptes-de-test)
- [Sécurité](#sécurité)
- [Structure du dépôt](#structure-du-dépôt)
- [🌱 Green Code (Étape 6)](#-green-code-étape-6)

---

## Contexte fonctionnel

Trois besoins, livrés en trois sprints :

1. **Gestion des patients** — l'*organisateur* consulte, ajoute et met à jour les informations personnelles (nom, prénom, date de naissance, genre, adresse, téléphone).
2. **Notes médicales** — le *praticien* consulte l'historique et ajoute des notes de consultation.
3. **Évaluation du risque de diabète** — le *praticien* génère un rapport de risque parmi quatre niveaux : `None`, `Borderline`, `In Danger`, `Early onset`. Le niveau dépend de l'âge, du genre et du nombre de **termes déclencheurs** repérés dans les notes.

---

## Architecture

| Service | Port | Rôle | Stockage | Rôle OAuth2 |
|---|:---:|---|---|---|
| **front-end** | 8082 | Interface web (Thymeleaf) | — | Client OIDC |
| **gateway-service** | 8080 | Point d'entrée unique, routage | — | Resource server (réactif) |
| **patient-service** | 8081 | CRUD patients | MySQL | Resource server |
| **note-service** | 8083 | Notes médicales | MongoDB | Resource server |
| **assessment-service** | 8084 | Calcul du risque | *aucun* (interroge patient + note) | Resource server |
| **keycloak** | 8090 | Serveur d'autorisation | H2 (interne) | — |
| **mysql** | 3306 | Base patients | — | — |
| **mongo** | 27017 | Base notes | — | — |

```
Navigateur ──login OIDC──▶ Keycloak (:8090)
    │ session
    ▼
front-end (:8082) ──Bearer JWT──▶ gateway (:8080) ──▶ patient / note / assessment
                                                              │
                                    assessment rappelle la gateway (patient + notes)
```

---

## Stack technique

- **Java 21**, **Spring Boot 3.4.2** (Maven multi-module)
- **Spring Cloud Gateway** (routage réactif)
- **Spring Data JPA** + **MySQL 8.4** · **Spring Data MongoDB** + **MongoDB 7**
- **Spring Security** — OAuth2 Resource Server & OAuth2 Client
- **Keycloak 26.6.2** (OAuth2 / OIDC, Authorization Code + PKCE)
- **Thymeleaf** (rendu côté serveur)
- **Docker** / **Docker Compose**

---

## Démarrage

**Prérequis :** Docker Desktop (avec Docker Compose). Java 21 et Maven sont **optionnels** — la compilation se fait dans les conteneurs (Dockerfiles multi-stage).

Depuis la racine du dépôt :

```bash
docker compose up --build
```

Une fois la stack démarrée :

- **Application :** http://localhost:8082 (redirige vers le login Keycloak)
- **Console Keycloak :** http://localhost:8090 → `admin` / `admin` → realm `abernathy`

Arrêter la stack (les données MySQL/Mongo sont conservées) :

```bash
docker compose down
```

Repartir de zéro (efface les volumes, réimporte le realm Keycloak) :

```bash
docker compose down -v
```

---

## Comptes de test

| Identifiant | Mot de passe | Rôle | Accès |
|---|---|---|---|
| `dr.house` | `password` | praticien | Patients (lecture) + notes + évaluation du risque |
| `secretary` | `password` | organisateur | Patients (lecture + création / modification) |
| `admin` | `admin` | — | Console d'administration Keycloak |

---

## Sécurité

L'authentification et l'autorisation reposent sur **Keycloak** (OAuth2 / OIDC). Le front-end se connecte via **Authorization Code + PKCE**, obtient un **JWT**, que la gateway puis chaque microservice valident. Les accès sont contrôlés **par rôle métier** via `@PreAuthorize`.

| Endpoint | praticien | organisateur |
|---|:--:|:--:|
| `GET /patient/all`, `GET /patient/{id}` | ✅ | ✅ |
| `POST /patient/add`, `PUT /patient/update/{id}` | ❌ | ✅ |
| `GET /note/patient/{patId}`, `POST /note/add` | ✅ | ❌ |
| `GET /assessment/{patId}` | ✅ | ❌ |

> Logique RGPD : les **données médicales** (notes, évaluation) sont réservées au praticien ; la gestion de l'**état civil** à l'organisateur ; la **lecture** de la fiche patient est partagée.

📄 Détails : [choix du serveur d'autorisation](docs/authorization-server.md) · [mise en place de Keycloak](docs/keycloak-setup.md).

---

## Structure du dépôt

```
abernathy-clinic/
├── gateway-service/       # Spring Cloud Gateway (resource server réactif)
├── patient-service/       # CRUD patients (MySQL)
├── note-service/          # Notes médicales (MongoDB)
├── assessment-service/    # Calcul du risque de diabète
├── front-end/             # Interface Thymeleaf (client OIDC)
├── keycloak/              # realm-abernathy.json (import automatique)
├── docs/                  # Documentation d'architecture / sécurité
├── docker-compose.yml     # Orchestration de toute la stack
└── pom.xml                # POM parent (multi-module)
```

---

## 🌱 Green Code (Étape 6)

> Cette section constitue le livrable de l'**étape 6** du projet : identifier les **enjeux** du Green Code et lister des **pistes d'amélioration** pour ce projet.
>
> ⚠️ Il s'agit d'une **analyse critique** : les pistes ci-dessous sont **volontairement non appliquées**. L'étape ne demande pas de refactorer le code, et la stack est fonctionnelle en l'état — les appliquer juste avant clôture n'apporterait aucune exigence supplémentaire tout en risquant de déstabiliser l'existant.

### Qu'est-ce que le Green Code et quel est son objectif ?

Le **Green Code** (ou écoconception logicielle) vise à **réduire l'empreinte environnementale d'un logiciel**. Le numérique représente aujourd'hui de l'ordre de **4 % des émissions mondiales de gaz à effet de serre**, une part en croissance continue, tirée autant par les infrastructures (data centers, réseaux) que par la fabrication du matériel.

L'objectif est simple à énoncer : **rendre le même service en consommant moins de ressources** — CPU, mémoire vive, réseau, stockage — et donc *in fine* moins d'électricité et moins de matériel. On distingue deux leviers complémentaires :

- **l'efficience** : écrire du code qui fait le même travail avec moins de calculs, moins d'allocations, moins d'échanges réseau ;
- **la sobriété** : ne pas exécuter ce qui est inutile (données chargées « au cas où », services qui tournent en permanence, traitements redondants).

Au-delà de l'aspect écologique, un code plus sobre apporte des **bénéfices connexes** directs : coûts d'hébergement réduits (surtout en cloud facturé à l'usage), meilleures performances et latences, et une **durée de vie du matériel prolongée** (un logiciel moins gourmand repousse l'obsolescence).

### Comment repérer le code qui consomme des ressources inutilement ?

Deux approches complémentaires :

**1. Mesurer (profiling & monitoring)** — on ne peut optimiser que ce que l'on mesure :

- **profilers JVM** (VisualVM, async-profiler, JProfiler) pour observer l'usage CPU, les allocations mémoire et l'activité du *garbage collector* ;
- **Spring Boot Actuator + Micrometer** pour exposer les métriques applicatives (heap, GC, requêtes) ;
- `docker stats` pour la **RAM/CPU par conteneur**, et `docker images` pour la **taille des images** ;
- observer le **volume et le nombre** de requêtes SQL et d'appels réseau.

**2. Relire le code (chasse aux anti-patterns)** — certains signaux se repèrent à la lecture :

- requêtes de liste **sans pagination** (on charge tout en mémoire) ;
- problèmes **N+1** (une requête par élément d'une liste) ;
- **concaténation de `String` dans une boucle** (réallocation à chaque tour) ;
- objets ou calculs **recréés à chaque appel** alors qu'ils pourraient être mis en cache ou en constante ;
- **appels réseau / base répétés** pour des données qui changent peu ;
- **sur-sérialisation** (renvoyer plus de champs que nécessaire) ;
- **logs verbeux** en production (I/O + construction de chaînes) ;
- services / conteneurs **actifs en permanence** alors qu'ils sont inutilisés.

> Trois questions réflexes : *« Est-ce que je charge plus de données que nécessaire ? »*, *« Est-ce que je recalcule à chaque appel quelque chose de stable ? »*, *« Est-ce que ce traitement/service tourne alors qu'il est inutile ? »*

### Pistes d'amélioration pour *ce* projet

#### 1. Conteneurs & déploiement

| Constat (dans le code) | Piste | Bénéfice attendu |
|---|---|---|
| Les 5 services partent tous de `eclipse-temurin:21-jre` (JRE complet) — [Dockerfiles](patient-service/Dockerfile) | Base **slim** (`-jre-alpine`) ou **runtime sur mesure** via `jlink`/`jdeps`, et/ou **image de base commune** mutualisée | Réduction sensible de la taille des images (à mesurer avec `docker images`) → moins de stockage, de transfert registre et de surface d'attaque |
| Keycloak lancé en `start-dev` — [docker-compose.yml](docker-compose.yml) | Mode `start` optimisé (build) + base externe | Empreinte mémoire réduite au démarrage et en fonctionnement |
| 8 conteneurs en `restart: unless-stopped`, dont 3 datastores, actifs en permanence | **Sobriété** : arrêter la stack quand elle n'est pas utilisée (dev) ; dimensionner / *scale-to-zero* (prod) | Moins de RAM/CPU consommés à vide |
| Aucune limite mémoire ni réglage JVM sur les conteneurs | Définir `mem_limit` + `-XX:MaxRAMPercentage` (ou `-Xmx`) | Évite que 5 JVM réservent chacune un heap surdimensionné |

#### 2. Accès aux données

| Constat (dans le code) | Piste | Bénéfice attendu |
|---|---|---|
| `getAllPatients()` fait un `findAll()` **sans pagination** — [PatientService.java:21](patient-service/src/main/java/com/abernathy/patient/service/PatientService.java) | Introduire `Pageable` / pagination sur `GET /patient/all` | Moins d'objets chargés en mémoire et sérialisés à mesure que la base grandit |
| `ddl-auto: update` — [application.yml:22](patient-service/src/main/resources/application.yml) | Migrations versionnées (Flyway/Liquibase) + `validate` en prod | Évite le scan/diff du schéma à chaque démarrage |

#### 3. Communication inter-services

| Constat (dans le code) | Piste | Bénéfice attendu |
|---|---|---|
| `assess()` enchaîne **2 appels HTTP séquentiels** (patient puis notes), **via la gateway** — [AssessmentService.java:37](assessment-service/src/main/java/com/abernathy/assessment/service/AssessmentService.java) | Paralléliser les deux appels ; envisager un cache court sur des données peu changeantes | Moins de round-trips réseau et latence réduite par évaluation |

#### 4. Code & mémoire (micro-optimisations)

| Constat (dans le code) | Piste | Bénéfice attendu |
|---|---|---|
| `reduce("", (a, b) -> a + " " + b)` concatène les notes → une nouvelle `String` à chaque étape (O(n²) en allocations) — [AssessmentService.java:60](assessment-service/src/main/java/com/abernathy/assessment/service/AssessmentService.java) | `Collectors.joining(" ")` (ou `StringBuilder`) | Moins d'objets temporaires, moins de pression sur le GC |
| `term.toLowerCase()` recalculé pour chaque terme **à chaque appel** — [AssessmentService.java:65](assessment-service/src/main/java/com/abernathy/assessment/service/AssessmentService.java) | Pré-calculer une constante `TRIGGER_TERMS` déjà en minuscules | Supprime un travail répété inutile à chaque évaluation |

#### 5. Logs & observabilité

| Constat (dans le code) | Piste | Bénéfice attendu |
|---|---|---|
| `show-sql: true` — [application.yml:23](patient-service/src/main/resources/application.yml) | `false` en production, niveau de log `WARN`/`ERROR` | Supprime l'I/O et la construction de chaînes pour chaque requête SQL |

### Ce qui est déjà « green » dans le projet

Un regard critique doit aussi reconnaître les bonnes pratiques **déjà en place** :

- ✅ **Builds Docker multi-stage** : les outils de build (Maven, JDK) ne sont pas embarqués dans l'image finale.
- ✅ [`.dockerignore`](.dockerignore) : contexte de build réduit (pas de `target/`, `.git/`, etc.).
- ✅ Couche `dependency:go-offline` en cache dans les Dockerfiles : moins de téléchargements réseau aux rebuilds.
- ✅ **Validation JWT stateless** sur les resource servers : pas de store de sessions à maintenir en mémoire.
- ✅ **Thymeleaf (rendu serveur)** : pas de gros bundle JavaScript à charger côté client.
- ✅ **MongoDB** pour les notes non structurées : stockage adapté au besoin.

### Références

- OpenClassrooms — *Appliquez les principes du Green IT dans votre entreprise* (chapitre « Réduisez l'empreinte écologique de votre site web »)
- Scitepress — *How Green Are Java Best Coding Practices?*
- Institut du Numérique Responsable — *Green code : écrivez du code vert !*
