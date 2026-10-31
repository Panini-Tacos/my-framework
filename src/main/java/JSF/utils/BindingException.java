package JSF.utils;

/**
 * Levee lorsque le "binding" d'un formulaire vers les arguments d'une methode
 * de controleur echoue.
 *
 * <p>Cas d'erreur : un parametre attend un objet (POJO) alors que seules les
 * donnees simples sont supportees, la valeur recue ressemble a un objet JSON,
 * une conversion de type est impossible, ou un champ obligatoire est absent.</p>
 */
public class BindingException extends Exception {

    private static final long serialVersionUID = 1L;

    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
