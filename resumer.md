# Résumé du projet my-framework

Ce projet est un mini-framework web Java nommé **LiliFW** (inspiré de Spring MVC).
Il permet de déclarer des contrôleurs via des annotations (`@Controller`, `@UrlMapping`, `@WebAPI`),
de scanner automatiquement les classes contrôleurs au démarrage, puis de router les requêtes HTTP
vers les bonnes méthodes via un servlet frontal (Front Controller).

## Fichiers à la racine

- **test.sh** : script qui compile tous les `.java` avec `javac -parameters` (classpath `lib/servlet-api.jar`) dans le dossier `test`, lance `lilifw.Main`, puis supprime `test`. Sert à tester rapidement le projet.
- **jar.sh** : script de build qui compile les sources (`javac -parameters`) dans `out`, crée l'archive `lilifw.jar` dans `output/`, et la copie dans `../TEST_liliFW/lib/` pour être utilisée par une application de test. L'option `-parameters` conserve les noms des paramètres, nécessaires au binding automatique par nom.
- **lib/** : bibliothèques externes : API Servlet (`servlet-api.jar`) et jars Spring (`spring-core`, `spring-beans`, `spring-context`, `spring-web`, `spring-expression`, `spring-aop`, `spring-jcl`).
- **.gitattributes** : configuration Git (gestion des fins de ligne).

## src/main/java/lilifw/annotation/

Annotations du framework :

- **Controller.java** : annotation `@Controller` (rétention RUNTIME) marquant une classe comme contrôleur à scanner.
- **UrlMapping.java** : annotation `@UrlMapping` applicable aux méthodes. Définit `value()` (l'URL) et `method()` (méthode HTTP, ex : GET, POST). Sert à lier une méthode de contrôleur à une URL.
- **WebAPI.java** : annotation `@WebAPI` applicable aux méthodes. Définit `api()` (l'URL de l'API) et `toJSON()` (true si le résultat doit être sérialisé en JSON, false si texte brut).
- **RequestParam.java** : annotation `@RequestParam` applicable aux paramètres de méthode. Définit `value()` (nom du champ de formulaire, vide = nom du paramètre Java) et `required()` (champ obligatoire). C'est le point d'entrée du binding : les champs d'un formulaire deviennent les arguments de la méthode du contrôleur. Pour un paramètre objet, `value()` sert de préfixe aux champs (`@RequestParam("user")` → `user.nom`, `user.age`).

## src/main/java/lilifw/

- **LiliFWContextListener.java** : listener de cycle de vie Servlet (`ServletContextListener`). Au démarrage de l'application, il lit le paramètre d'init `package` du `web.xml`, scanne les contrôleurs annotés dans le sous-package `controllers` via `Util.scanAllAnnotedControllers`, et stocke les maps `urlToMethods` et `urlToApiMethods` comme attributs du `ServletContext`. Il lit aussi le paramètre d'init `uploadPath` (dossier de stockage des fichiers téléversés, variable — voir `Uploads`) et le publie en attribut `uploadPath`.
- **FrontControllerServlet.java** : servlet frontal qui reçoit toutes les requêtes GET/POST. Il est annoté `@MultipartConfig` : le conteneur accepte donc les formulaires `multipart/form-data` (upload de fichiers, 20 Mo/fichier et 30 Mo/requête par défaut, surchargeables dans le web.xml) sans configuration supplémentaire. Dans `init()`, il récupère les maps de routage depuis le contexte et tente d'obtenir le `ApplicationContext` Spring (sinon `ctx = null`). Dans `processRequest()` :
  - Si l'URL est `/`, il affiche `/index.jsp` avec la liste des routes.
  - Pour une requête multipart, il force la lecture du corps (`request.getParts()`) avant le binding afin que les fichiers restent disponibles.
  - Si l'URL correspond à une API (`urlToApiMethods`), il instancie le contrôleur, construit les arguments via `DataBinder.bind(...)` (injection du contexte Spring, de la requête, et binding des champs de formulaire **et des fichiers**), invoque la méthode et renvoie le résultat en JSON (via `JsonSerializer`) ou en texte brut selon `toJson`. En cas d'erreur, renvoie un JSON `{"error": ...}` avec statut 500.
  - Sinon, il cherche la méthode contrôleur dans `urlToMethods`. Si aucune ne correspond, il affiche `/route.jsp` avec les routes similaires (préfixe commun). Sinon, il construit les arguments via `DataBinder.bind(...)`, invoque la méthode ; si le résultat est un `ModelAndView`, il injecte les données dans la requête et forward vers la vue JSP correspondante, sinon affiche `/route.jsp`.
- **Main.java** : classe de test avec un `main()` qui (normalement) scanne les contrôleurs et affiche les URLs et méthodes trouvées. Actuellement le scan est commenté.

## src/main/java/lilifw/utils/

- **Util.java** : utilitaire de scan. `scanAllAnnotedControllers(packageLocation, urlToMethods, urlToApiMethods)` parcourt le dossier `<package>/controllers`, charge chaque classe `.class`, vérifie `@Controller`, puis pour chaque méthode annotée `@UrlMapping` (ajout dans `urlToMethods`, GET/POST) ou `@WebAPI` (ajout dans `urlToApiMethods`, GET seulement). Détecte les conflits d'URL entre routes et API en levant une exception.
- **ControllerMethods.java** : porte le nom de la classe contrôleur et la `Method` Java correspondant à une route `@UrlMapping`. Fournit getters/setters et une méthode `print()`.
- **ApiMethods.java** : porte le nom de la classe contrôleur, la `Method` et le booléen `toJson` pour une méthode annotée `@WebAPI`.
- **URLMethod.java** : clé de routage composée d'une URL et d'une méthode HTTP. Implémente `equals`/`hashCode` pour être utilisable comme clé de `HashMap`.
- **ModelAndView.java** : représente le couple vue + données. Contient le nom de la vue (`view`) et une map `data` d'attributs ; constructeur avec vue et objet, méthode `addObject`, getters.
- **JsonSerializer.java** : sérialiseur JSON fait maison. Gère `null`, primitifs, nombres, booléens, caractères, énumérations, `CharSequence`, collections, tableaux, `Map` et objets (via leurs getters `getX()`/`isX()`). Détecte les cycles (références circulaires) et échappe correctement les chaînes.
- **DataBinder.java** : cœur du binding. `bind(method, request, ctx)` construit le tableau d'arguments d'une méthode de contrôleur : injection automatique de `ApplicationContext` et de `HttpServletRequest`, puis lecture des champs de formulaire. Trois cas :
  - paramètre simple (primitifs, wrappers, `String`, enums, `BigDecimal`/`BigInteger`) : la valeur du champ de même nom est convertie vers le type ;
  - paramètre fichier (`UploadedFile` ou `Part`) : le fichier envoyé par un champ `<input type="file">` d'un formulaire `multipart/form-data` est bindé automatiquement (`@RequestParam("photo") UploadedFile photo`), y compris les collections (`UploadedFile[]`, `List<UploadedFile>`) et les champs fichier à l'intérieur des POJO (`user.photo`) ; un `UploadedFile` est enregistré **automatiquement au binding**, sous forme de `.txt`, dans le dossier de stockage configuré (`getSavedPath()` donne le chemin écrit) ;
  - paramètre objet (POJO) : il est instancié (constructeur sans argument) puis rempli avec les champs du formulaire, y compris les objet imbriqués (`adresse.ville`) et les listes/tableaux (`roles`, plusieurs valeurs) ;
  - paramètre `List`/tableau : rempli depuis plusieurs valeurs du même champ.
  Le nom d'un champ vient de `@RequestParam("nom")` ou du nom du paramètre Java (compilation `-parameters`). Une `BindingException` n'est levée que pour un vrai échec (conversion impossible, champ obligatoire absent, objet non instanciable).
- **BindingException.java** : exception levée par `DataBinder` lors d'un échec de binding (conversion impossible, champ obligatoire absent, objet sans constructeur sans argument).
- **UploadedFile.java** : fichier téléversé par un formulaire `multipart/form-data`, encapsulant un `Part` du conteneur. Expose le nom d'origine, le type MIME, la taille, `getBytes()`/`getInputStream()`, et le chemin du fichier écrit par le framework (`getSavedPath()`/`getSavedName()`/`getSavedFile()`). Au binding, le contenu est enregistré automatiquement (`.txt` dans le dossier configuré) ; un ré-enregistrement reste possible via `save()` (dossier configuré) ou `saveTo(File)` (dossier choisi). C'est le type à déclarer dans un paramètre de contrôleur pour recevoir un fichier (binding).
- **Uploads.java** : configuration et enregistrement des fichiers téléversés. Le dossier de stockage est **variable** : paramètre d'init `uploadPath` du `web.xml` (lu au démarrage par `LiliFWContextListener`), sinon le dossier `uploads` par défaut. `save(UploadedFile)`/`save(UploadedFile, File)` créent le dossier si besoin, assainissent le nom d'origine (pas de `../`, pas de séparateurs), remplacent l'extension par `.txt` (stockage simplifié, ex. `photo.png` → `photo.txt`) et génèrent un nom unique (`photo.txt`, `photo-1.txt`...) pour ne jamais écraser un fichier existant.
