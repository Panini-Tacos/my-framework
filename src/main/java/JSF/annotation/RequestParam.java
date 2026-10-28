package JSF.annotation;

import java.lang.annotation.*;

/**
 * Lie un parametre de methode d'un controleur a un champ de formulaire
 * (c'est-a-dire un parametre de la requete HTTP).
 *
 * <p>C'est le principe du "binding" : les champs d'un formulaire deviennent les
 * arguments de la methode du controleur.</p>
 *
 * <p>Exemple :</p>
 * <pre>
 * &#64;UrlMapping(value = "/inscription", method = "POST")
 * public ModelAndView inscrire(&#64;RequestParam("nom") String nom,
 *                              &#64;RequestParam("age") int age) {
 *     ...
 * }
 * </pre>
 *
 * <p>Si {@code value()} est vide, le nom du parametre Java est utilise. Cela
 * necessite de compiler les controleurs avec l'option {@code -parameters},
 * sinon le nom est perdu par la compilation.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {

    /** Nom du champ de formulaire a lire dans la requete. */
    public String value() default "";

    /** true si le champ doit obligatoirement etre present dans la requete. */
    public boolean required() default true;
}
