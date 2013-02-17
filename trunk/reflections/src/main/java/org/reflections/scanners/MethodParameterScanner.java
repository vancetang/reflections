package org.reflections.scanners;

import com.google.common.collect.Lists;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.util.Utils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static org.reflections.ReflectionUtils.names;

/** scans methods/constructors and indexes parameters, return type and parameter annotations */
@SuppressWarnings("unchecked")
public class MethodParameterScanner extends AbstractScanner {

    @Override
    public void scan(Object cls) {
        final MetadataAdapter md = getMetadataAdapter();

        for (Object method : md.getMethods(cls)) {

            String signature = md.getParameterNames(method).toString();
            if (acceptResult(signature)) {
                getStore().put(signature, md.getMethodFullKey(cls, method));
            }

            String returnTypeName = md.getReturnTypeName(method);
            if (acceptResult(returnTypeName)) {
                getStore().put(returnTypeName, md.getMethodFullKey(cls, method));
            }

            List<String> parameterNames = md.getParameterNames(method);
            for (int i = 0; i < parameterNames.size(); i++) {
                for (Object paramAnnotation : md.getParameterAnnotationNames(method, i)) {
                    if (acceptResult((String) paramAnnotation)) {
                        getStore().put((String) paramAnnotation, md.getMethodFullKey(cls, method));
                    }
                }
            }
        }
    }

    /** get methods with parameter types matching given {@code types}*/
    public Collection<String> getMethodsMatchParams(Class<?>... types) {
        return getStore().get(names(Lists.newArrayList(types)).toString());
    }

    /** get methods with return type match given type */
    public Collection<String> getMethodsReturn(Class returnType) {
        return getStore().get(returnType.getName());
    }

    /** get methods with parameter type match first parameter {@code from}, and return type match type {@code to} */
    public Collection<String> getConverters(Class<?> from, Class<?> to) {
        return Utils.intersect(getMethodsMatchParams(from), getMethodsReturn(to));
    }

    /** get methods with any parameter annotated with given annotation */
    public Collection<String> getMethodsWithParamAnnotated(Class<? extends Annotation> annotation) {
        return getStore().get(annotation.getName());
    }

    /** get constructors with parameter types matching given {@code types}*/
    public Collection<String> getConstructorsMatchParams(Class<?>... types) {
        return getStore().get(names(Lists.newArrayList(types)).toString());
    }

    /** get constructors with any parameter annotated with given annotation */
    public Collection<String> getConstructorsWithParamAnnotated(Class<? extends Annotation> annotation) {
        return getStore().get(annotation.getName());
    }
}
