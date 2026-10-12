package lilifw.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.context.ApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import lilifw.annotation.RequestParam;

/**
 * Coeur du "binding" : transforme les champs d'un formulaire (parametres de la
 * requete HTTP) en arguments de la methode d'un controleur.
 *
 * <p>Pour chaque parametre de la methode :</p>
 * <ul>
 *   <li>un {@code ApplicationContext} ou un {@code HttpServletRequest} est
 *       injecte automatiquement par le framework ;</li>
 *   <li>sinon la valeur du champ de formulaire portant le meme nom est
 *       convertie vers le type du parametre. Le nom du champ est donne par
 *       {@code @RequestParam("nom")} ou, a defaut, par le nom du parametre Java
 *       (compilation avec {@code -parameters}).</li>
 * </ul>
 *
 * <p>Seules les donnees "simples" sont supportees : primitifs, wrappers,
 * String, enums, BigDecimal/BigInteger. Si un parametre attend un objet (POJO),
 * ou si la valeur recue ressemble a un objet/tableau JSON, une
 * {@link BindingException} est levee.</p>
 */
public class DataBinder {

    private DataBinder() {
    }

    /**
     * Construit le tableau d'arguments a passer a la methode du controleur en
     * lisant les champs du formulaire dans la requete.
     *
     * @param method  methode du controleur a invoquer
     * @param request requete HTTP contenant les champs du formulaire
     * @param ctx     contexte Spring eventuellement injecte (peut etre null)
     * @return les arguments, dans l'ordre des parametres de la methode
     * @throws BindingException si une donnee est un objet, si la conversion
     *                          echoue ou si un champ obligatoire est absent
     */
    public static Object[] bind(Method method, HttpServletRequest request, ApplicationContext ctx)
            throws BindingException {

        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            // Objets fournis par le framework : pas de binding depuis le formulaire.
            if (ApplicationContext.class.equals(type)) {
                arguments[i] = ctx;
                continue;
            }
            if (HttpServletRequest.class.equals(type)) {
                arguments[i] = request;
                continue;
            }

            // Si le parametre attend un objet, on ne peut pas le remplir depuis un
            // formulaire : on leve une exception.
            if (!isSimpleType(type)) {
                throw new BindingException("Le parametre '" + displayName(parameter) + "' de la methode "
                        + method.getName() + " attend un objet (" + type.getName()
                        + ") : seules les donnees simples sont supportees par le binding.");
            }

            String name = resolveName(parameter);
            String value = request.getParameter(name);

            if (value == null) {
                // Un primitif ne peut jamais valoir null : il est obligatoire.
                if (type.isPrimitive() || isRequired(parameter)) {
                    throw new BindingException("Le champ obligatoire '" + name + "' est absent de la requete (methode "
                            + method.getName() + "). Nommez-le avec @RequestParam(\"nom\") ou compilez les controleurs avec -parameters.");
                }
                arguments[i] = null;
                continue;
            }

            // Une valeur qui ressemble a un objet/tableau JSON n'est pas une donnee
            // simple : on leve une exception (sauf si le type attendu est du texte).
            if (!isTextType(type) && looksLikeObject(value)) {
                throw new BindingException("Le champ '" + name
                        + "' contient une valeur objet : seules les donnees simples sont supportees par le binding.");
            }

            arguments[i] = convert(value, type, name);
        }

        return arguments;
    }

    /** Types "simples" pouvant etre remplis depuis un formulaire. */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type.equals(String.class)
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || type.equals(Boolean.class)
                || type.equals(Character.class);
    }

    /** Types texte : une valeur JSON y est acceptee telle quelle. */
    private static boolean isTextType(Class<?> type) {
        return type.equals(String.class) || CharSequence.class.isAssignableFrom(type);
    }

    /** Nom du champ de formulaire associe au parametre. */
    private static String resolveName(Parameter parameter) throws BindingException {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }
        throw new BindingException("Impossible de determiner le nom du champ pour un parametre de type "
                + parameter.getType().getSimpleName()
                + " : nommez-le avec @RequestParam(\"nom\") ou compilez les controleurs avec -parameters.");
    }

    /** false uniquement si @RequestParam(required = false). */
    private static boolean isRequired(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        return annotation != null && annotation.required();
    }

    private static String displayName(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }
        return parameter.isNamePresent() ? parameter.getName() : parameter.getType().getSimpleName();
    }

    /** Detecte une valeur qui a la forme d'un objet ({...}) ou tableau ([...]) JSON. */
    private static boolean looksLikeObject(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        return (first == '{' && last == '}') || (first == '[' && last == ']');
    }

    private static Object convert(String value, Class<?> type, String name) throws BindingException {
        try {
            if (isTextType(type)) {
                return value;
            }
            if (type.isEnum()) {
                return toEnum(type, value);
            }
            if (type.equals(boolean.class) || type.equals(Boolean.class)) {
                return parseBoolean(value);
            }
            if (type.equals(char.class) || type.equals(Character.class)) {
                if (value.length() != 1) {
                    throw new BindingException("Le champ '" + name + "' doit contenir un seul caractere.");
                }
                return value.charAt(0);
            }
            if (type.equals(byte.class) || type.equals(Byte.class)) {
                return Byte.valueOf(value.trim());
            }
            if (type.equals(short.class) || type.equals(Short.class)) {
                return Short.valueOf(value.trim());
            }
            if (type.equals(int.class) || type.equals(Integer.class)) {
                return Integer.valueOf(value.trim());
            }
            if (type.equals(long.class) || type.equals(Long.class)) {
                return Long.valueOf(value.trim());
            }
            if (type.equals(float.class) || type.equals(Float.class)) {
                return Float.valueOf(value.trim());
            }
            if (type.equals(double.class) || type.equals(Double.class)) {
                return Double.valueOf(value.trim());
            }
            if (type.equals(BigDecimal.class)) {
                return new BigDecimal(value.trim());
            }
            if (type.equals(BigInteger.class)) {
                return new BigInteger(value.trim());
            }
        } catch (BindingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BindingException("Impossible de convertir la valeur '" + value + "' du champ '" + name
                    + "' vers le type " + type.getSimpleName() + ".", e);
        }
        // Ne devrait pas arriver : le type est filtre par isSimpleType().
        throw new BindingException("Type non supporte pour le champ '" + name + "' : " + type.getName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object toEnum(Class<?> type, String value) {
        return Enum.valueOf((Class<? extends Enum>) type, value.trim());
    }

    /** Conversion booleenne tolerante (utile pour les cases a cocher HTML). */
    private static boolean parseBoolean(String value) {
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("on") || v.equals("yes") || v.equals("1");
    }
}
