package JSF.utils;

import java.io.IOException;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import org.springframework.context.ApplicationContext;

import JSF.annotation.RequestParam;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

public class DataBinder {

    private DataBinder() {
    }

    public static Object[] bind(Method method, HttpServletRequest request, ApplicationContext ctx)
            throws BindingException {

        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            if (ApplicationContext.class.equals(type)) {
                arguments[i] = ctx;
                continue;
            }
            if (HttpServletRequest.class.equals(type)) {
                arguments[i] = request;
                continue;
            }
            if (HttpSession.class.equals(type)) {
                // Cree la session si elle n'existe pas encore (comportement
                // attendu quand le controleur demande explicitement la session).
                arguments[i] = request.getSession(true);
                continue;
            }

            if (isSimpleType(type)) {
                arguments[i] = bindSimple(parameter, type, request);
            } else if (isFileType(type)) {

                arguments[i] = bindFile(parameter, type, request);
            } else if (isFileTypeCollection(type, parameter.getParameterizedType())) {
                arguments[i] = bindFileCollection(parameter, type, request);
            } else if (type.isArray() || isListType(type)) {
                arguments[i] = bindCollection(parameter, type, request);
            } else {

                arguments[i] = bindObject(parameter, type, request);
            }
        }

        return arguments;
    }

    // ------------------------------------------------------------------
    // Parametre simple
    // ------------------------------------------------------------------

    private static Object bindSimple(Parameter parameter, Class<?> type, HttpServletRequest request)
            throws BindingException {
        String name = resolveName(parameter);
        String value = request.getParameter(name);

        if (value == null) {

            if (type.isPrimitive() || isRequired(parameter)) {
                throw new BindingException("Le champ obligatoire '" + name + "' est absent de la requete.");
            }
            return null;
        }

        return convert(value, type, name);
    }

    // ------------------------------------------------------------------
    // Parametre liste / tableau
    // ------------------------------------------------------------------

    private static Object bindCollection(Parameter parameter, Class<?> type, HttpServletRequest request)
            throws BindingException {
        String name = resolveName(parameter);
        String[] values = request.getParameterValues(name);

        if (values == null) {
            if (isRequired(parameter)) {
                throw new BindingException("Le champ obligatoire '" + name + "' est absent de la requete.");
            }
            return null;
        }

        if (type.isArray()) {
            return convertArray(values, type.getComponentType(), name);
        }
        return convertList(values, listElementType(parameter.getParameterizedType()), name);
    }

    // ------------------------------------------------------------------
    // Parametre fichier (upload multipart)
    // ------------------------------------------------------------------

    private static Object bindFile(Parameter parameter, Class<?> type, HttpServletRequest request)
            throws BindingException {
        String name = resolveName(parameter);
        Part part = findPart(request, name);

        if (part == null || isBlankFilename(part)) {
            if (isRequired(parameter)) {
                throw new BindingException("Le champ obligatoire '" + name + "' ne contient aucun fichier.");
            }
            return null;
        }
        return toFileType(type, part, name);
    }

    private static Object bindFileCollection(Parameter parameter, Class<?> type, HttpServletRequest request)
            throws BindingException {
        String name = resolveName(parameter);
        Class<?> elementType = type.isArray()
                ? type.getComponentType()
                : listElementType(parameter.getParameterizedType());

        List<Part> parts = findParts(request, name);
        if (parts.isEmpty()) {
            if (isRequired(parameter)) {
                throw new BindingException("Le champ obligatoire '" + name + "' ne contient aucun fichier.");
            }
            return null;
        }
        return toFileTypeCollection(type, elementType, parts, name);
    }

    private static Part findPart(HttpServletRequest request, String name) throws BindingException {
        List<Part> parts = findParts(request, name);
        return parts.isEmpty() ? null : parts.get(0);
    }

    private static List<Part> findParts(HttpServletRequest request, String name) throws BindingException {
        List<Part> result = new ArrayList<>();
        if (!isMultipart(request)) {
            return result;
        }
        try {
            for (Part part : request.getParts()) {
                if (part.getName().equals(name) && !isBlankFilename(part)) {
                    result.add(part);
                }
            }
        } catch (IllegalStateException e) {
            throw new BindingException("Impossible de lire les fichiers de la requete : le servlet doit etre "
                    + "configure pour le multipart (annotation @MultipartConfig).", e);
        } catch (IOException | ServletException e) {
            throw new BindingException("Erreur lors de la lecture du champ fichier '" + name + "' : "
                    + e.getMessage(), e);
        }
        return result;
    }


    private static Object toFileType(Class<?> type, Part part, String name) throws BindingException {
        if (Part.class.equals(type)) {
            return part;
        }
        if (UploadedFile.class.equals(type)) {
            UploadedFile file = new UploadedFile(part);
            try {
                
                file.save();
            } catch (IOException e) {
                throw new BindingException("Impossible d'enregistrer le fichier '" + name + "' : "
                        + e.getMessage(), e);
            }
            return file;
        }
        throw new BindingException("Type non supporte pour le champ fichier '" + name + "' : "
                + type.getSimpleName() + " (utilisez UploadedFile ou Part).");
    }

    private static Object toFileTypeCollection(Class<?> collectionType, Class<?> elementType, List<Part> parts,
            String name) throws BindingException {
        if (!isFileType(elementType)) {
            throw new BindingException("Type non supporte pour le champ fichier '" + name + "' : "
                    + collectionType.getSimpleName() + " (utilisez UploadedFile ou Part).");
        }
        if (collectionType.isArray()) {
            Object array = Array.newInstance(elementType, parts.size());
            for (int i = 0; i < parts.size(); i++) {
                Array.set(array, i, toFileType(elementType, parts.get(i), name));
            }
            return array;
        }
        List<Object> files = new ArrayList<>();
        for (Part part : parts) {
            files.add(toFileType(elementType, part, name));
        }
        return files;
    }

    private static boolean isBlankFilename(Part part) {
        String submittedFileName = part.getSubmittedFileName();
        return submittedFileName == null || submittedFileName.isEmpty();
    }


    private static Object bindObject(Parameter parameter, Class<?> type, HttpServletRequest request)
            throws BindingException {
        boolean annotated = parameter.getAnnotation(RequestParam.class) != null;
        String prefix = objectPrefix(parameter, request);

        if (annotated && !isRequired(parameter) && !hasParametersWithPrefix(request, prefix)) {
            return null;
        }

        Object instance = instantiate(type);
        populate(instance, prefix, request);
        return instance;
    }

    /** Remplit recursivement un objet a partir des champs du formulaire. */
    private static void populate(Object target, String prefix, HttpServletRequest request) throws BindingException {
        for (Field field : allFields(target.getClass())) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                continue;
            }

            String name = field.getName();
            String fullName = prefix.isEmpty() ? name : prefix + "." + name;
            Class<?> fieldType = field.getType();

            if (isSimpleType(fieldType)) {
                String value = request.getParameter(fullName);
                if (value != null) {
                    writeProperty(target, field, convert(value, fieldType, fullName));
                }
            } else if (fieldType.isArray()) {
                String[] values = request.getParameterValues(fullName);
                if (values != null) {
                    writeProperty(target, field, convertArray(values, fieldType.getComponentType(), fullName));
                }
            } else if (isListType(fieldType)) {
                String[] values = request.getParameterValues(fullName);
                if (values != null) {
                    writeProperty(target, field, convertList(values, listElementType(field.getGenericType()), fullName));
                }
            } else if (isFileType(fieldType)) {
                // Champ fichier : photo, piece jointe... (formulaire multipart).
                Part part = findPart(request, fullName);
                if (part != null) {
                    writeProperty(target, field, toFileType(fieldType, part, fullName));
                }
            } else if (isFileTypeCollection(fieldType, field.getGenericType())) {
                // Champ fichier multiple ( UploadedFile[] / List<UploadedFile> ).
                Class<?> elementType = fieldType.isArray()
                        ? fieldType.getComponentType()
                        : listElementType(field.getGenericType());
                List<Part> parts = findParts(request, fullName);
                if (!parts.isEmpty()) {
                    writeProperty(target, field, toFileTypeCollection(fieldType, elementType, parts, fullName));
                }
            } else {
                // Objet imbrique : on ne le remplit que si des champs le concernent
                // (adresse.ville, adresse.codePostal, ...).
                if (!hasParametersWithPrefix(request, fullName)) {
                    continue;
                }
                Object nested = readProperty(target, field);
                if (nested == null) {
                    nested = instantiate(fieldType);
                    writeProperty(target, field, nested);
                }
                populate(nested, fullName, request);
            }
        }
    }

    /** Prefixe des champs d'un objet : @RequestParam("x") -> "x", sinon
     *  auto-detection du nom du parametre, sinon binding "plat". */
    private static String objectPrefix(Parameter parameter, HttpServletRequest request) throws BindingException {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }
        if (parameter.isNamePresent() && hasParametersWithPrefix(request, parameter.getName())) {
            return parameter.getName();
        }
        return "";
    }

    // ------------------------------------------------------------------
    // Conversions
    // ------------------------------------------------------------------

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
        // Ne devrait pas arriver : le type est filtre en amont.
        throw new BindingException("Type non supporte pour le champ '" + name + "' : " + type.getName());
    }

    private static Object convertArray(String[] values, Class<?> componentType, String name) throws BindingException {
        Object array = Array.newInstance(componentType, values.length);
        for (int i = 0; i < values.length; i++) {
            Array.set(array, i, convert(values[i], componentType, name));
        }
        return array;
    }

    private static List<Object> convertList(String[] values, Class<?> elementType, String name)
            throws BindingException {
        List<Object> list = new ArrayList<>();
        for (String value : values) {
            list.add(convert(value, elementType, name));
        }
        return list;
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

    // ------------------------------------------------------------------
    // Instanciation / lecture / ecriture
    // ------------------------------------------------------------------

    private static Object instantiate(Class<?> type) throws BindingException {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new BindingException("La classe " + type.getName()
                    + " ne possede pas de constructeur sans argument : impossible de la binder.", e);
        } catch (Exception e) {
            throw new BindingException("Impossible d'instancier " + type.getName() + " pour le binding.", e);
        }
    }

    private static void writeProperty(Object target, Field field, Object value) throws BindingException {
        try {
            Method setter = findSetter(target.getClass(), field.getName(), value);
            if (setter != null) {
                setter.setAccessible(true);
                setter.invoke(target, value);
                return;
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new BindingException("Impossible d'ecrire la propriete '" + field.getName() + "' de "
                    + target.getClass().getName() + ".", e);
        }
    }

    private static Object readProperty(Object target, Field field) {
        try {
            String base = field.getName();
            String suffix = Character.toUpperCase(base.charAt(0)) + base.substring(1);
            for (Method method : target.getClass().getMethods()) {
                if (method.getParameterCount() == 0
                        && (method.getName().equals("get" + suffix) || method.getName().equals("is" + suffix))) {
                    method.setAccessible(true);
                    return method.invoke(target);
                }
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static Method findSetter(Class<?> type, String property, Object value) {
        String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        String setterName = "set" + suffix;
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(setterName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (value == null || parameterType.isAssignableFrom(value.getClass())
                    || (parameterType.isPrimitive() && isWrapper(parameterType, value.getClass()))) {
                return method;
            }
        }
        return null;
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    // ------------------------------------------------------------------
    // Types / outils
    // ------------------------------------------------------------------

    /** Types "simples" pouvant etre convertis depuis une chaine. */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type.equals(String.class)
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || type.equals(Boolean.class)
                || type.equals(Character.class);
    }

    /** Types texte : une valeur quelconque y est acceptee telle quelle. */
    private static boolean isTextType(Class<?> type) {
        return type.equals(String.class) || CharSequence.class.isAssignableFrom(type);
    }

    private static boolean isListType(Class<?> type) {
        return List.class.isAssignableFrom(type);
    }

    /** Types representant un fichier televerse (upload multipart). */
    private static boolean isFileType(Class<?> type) {
        return UploadedFile.class.equals(type) || Part.class.equals(type);
    }

    /** true pour UploadedFile[], List<UploadedFile>, Part[], List<Part>... */
    private static boolean isFileTypeCollection(Class<?> type, Type genericType) {
        if (type.isArray()) {
            return isFileType(type.getComponentType());
        }
        return isListType(type) && isFileType(listElementType(genericType));
    }

    /** true si la requete est un formulaire multipart/form-data (upload). */
    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private static Class<?> listElementType(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            Type argument = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (argument instanceof Class) {
                return (Class<?>) argument;
            }
            if (argument instanceof ParameterizedType
                    && ((ParameterizedType) argument).getRawType() instanceof Class) {
                return (Class<?>) ((ParameterizedType) argument).getRawType();
            }
        }
        return String.class;
    }

    private static boolean isWrapper(Class<?> primitive, Class<?> wrapper) {
        if (primitive.equals(boolean.class)) return wrapper.equals(Boolean.class);
        if (primitive.equals(byte.class)) return wrapper.equals(Byte.class);
        if (primitive.equals(char.class)) return wrapper.equals(Character.class);
        if (primitive.equals(short.class)) return wrapper.equals(Short.class);
        if (primitive.equals(int.class)) return wrapper.equals(Integer.class);
        if (primitive.equals(long.class)) return wrapper.equals(Long.class);
        if (primitive.equals(float.class)) return wrapper.equals(Float.class);
        if (primitive.equals(double.class)) return wrapper.equals(Double.class);
        return false;
    }

    /** true si au moins un champ de la requete commence par le prefixe donne. */
    private static boolean hasParametersWithPrefix(HttpServletRequest request, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return !request.getParameterMap().isEmpty();
        }
        String expected = prefix + ".";
        for (String key : request.getParameterMap().keySet()) {
            if (key.startsWith(expected)) {
                return true;
            }
        }
        // En multipart, les champs de type <input type="file"> n'apparaissent
        // pas dans les parametres : on verifie aussi le nom des "parts".
        if (isMultipart(request)) {
            try {
                for (Part part : request.getParts()) {
                    if (part.getName().startsWith(expected)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // Requete illisible : on considere qu'aucun champ ne correspond.
            }
        }
        return false;
    }

    /** Nom du champ de formulaire associe a un parametre simple. */
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
}
