# Explication détaillée du code, fichier par fichier

## `src/main/java/lilifw/annotation/Controller.java`

Définit l'annotation `@Controller`.
- `@Retention(RetentionPolicy.RUNTIME)` : l'annotation est lisible à l'exécution via la réflexion (nécessaire pour le scan).
- `// @Target(ElementType.PACKAGE)` est commenté : pas de restriction de cible, on l'utilise sur les classes.
- Elle sert de simple marqueur : une classe avec `@Controller` sera considérée comme contrôleur lors du scan.

## `src/main/java/lilifw/annotation/UrlMapping.java`

Définit l'annotation `@UrlMapping` (applicable aux méthodes, visible à l'exécution).
- `String value()` : l'URL associée à la méthode (ex : `/hello`).
- `String method()` : la méthode HTTP (ex : `"GET"`, `"POST"`).
- Exemple : `@UrlMapping(value="/hello", method="GET")`.

## `src/main/java/lilifw/annotation/WebAPI.java`

Définit l'annotation `@WebAPI` (applicable aux méthodes, visible à l'exécution).
- `String api()` : l'URL de l'API REST.
- `boolean toJSON()` : `true` pour retourner du JSON, `false` pour du texte brut.

## `src/main/java/lilifw/annotation/RequestParam.java`

Définit l'annotation `@RequestParam` (applicable aux paramètres, visible à l'exécution). C'est le point d'entrée du binding.
- `String value()` : nom du champ de formulaire à lire dans la requête. Si vide, le nom du paramètre Java est utilisé (nécessite la compilation avec `-parameters`).
- `boolean required()` : `true` (par défaut) si le champ doit obligatoirement être présent.
- Exemple : `public ModelAndView inscrire(@RequestParam("nom") String nom, @RequestParam("age") int age)` : les champs `nom` et `age` du formulaire deviennent les arguments de la méthode.

## `src/main/java/lilifw/LiliFWContextListener.java`

Implémente `ServletContextListener` pour être notifié au démarrage/arrêt de l'application.

- `contextInitialized(...)` :
  - `event.getServletContext()` : récupère le contexte global de l'application web.
  - `context.getInitParameter("package")` : lit le paramètre d'init `package` du `web.xml` (package racine des contrôleurs).
  - Création des deux maps de routage : `urlToMethods` (@UrlMapping) et `urlToApiMethods` (@WebAPI).
  - `Util.scanAllAnnotedControllers(...)` : scanne les classes et remplit les maps.
  - `context.setAttribute(...)` : stocke les maps pour les rendre accessibles au FrontControllerServlet.
  - En cas d'erreur (conflit d'URL par ex.), une RuntimeException arrête le démarrage.
- `contextDestroyed(...)` : rien à faire à l'arrêt.

## `src/main/java/lilifw/Main.java`

Classe de test avec un `main()`.
- Instancie la map `contMethods` et la parcourt pour afficher chaque URL et invoquer `print()` dessus.
- Le scan (`Util.scanAllAnnotedControllers`) est commenté : la map reste vide et rien n'est affiché.
- Sert de brouillon pour tester le scan hors serveur.

## `src/main/java/lilifw/FrontControllerServlet.java`

Classe centrale du framework : toutes les requêtes GET/POST passent par elle.

### Attributs
- `urlToMethods` : routes classiques (@UrlMapping).
- `urlToApiMethods` : routes API (@WebAPI).
- `ctx` : contexte Spring (`ApplicationContext`), injecté aux méthodes de contrôleurs qui le demandent.

### `init()`
- Récupère `urlToMethods` et `urlToApiMethods` depuis le ServletContext (remplies par le listener).
- Tente de récupérer le contexte Spring via `WebApplicationContextUtils` ; en cas d'échec, `ctx = null` (le framework fonctionne sans Spring).

### `doGet` / `doPost`
- Délèguent simplement à `processRequest`, en enveloppant les exceptions dans une `ServletException`.

### `processRequest(request, response)`

1. Calcul de l'URL relative : `uri.substring(contextPath.length())`.
2. Si URL nulle ou `/` : on met la map des routes dans la requête et on forward vers `/index.jsp` (liste des routes).
3. Sinon on stocke l'URL dans l'attribut `url` de la requête.
4. Si l'URL correspond à une API (`urlToApiMethods.get(new URLMethod(url, method))` non null) :
   - Instanciation du contrôleur par réflexion (`Class.forName` + constructeur sans argument).
   - Construction des paramètres via `DataBinder.bind(methode, request, ctx)` : injection automatique de `ApplicationContext` et `HttpServletRequest`, et binding des champs de formulaire vers les paramètres simples (`@RequestParam` / nom du paramètre).
   - Invocation de la méthode par réflexion.
   - Si `toJson == true` : écrit le résultat sérialisé en JSON via `JsonSerializer`, sinon `String.valueOf(result)` en texte brut.
   - En cas d'exception (dont `BindingException`) : HTTP 500 + JSON `{"error": "message"}`.
