# Mise en place de Keycloak — récapitulatif

Ce document récapitule l'intégration de **Keycloak** comme serveur d'autorisation OAuth2 / OpenID Connect dans Abernathy Clinic. Le choix de Keycloak est justifié dans [authorization-server.md](authorization-server.md) ; ce document décrit la **réalisation**.

---

## 1. Objectif

Remplacer la sécurité provisoire (HTTP Basic avec un utilisateur en mémoire `medecin:password123`, codé en dur dans le front et l'assessment-service) par une vraie chaîne d'authentification/autorisation centralisée :

- authentification des utilisateurs via Keycloak (login web) ;
- jetons **JWT** signés, validés par la gateway puis par chaque microservice ;
- autorisation **par rôle métier** (`@PreAuthorize`), adaptée à la confidentialité des données de santé.

---

## 2. Architecture cible

```
Navigateur ──login (Authorization Code + PKCE)──▶ Keycloak (localhost:8090)
   │
   ▼ session
Front-end (client OAuth2, :8082) ──Bearer JWT──▶ Gateway (resource server, :8080)
                                                   │ relaie le header Authorization
                          ┌────────────────────────┼─────────────────────────┐
                          ▼                        ▼                         ▼
                   patient-service           note-service            assessment-service
                   (resource server)        (resource server)       (resource server)
                          ▲                                                  │
                          └──────── assessment propage le Bearer ────────────┘
```

| Composant | Rôle OAuth2 | Responsabilité |
|---|---|---|
| **front-end** | Client OIDC | Login (Authorization Code + PKCE), garde le token en session, le relaie à la gateway |
| **gateway** | Resource server (réactif) | Valide le JWT, exige l'authentification, relaie le header `Authorization` |
| **patient / note / assessment** | Resource servers (servlet) | Valident le JWT et appliquent les règles de rôle via `@PreAuthorize` |
| **assessment** (cas particulier) | Resource server + appelant | Propage le JWT entrant quand il rappelle la gateway |

---

## 3. Modèle d'autorisation (séparation RGPD)

Deux rôles *realm* : **`praticien`** et **`organisateur`**.

| Endpoint | Service | praticien | organisateur |
|---|---|:--:|:--:|
| `GET /patient/all`, `GET /patient/{id}` | patient | ✅ | ✅ |
| `POST /patient/add`, `PUT /patient/update/{id}` | patient | ❌ | ✅ |
| `GET /note/patient/{patId}`, `POST /note/add` | note | ✅ | ❌ |
| `GET /assessment/{patId}` | assessment | ✅ | ❌ |

Logique : les **données médicales** (notes, évaluation du risque) sont réservées au `praticien` ; la gestion de l'**état civil** (création/modification de patients) à l'`organisateur` ; la **lecture** de la fiche patient est partagée.

---

## 4. Le service Keycloak (Docker)

Ajouté dans [docker-compose.yml](../docker-compose.yml) :

- image officielle `quay.io/keycloak/keycloak:26.6.2` ;
- démarrage en `start-dev --import-realm` (mode développement + import automatique du realm) ;
- exposé sur **`localhost:8090`** (port conteneur 8080) ;
- admin bootstrap `admin` / `admin` ;
- *healthcheck* sur l'endpoint `/health/ready` (port de management 9000) ;
- les autres services attendent que Keycloak soit *healthy* avant de démarrer.

### Import automatique du realm

Le fichier [keycloak/realm-abernathy.json](../keycloak/realm-abernathy.json) est monté dans le conteneur et importé au démarrage. Il définit **tout** de façon reproductible :

- le realm `abernathy` ;
- les rôles `praticien` et `organisateur` ;
- les utilisateurs de test (voir §8) ;
- le client `abernathy-front` : confidentiel, *Standard flow* (Authorization Code), **PKCE S256** imposé, redirect URIs vers `http://localhost:8082/...`.

> ⚠️ Toute modification faite ensuite **via la console web** est perdue à la recréation du conteneur (`docker compose down -v`). Pour la conserver, il faut la reporter dans le fichier JSON.

---

## 5. Le piège « issuer / hostname » en Docker (et sa résolution)

Le navigateur joint Keycloak en `localhost:8090`, mais les conteneurs le joignent en `keycloak:8080`. Or le claim `iss` du token doit être cohérent partout, sinon la validation échoue.

**Solution appliquée :**

- Keycloak épinglé sur l'URL publique : `KC_HOSTNAME=http://localhost:8090` → l'`iss` vaut **toujours** `http://localhost:8090/realms/abernathy`.
- `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true` → les appels *back-channel* des conteneurs (token, certs, userinfo) restent acceptés sur `keycloak:8080`.

Côté Spring :

- **Resource servers** : `issuer-uri` = URL publique (valide l'`iss`) **+** `jwk-set-uri` = URL interne `keycloak:8080` (récupère les clés). Les deux ensemble → clés récupérées en interne, `iss` validé sur l'URL publique, pas de discovery distante bloquante au démarrage.
- **Front-end (client)** : endpoints renseignés **manuellement** (pas d'`issuer-uri`) — `authorization-uri` en `localhost:8090` (navigateur), `token`/`jwks`/`userinfo` en `keycloak:8080` (back-channel).

---

## 6. Modifications par service

### POMs
- `gateway`, `patient`, `note`, `assessment` : ajout de `spring-boot-starter-oauth2-resource-server`.
- `front-end` : ajout de `spring-boot-starter-oauth2-client` (+ `thymeleaf-extras-springsecurity6`).

### Microservices (patient / note / assessment)
- `SecurityConfig` réécrit : suppression du HTTP Basic / utilisateur en mémoire, passage en `oauth2ResourceServer(jwt)`, session *stateless*, `@EnableMethodSecurity`.
- Classe `KeycloakRealmRoleConverter` : mappe le claim `realm_access.roles` de Keycloak en autorités Spring `ROLE_*` (pour que `hasRole('praticien')` fonctionne).
- `application.yml` : ajout de `spring.security.oauth2.resourceserver.jwt.{issuer-uri, jwk-set-uri}`.
- Annotations `@PreAuthorize` sur les contrôleurs selon la matrice du §3.

### assessment-service (propagation du token)
L'assessment rappelle la gateway (pour lire patient + notes). Il n'a pas d'identité propre : un `ClientHttpRequestInterceptor` (`RestClientConfig`) **propage le JWT entrant** lu dans le `SecurityContext`. Le Basic codé en dur a été supprimé.

### gateway
- `SecurityConfig` réactif (`SecurityWebFilterChain`) : `oauth2ResourceServer(jwt)`, toute requête authentifiée. La gateway relaie le header `Authorization` en aval par défaut.

### front-end
- `SecurityConfig` : `oauth2Login` avec **PKCE forcé** (via `OAuth2AuthorizationRequestCustomizers.withPkce()`), et **déconnexion RP-initiée** (`KeycloakLogoutSuccessHandler` — termine aussi la session côté Keycloak).
- `RestClientConfig` : un `RestClient` muni d'un `OAuth2ClientHttpRequestInterceptor` qui attache automatiquement le token de l'utilisateur connecté aux appels gateway.
- `FrontController` : suppression du Basic codé en dur (injection du `RestClient` configuré) + ajout d'une **route racine `/` → redirection vers `/patients`** (l'absence de cette route provoquait une page 404 « Whitelabel » au retour de login/logout).
- Templates : bandeau « Connecté en tant que … » + bouton **Déconnexion** sur la page patients.

---

## 7. Les flux d'authentification

**Login (navigateur) :** l'utilisateur non authentifié est redirigé vers Keycloak (Authorization Code + PKCE S256). Après saisie des identifiants, Keycloak renvoie un code, le front l'échange (back-channel) contre un JWT et ouvre une session.

**Appel d'API :** le front (et l'assessment) attachent le JWT en `Authorization: Bearer …`. La gateway valide et relaie ; chaque microservice revalide la signature + l'`iss` + l'expiration, puis applique ses `@PreAuthorize`.

**Déconnexion :** vide la session locale **et** la session SSO Keycloak, puis renvoie sur la page d'accueil.

---

## 8. Comptes

| Identifiant | Mot de passe | Rôle | Usage |
|---|---|---|---|
| `dr.house` | `password` | praticien | Patients + notes + évaluation |
| `secretary` | `password` | organisateur | Patients (lecture + création/modif) |
| `admin` | `admin` | — | Console d'administration Keycloak |

---

## 9. Démarrage & utilisation

```bash
# À la racine du projet
mvn clean package          # build des 5 modules
docker compose up --build  # lance toute la stack (dont Keycloak)
```

- Application : **http://localhost:8082** (ou `/patients`) → login Keycloak.
- Console Keycloak : **http://localhost:8090** → `admin` / `admin` → sélectionner le realm `abernathy`.

---

## 10. Vérifications effectuées

- Build des 5 modules : OK.
- Démarrage de la stack : tous les conteneurs *up*, realm `abernathy` importé, `iss` figé sur `localhost:8090`.
- Sans token → **401** sur la gateway.
- Tokens `abernathy-front` : contiennent bien `realm_access.roles`.
- Matrice d'accès conforme au §3 (lecture partagée, écriture patient réservée `organisateur`, médical réservé `praticien`).
- Le `200` sur `/assessment/{id}` pour `dr.house` confirme la **propagation du token** (assessment rappelle patient + note).
- Login web complet (Authorization Code + PKCE) validé pour `dr.house` **et** `secretary` ; racine `/` → `/patients`.

---

## 11. Pour passer en production (pistes)

Cette configuration est volontairement orientée **développement**. Avant un déploiement réel :

- remplacer `start-dev` par `start` + **HTTPS** ;
- utiliser une **base externe** (PostgreSQL) au lieu du H2 interne ;
- **externaliser les secrets** (secret client, admin) hors du dépôt (variables d'env / vault) ;
- identifiants admin robustes, politique de mot de passe, voire **2FA** ;
- éventuellement brancher un annuaire **LDAP / Active Directory** (User federation).
