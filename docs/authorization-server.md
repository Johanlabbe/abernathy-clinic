# Choix du serveur d'autorisation

## Le contexte

L'application Abernathy Clinic est découpée en plusieurs microservices Spring Boot (patients, notes, évaluation du risque diabète), exposés derrière une gateway et orchestrés par Docker Compose. On manipule des données médicales, donc on est forcément concerné par le RGPD et le secret médical : impossible de laisser les endpoints ouverts, il faut authentifier les utilisateurs et contrôler ce qu'ils ont le droit de faire.

La question est donc : quel serveur d'autorisation brancher devant tout ça ?

## Petite mise au point

Avant de comparer quoi que ce soit, il y a une confusion qui revient souvent et qu'il faut lever. OAuth 2.0 n'est pas un produit, c'est un protocole [la RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749). OpenID Connect est une couche d'authentification posée par-dessus. Keycloak, Auth0, Okta ou Spring Authorization Server sont des **implémentations** de ces protocoles.

Autrement dit, on ne choisit pas "entre OAuth 2.0 et Keycloak", on choisit quel produit va parler OAuth 2.0 / OIDC pour nous.

## Les candidats que j'ai regardés

### Keycloak

C'est la solution open-source de Red Hat. Elle s'installe via une image Docker, donc elle vient se poser naturellement à côté des autres services dans le **docker-compose.yml**. Elle propose une console d'admin web pour gérer les utilisateurs, les rôles, les clients, et s'intègre très bien avec Spring Security (le starter **oauth2-resource-server** valide les JWT en quelques lignes). On peut aussi brancher un LDAP ou un Active Directory derrière si besoin.

Le revers de la médaille : c'est un service à maintenir, et son empreinte mémoire n'est pas négligeable.

### Auth0 / Okta

Ce sont des services SaaS, très bien finis, avec une doc agréable et un setup très rapide. Mais on paye dès qu'on dépasse quelques milliers d'utilisateurs, et surtout les données d'authentification partent chez un prestataire externe (souvent hébergé hors UE). Pour des données médicales, ça devient compliqué côté RGPD.

### Spring Authorization Server

Très tentant sur le papier puisque le projet est 100 % Spring. Le souci, c'est qu'il fournit juste la brique protocolaire : pas d'interface d'admin, pas de gestion d'utilisateurs prête à l'emploi, pas de fédération. Il faudrait coder tout ça, et on réinventerait pour pas grand-chose ce que Keycloak fait déjà.

### AWS Cognito / Azure AD B2C

Bons produits, mais ils ont du sens quand on est déjà sur le cloud du fournisseur. Le projet est conteneurisé et indépendant d'un provider — partir sur Cognito ajouterait une dépendance externe sans contrepartie évidente.

## Ce que j'ai retenu : Keycloak

Plusieurs raisons m'ont fait pencher vers Keycloak.

D'abord, le critère qui pèse le plus pour ce projet, c'est la **confidentialité des données de santé**. Avec Keycloak auto-hébergé, les identifiants et les tokens restent dans notre infra, on garde la maîtrise. C'est beaucoup plus simple à défendre vis-à-vis du RGPD qu'un SaaS américain.

Ensuite, **l'intégration est naturelle**. On ajoute un service dans **docker-compose.yml**, on configure la gateway et chaque microservice en resource server (juste un **issuer-uri** à pointer vers Keycloak), et Spring Security se charge du reste — validation de signature, expiration, extraction des rôles. Pas de glue code exotique.

Côté **coût**, Keycloak est gratuit. Pour un projet pédagogique c'est anecdotique, mais sur un vrai déploiement on évite la facturation par utilisateur actif.

Enfin, c'est un produit **mature**, soutenu par Red Hat, largement déployé en production, avec une vraie communauté et beaucoup de retours d'expérience. Si demain la clinique veut brancher son annuaire LDAP ou faire du SSO avec un autre système hospitalier, c'est natif.

## Ce que ça donne concrètement

L'idée serait d'ajouter un service **keycloak** dans le **docker-compose.yml** (image officielle **quay.io/keycloak/keycloak**), de créer un realm **abernathy** avec les rôles métier (praticien, organisateur), et de déclarer un client pour le front-end utilisant le flow Authorization Code avec PKCE. Le front récupère un JWT, la gateway le valide, le transmet aux microservices qui le valident à leur tour et appliquent leurs règles d'accès via **@PreAuthorize**.

Rien d'exotique, c'est le pattern classique d'une archi microservices sécurisée — et c'est justement ce qui en fait un bon choix.