5. Sinon, recherche dans `urlToMethods`. Si aucune correspondance :
   - Calcul d'un préfixe (dossier parent de l'URL).
   - Collecte des routes commençant par ce préfixe pour aide au diagnostic.
   - Forward vers `/route.jsp` avec `notFoundUrl` et `matchingRoutes`.
6. Si correspondance trouvée :
   - Même logique d'instanciation/injection/invocation par réflexion que pour l'API (arguments construits par `DataBinder.bind(...)`). Si un paramètre attend un objet, `DataBinder` lève une `BindingException`.
   - Si le résultat est un `ModelAndView` : chaque entrée de `mv.getData()` devient un attribut de la requête, puis forward vers `/<view>.jsp`.
   - Sinon : forward vers `/route.jsp` avec le nom du contrôleur et de la méthode.

### `getUrlToMethods()`
- Simple getter utilisé par `index.jsp` pour lister les routes.

## `src/main/java/lilifw/utils/Util.java`

`scanAllAnnotedControllers(packageLocation, urlToMethods, urlToApiMethods)` :

1. Transforme le package en chemin : `packageLocation.replace(".", "/") + "/controllers"`.
2. Récupère la ressource via le ClassLoader ; si null, affiche un message et retourne.
3. Parcourt les fichiers du dossier (`dossier.listFiles()`).
4. Pour chaque fichier `.class` :
   - Reconstruit le nom complet de la classe et fait `Class.forName(className)`.
   - Si la classe a `@Controller`, parcourt ses méthodes déclarées :
     - `@UrlMapping` : lit `value()` (URL) et `method()` (verbe HTTP), crée la clé `URLMethod`, vérifie l'absence de conflit (sinon exception), et ajoute `new ControllerMethods(nomClasse, method)` dans `urlToMethods`.
     - `@WebAPI` : lit `api()` (URL), `toJSON()`, crée la clé `URLMethod(url, "GET")`, vérifie les conflits (dans les APIs et les routes classiques), puis ajoute `new ApiMethods(nomClasse, method, toJson)` dans `urlToApiMethods`.
   - Pour toute autre classe (sans `@Controller`), on passe son tour.

## `src/main/java/lilifw/utils/ControllerMethods.java`

Conteneur pour une route `@UrlMapping` :
- `controllerName` : nom complet de la classe contrôleur.
- `Methode` : la `Method` Java à invoquer.
- Constructeur, getters/setters, et `print()` qui affiche `nomClasse + nomMethode`.

## `src/main/java/lilifw/utils/ApiMethods.java`

Conteneur pour une méthode `@WebAPI` :
- `controller` : nom complet de la classe contrôleur.
- `methode` : la `Method` Java à invoquer.
- `toJson` : true si la réponse doit être en JSON.
- Constructeur, getters/setters.

## `src/main/java/lilifw/utils/URLMethod.java`

Clé de routage pour les HashMap :
- `url` : l'URL de la route.
- `Method` : le verbe HTTP ("GET", "POST"...).
- Constructeur, getters/setters.
- `equals` / `hashCode` basés sur `url` + `Method` : permet d'utiliser cette classe comme clé de `HashMap`.

## `src/main/java/lilifw/utils/ModelAndView.java`

Représente le couple vue + données pour une page JSP :
- `view` : nom de la vue (sans extension `.jsp`).
- `data` : map attribut-clé → valeur à exposer à la JSP.
- Constructeur vide et constructeur `(view, data)` qui met l'objet dans `data.put("data", data)`.
- `addObject(name, value)` : ajoute un attribut.
- `getData()` / getters-setters.

## `src/main/java/lilifw/utils/JsonSerializer.java`

Sérialiseur JSON maison, récursif.

- `serialize(Object)` : point d'entrée, délègue à `toJson` avec un set de visite pour détecter les cycles.
- `toJson(object, visited)` :
  - `null` → `"null"`.
  - types "primitifs" (int, Number, Boolean, Character) → valeur directe.
  - `Enum` → nom entre guillemets.
  - `CharSequence` (String...) → chaîne échappée.
  - `Collection` → tableau JSON.
  - tableau Java → tableau JSON (via `Array.getLength`/`Array.get`).
  - `Map` → objet JSON (`"clé": valeur`).
  - Sinon objet quelconque → `toJsonObject`.
