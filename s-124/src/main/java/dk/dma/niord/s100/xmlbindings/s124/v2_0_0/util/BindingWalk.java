package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.xml.bind.JAXBElement;

/**
 * Visits every generated binding object reachable from a dataset.
 * <p/>
 * The traversal is reflective rather than a hand-written descent through the S-124 feature model.
 * The attributes the conformance rules apply to - the coded elements of S-100 Part 10b, clause
 * 10b-8.2.4, the times of S-124 clause 4.3.3 - sit at a dozen different depths, and a hand-written
 * walk would silently stop covering one the next time the bindings are regenerated from a revised
 * schema. Being reflective, it keeps covering whatever the schema grows.
 * <p/>
 * Only no-argument getters declared on binding types are followed, so the walk cannot wander out of
 * the dataset into the JDK, and an identity-based visited set makes it safe on the shared and
 * self-referential objects a hand-assembled dataset can easily contain.
 */
final class BindingWalk {

    /**
     * The package every generated S-124 and S-100 GML binding type lives under. Both the interfaces
     * of the model and the {@code impl} classes that realise them share this prefix.
     */
    private static final String BINDING_PACKAGE = "dk.dma.niord.s100.xmlbindings";

    private BindingWalk() {
    }

    /** Applies the visitor to the root and to every binding object reachable from it. */
    static void forEach(Object root, Consumer<Object> visitor) {
        forEachProperty(root, (property, node) -> visitor.accept(node));
    }

    /**
     * As {@link #forEach}, but also tells the visitor which property the object was reached
     * through - the getter name less its {@code get} prefix, decapitalised, or {@code null} for the
     * root. Elements of a collection are reported under the property the collection came from.
     * <p/>
     * Some rules apply to a type only in certain positions. {@code gml:ReferenceType} is the case
     * that forced this: it encodes S-124's feature and information associations, but the same type
     * also encodes {@code maskReference} inside {@code S100_SpatialAttributeType}, which is a
     * spatial mask rather than an association, so a check keyed on the type alone would misfire.
     */
    static void forEachProperty(Object root, BiConsumer<String, Object> visitor) {
        walk(null, root, Collections.newSetFromMap(new IdentityHashMap<>()), visitor);
    }

    private static void walk(String property, Object node, Set<Object> seen,
            BiConsumer<String, Object> visitor) {
        if (node == null) {
            return;
        }
        if (node instanceof Collection<?> collection) {
            for (Object element : collection) {
                walk(property, element, seen, visitor);
            }
            return;
        }
        if (node instanceof JAXBElement<?> element) {
            walk(property, element.getValue(), seen, visitor);
            return;
        }
        if (!isBindingType(node.getClass()) || !seen.add(node)) {
            return;
        }
        visitor.accept(property, node);
        for (Method getter : node.getClass().getMethods()) {
            if (getter.getParameterCount() != 0 || !getter.getName().startsWith("get")
                    || "getClass".equals(getter.getName())) {
                continue;
            }
            Class<?> returnType = getter.getReturnType();
            // Primitives, strings, dates and the like hold no nested binding object, so calling
            // them would only cost time.
            if (returnType.isPrimitive()) {
                continue;
            }
            if (Collection.class.isAssignableFrom(returnType)) {
                // A collection getter is followed only when its element type can actually hold a
                // binding object. This is not an optimisation: the generated impls create their
                // backing list on first call, and eleven of them back a list-valued XML *attribute*
                // that way - {@code @XmlAttribute List<String> nilReasons} on ReferenceTypeImpl and
                // the spatial property impls among them. JAXB omits a null attribute list but
                // marshals a materialised empty one as {@code nilReason=""}, so merely looking at
                // those getters would add attributes to the document - and those bytes are what the
                // exchange set signs. Element types are recoverable from the generic signature, so
                // the collections that matter are still followed.
                if (!holdsBindingObjects(getter)) {
                    continue;
                }
            } else if (!(JAXBElement.class.isAssignableFrom(returnType) || isBindingType(returnType))) {
                continue;
            }
            Object child;
            try {
                child = getter.invoke(node);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // A getter that cannot be read carries nothing a caller of this walk can act on.
                // Skipping it keeps a hostile or half-built object graph from failing the marshal.
                continue;
            }
            walk(propertyName(getter), child, seen, visitor);
        }
    }

    /** The property a getter reads: its name less the {@code get} prefix, decapitalised. */
    private static String propertyName(Method getter) {
        String name = getter.getName().substring(3);
        return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Whether a collection-returning getter can hold binding objects, judged from its declared
     * element type.
     * <p/>
     * A collection whose element type cannot be determined - a raw {@code List}, a type variable,
     * a wildcard - is followed, because the cost of missing a coded element is a silent conformance
     * hole while the cost of an extra call is nil for every generated type that is not one of the
     * eleven attribute-backed lists.
     */
    private static boolean holdsBindingObjects(Method getter) {
        Type generic = getter.getGenericReturnType();
        if (!(generic instanceof ParameterizedType parameterized)) {
            return true;
        }
        Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length != 1 || !(arguments[0] instanceof Class<?> element)) {
            return true;
        }
        return isBindingType(element) || JAXBElement.class.isAssignableFrom(element);
    }

    /**
     * Whether the type is one of the generated bindings. Interfaces are matched as well as impl
     * classes, because the getters of the generated model are declared to return the interfaces.
     */
    private static boolean isBindingType(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg != null && pkg.getName().startsWith(BINDING_PACKAGE);
    }
}
