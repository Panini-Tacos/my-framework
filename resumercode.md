# Résumé fichier par fichier

| Fichier | Rôle |
|---|---|
| `test.sh` | Compile tous les `.java` avec `javac` et lance `lilifw.Main` pour un test rapide. |
| `jar.sh` | Compile les sources, crée `lilifw.jar` dans `output/` et le copie dans `../TEST_liliFW/lib/`. |
| `.gitattributes` | Configuration Git (normalisation des fins de ligne). |
| `lib/servlet-api.jar` | API Servlet (Jakarta) nécessaire à la compilation. |
| `lib/spring-*.jar` | Bibliothèques Spring (core, beans, context, web, expression, aop, jcl) pour l'injection du `ApplicationContext`. |
| `src/main/java/lilifw/annotation/Controller.java` | Annotation `@Controller` : marque une classe comme contrôleur scannable (rétention runtime). |
| `src/main/java/lilifw/annotation/UrlMapping.java` | Annotation `@UrlMapping` : associe une méthode à une URL (`value`) et une méthode HTTP (`method`). |
| `src/main/java/lilifw/annotation/WebAPI.java` | Annotation `@WebAPI` : associe une méthode à une URL d'API (`api`) et indique si le résultat est en JSON (`toJSON`). |
| `src/main/java/lilifw/annotation/RequestParam.java` | Annotation `@RequestParam` : lie un paramètre de méthode à un champ de formulaire (`value`, `required`) — principe du binding. |
| `src/main/java/lilifw/LiliFWContextListener.java` | Listener de démarrage : lit les paramètres `package` et `uploadPath` du web.xml, scanne les contrôleurs, configure le dossier d'upload (`Uploads`) et stocke les maps de routes dans le `ServletContext`. |
| `src/main/java/lilifw/FrontControllerServlet.java` | Servlet frontal (`@MultipartConfig` pour l'upload) : route chaque requête GET/POST vers la méthode de contrôleur ou l'API correspondante, sérialise en JSON/texte, ou forwarde vers la JSP/`ModelAndView`. |
| `src/main/java/lilifw/Main.java` | Classe de test avec un `main()` qui affiche les URLs et méthodes annotées détectées (scan actuellement commenté). |
| `src/main/java/lilifw/utils/Util.java` | `scanAllAnnotedControllers` : parcourt le package `controllers`, détecte `@Controller`, `@UrlMapping`, `@WebAPI`, remplit les maps et détecte les conflits d'URL. |
| `src/main/java/lilifw/utils/ControllerMethods.java` | Stocke le nom de la classe contrôleur et la `Method` pour une route `@UrlMapping`. |
| `src/main/java/lilifw/utils/ApiMethods.java` | Stocke le nom de la classe contrôleur, la `Method` et le flag `toJson` pour une méthode `@WebAPI`. |
| `src/main/java/lilifw/utils/URLMethod.java` | Clé de routage (URL + méthode HTTP) avec `equals`/`hashCode` pour les HashMap. |
| `src/main/java/lilifw/utils/ModelAndView.java` | Couple vue + données : nom de la JSP (`view`) et map d'attributs (`data`) à injecter dans la requête. |
| `src/main/java/lilifw/utils/JsonSerializer.java` | Sérialiseur JSON maison : gère primitifs, chaînes, enum, collections, tableaux, maps, objets via getters, échappement et détection de cycles. |
| `src/main/java/lilifw/utils/DataBinder.java` | Cœur du binding : convertit les champs de formulaire en arguments de méthode (injection `ApplicationContext`/`HttpServletRequest`, données simples, objets/POJO remplis récursivement, listes, fichiers `UploadedFile`/`Part` en multipart), lève `BindingException` uniquement en cas de vrai échec. |
| `src/main/java/lilifw/utils/UploadedFile.java` | Fichier téléversé (`multipart/form-data`) encapsulant un `Part` : nom, type MIME, taille, lecture (`getBytes`/`getInputStream`) et chemin du `.txt` écrit automatiquement au binding (`getSavedPath()`/`getSavedName()`). `save()`/`saveTo(File)` permettent de ré-enregistrer. |
| `src/main/java/lilifw/utils/Uploads.java` | Stockage des fichiers téléversés : dossier variable (`uploadPath` du web.xml, sinon `uploads`), nom assaini, extension remplacée par `.txt` et nom unique (`photo.txt`, `photo-1.txt`...). |
| `src/main/java/lilifw/utils/BindingException.java` | Exception levée quand le binding échoue (conversion impossible, champ obligatoire absent, objet non instanciable). |