- `isPrimitiveLike(type)` : teste si le type est un primitif, un Number, Boolean ou Character.
- `toPrimitive(object)` : Character → chaîne, sinon `toString()`.
- `toJsonArray(...)` : parcourt le tableau et joint les éléments par des virgules.
- `toJsonMap(...)` : vérifie les cycles (`checkCycle`), puis sérialise chaque paire clé/valeur.
- `toJsonObject(...)` : parcourt les méthodes publiques de la classe et utilise celles qui ressemblent à des getters (`getX()` sans paramètres, ou `isX()` retournant boolean). La valeur retournée par chaque getter est sérialisée récursivement.
- `getterName(Method)` : détermine le nom de la propriété à partir du nom de la méthode ; ignore `getClass()`, exige 0 paramètre et un type de retour non void.
- `checkCycle(...)` : lève une exception si l'objet est déjà dans `visited` (référence circulaire).
- `quote(String)` : entoure la chaîne de guillemets et échappe `"`, `\`, `\n`, `\r`, `\t` et les caractères de contrôle (`\uXXXX`).

## `src/main/java/lilifw/utils/DataBinder.java`

Cœur du binding : transforme les champs d'un formulaire (paramètres de la requête) en arguments de la méthode d'un contrôleur.

- `bind(Method method, HttpServletRequest request, ApplicationContext ctx)` : point d'entrée. Renvoie un tableau d'arguments, un par paramètre de la méthode.
  - Pour chaque paramètre :
    - Si le type est `ApplicationContext` → injecte `ctx` ; si c'est `HttpServletRequest` → injecte `request`.
    - Si le type n'est pas "simple" (objet/POJO) → lève `BindingException` : le binding ne supporte que les données simples.
    - Sinon, lit `request.getParameter(nom)` où `nom` vient de `@RequestParam("nom")` ou du nom du paramètre Java (`resolveName`, nécessite `-parameters`).
    - Si le champ est absent : lève `BindingException` si c'est un primitif ou `@RequestParam(required = true)` ; sinon passe `null`.
    - Si la valeur ressemble à un objet/tableau JSON (`{...}`/`[...]`) et que le type n'est pas du texte → lève `BindingException`.
    - Sinon convertit la valeur avec `convert(...)`.
- `isSimpleType(Class)` / `isTextType(Class)` : déterminent les types acceptés (primitifs, wrappers, `String`, enums, `BigDecimal`/`BigInteger`).
- `resolveName(Parameter)` : nom du champ (`@RequestParam`, sinon nom du paramètre). Lève `BindingException` si le nom ne peut pas être déterminé.
- `isRequired(Parameter)` : `false` uniquement si `@RequestParam(required = false)`.
- `looksLikeObject(String)` : détecte une valeur de la forme `{...}` ou `[...]`.
- `convert(value, type, name)` : conversion vers le type cible (`String`, enum, booléen, caractère, tous les nombres, `BigDecimal`/`BigInteger`) ; toute erreur est encapsulée dans une `BindingException`.
- `parseBoolean(String)` : conversion tolérante (`true`/`on`/`yes`/`1`), pratique pour les cases à cocher HTML.

## `src/main/java/lilifw/utils/BindingException.java`

Exception (checked) levée par `DataBinder` lorsqu'un binding échoue : donnée objet, valeur objet dans un champ simple, conversion impossible ou champ obligatoire absent. Deux constructeurs : message seul, ou message + cause.

## `test.sh`

Script de test : compile tous les `.java` du projet avec `javac -parameters -Xlint -cp "lib/servlet-api.jar"` dans le dossier `test`, exécute `lilifw.Main` avec le classpath `lib/*.jar:test`, puis supprime le dossier `test`. L'option `-parameters` conserve les noms des paramètres pour le binding automatique.

## `jar.sh`

Script de build :
- Supprime `out` et le recrée.
- Compile les sources avec `javac -parameters -Xlint -cp "lib/*"` dans `out`.
- En cas d'erreur de compilation, affiche le message et sort.
- Crée `lilifw.jar` avec `jar cf`, le déplace dans `output/`.
- Copie le jar dans `../TEST_liliFW/lib/` (application de test).
- Supprime le dossier `out`.

## `lib/`

- `servlet-api.jar` : API Servlet Jakarta (classes `HttpServlet`, `HttpServletRequest`, etc.).
- `spring-*.jar` : jars Spring (context, core, web, beans, expression, aop, jcl) utilisés pour l'injection de `ApplicationContext` dans les méthodes de contrôleurs.
