package org.reflections.spring;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.util.ClasspathHelper;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.reflections.ReflectionUtils.forName;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withName;

/** Reflections conponent-scan.
 * <p>scan components using Reflections scan.
 * <p>optionally collect saved resources using {@link Reflections#collect()} from META-INF/reflections/{basePackage}-reflections.xml
 * <p>optionally save scanned resources using {@link Reflections#save(String)} to META-INF/reflections/{basePackage}-reflections.xml
 * <pre>
 *       &#60beans
 *          ...
 *          xmlns:reflections="http://org.reflections"
 *          xsi:schemaLocation="...
 *             http://org.reflections http://org.reflections/reflections.xsd">
 *          ...
 *          &#60reflections:component-scan base-package="my.package.prefix"
 *              collect="false" save="false" parallel="true">
 *          &#60/reflections:component-scan>
 * </pre>
 */
@SuppressWarnings("unchecked")
public class ReflectionsNamespaceHandler extends NamespaceHandlerSupport {
    public void init() {
        registerBeanDefinitionParser("component-scan", new Parser());
    }

    public static class Parser extends ComponentScanBeanDefinitionParser {
        @Override protected Scanner configureScanner(ParserContext parserContext, Element element) {
            Scanner scanner = (Scanner) super.configureScanner(parserContext, element);
            if (element.hasAttribute("collect")) scanner.collect = Boolean.valueOf(element.getAttribute("collect"));
            if (element.hasAttribute("parallel")) scanner.parallel = Boolean.valueOf(element.getAttribute("parallel"));
            if (element.hasAttribute("save")) scanner.save = Boolean.valueOf(element.getAttribute("save"));
            scanner.resource = parserContext.getReaderContext().getResource();
            return scanner;
        }

        @Override protected Scanner createScanner(final XmlReaderContext readerContext, final boolean useDefaultFilters) {
            return new Scanner(readerContext.getRegistry(), useDefaultFilters);
        }
    }

    public static class Scanner extends ClassPathBeanDefinitionScanner {
        static Field annotationTypeField = field(AnnotationTypeFilter.class, "annotationType");
        static Field targetTypeField = field(AssignableTypeFilter.class, "targetType");
        static Field patternField = field(RegexPatternTypeFilter.class, "pattern");

        Resource resource;
        boolean collect = false;
        boolean save = false;
        boolean parallel = false;
        Set<String> additionalPackages;
        List<TypeFilter> additionalIncludeFilters;
        Set<BeanDefinition> candidates;

        public Scanner(final BeanDefinitionRegistry registry, final boolean useDefaultFilters) {
            super(registry, useDefaultFilters);
            if (useDefaultFilters) {
                additionalPackages = Sets.newHashSet("org.springframework");
                additionalIncludeFilters = Lists.<TypeFilter>newArrayList(new AnnotationTypeFilter(Configuration.class));
            }
        }

        protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
            Assert.notEmpty(basePackages, "At least one base package must be specified");
            findAllCandidateComponents(basePackages);
            return super.doScan("candidates"); 
        }

        @Override
        public Set<BeanDefinition> findCandidateComponents(String basePackage) {
            return basePackage.equals("candidates") ? candidates : null;
        }

        public Set<BeanDefinition> findAllCandidateComponents(String... basePackages) {
            long start = System.currentTimeMillis();
            try {
                candidates = Sets.newHashSet();
                if (collect) {
                    candidates = collect();
                }
                if (candidates.isEmpty()) {
                    Reflections reflections = scanAll(basePackages);
                    save(reflections, basePackages[0]);
                }
            }
            catch (Exception ex) {
                throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
            }
            long duration = System.currentTimeMillis() - start;
            logger.debug("findAllCandidateComponents took " + duration);

            return candidates;
        }

        private Set<BeanDefinition> collect() {
            Reflections collect = Reflections.collect();
            if (collect != null) {
                Collection<String> resources = getValues(collect.getStore());
                for (String resource : resources) {
                    ScannedGenericBeanDefinition e = find(forName(resource));
                    if (e != null) candidates.add(e);
                }
            }
            return candidates;
        }

        private Reflections scanAll(String... basePackages) throws IllegalAccessException {
            Reflections reflections = new Reflections(basePackages, additionalPackages,
                    parallel ? Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()) : null);

            Set<Class<?>> candidateTypes = getTypes(reflections, (List<TypeFilter>) field(this, "includeFilters"));
            if (additionalIncludeFilters != null) {
                candidateTypes.addAll(getTypes(reflections, additionalIncludeFilters));
            }
            List<TypeFilter> excludeFilters = (List<TypeFilter>) field(this, "excludeFilters");
            candidateTypes.removeAll(excludeFilters);

            for (Class<?> type : candidateTypes) {
                ScannedGenericBeanDefinition e = find(type);
                if (e != null) candidates.add(e);
            }
            return reflections;
        }

        private ScannedGenericBeanDefinition find(final Class<?> component) {
            boolean traceEnabled = logger.isTraceEnabled(), debugEnabled = logger.isDebugEnabled();

            String name = component.getName();
            if (traceEnabled) logger.trace("Scanning " + name);

            try {
                MetadataReader metadataReader = getMetadataReader(component);
                if (isCandidateComponent(metadataReader)) {
                    ScannedGenericBeanDefinition candidate = new ScannedGenericBeanDefinition(metadataReader);
                    if (isCandidateComponent(candidate)) {
                        if (debugEnabled) logger.debug("Identified candidate component class: " + name);
                        return candidate;
                    } else if (debugEnabled) logger.debug("Ignored because not a concrete top-level class: " + name);
                } else if (traceEnabled) logger.trace("Ignored because not matching any filter: " + name);
            } catch (Throwable ex) { throw new BeanDefinitionStoreException("Failed to read candidate component class: " + name, ex); }

            return null;
        }

        private void clear(Map<String, Multimap<String, String>> storeMap) {
            final List<String> names = Lists.newArrayList();
            for (BeanDefinition resource : candidates) {
                names.add(resource.getBeanClassName());
            }

            for (String key : storeMap.keySet()) {
                Multimap<String, String> map = storeMap.get(key);
                final Iterator<String> iterator = map.values().iterator();
                while (iterator.hasNext()) {
                    if (!names.contains(iterator.next())) iterator.remove();
                }
            }
        }

        private void save(Reflections reflections, String basePackage) throws URISyntaxException, IllegalAccessException {
            if (save && !candidates.isEmpty()) {
                clear(reflections.getStore().getStoreMap());
                String path = field(resource, "path");
                Set<URL> urls = ClasspathHelper.forResource(path);
                for (URL url : urls) {
                    File file = new File(url.toURI());
                    if (file.isDirectory() && file.canWrite()) {
                        reflections.save(file.getPath() + "/META-INF/reflections/" + basePackage + "-reflections.xml");
                        return;
                    }
                }
            }
        }

        private Set<Class<?>> getTypes(Reflections reflections, List<TypeFilter> includeFilters) throws IllegalAccessException {
            Set<Class<?>> types = Sets.newHashSet();

            for (TypeFilter includeFilter : includeFilters) {
                if (includeFilter instanceof AnnotationTypeFilter) {
                    Class<? extends Annotation> annotationType = field(includeFilter, annotationTypeField);
                    types.addAll(reflections.getTypesAnnotatedWith(annotationType, false));

                } else if (includeFilter instanceof AssignableTypeFilter) {
                    Class targetType = field(includeFilter, targetTypeField);
                    types.addAll(reflections.getSubTypesOf(targetType));

                } else if (includeFilter instanceof RegexPatternTypeFilter) {
                    Pattern pattern = field(includeFilter, patternField);
                    Map<String, Multimap<String, String>> map = reflections.getStore().getStoreMap();
                    for (String k : map.keySet()) {
                        Collection<String> values = map.get(k).values();
                        for (String value : values) {
                            if (pattern.matcher(value).matches()) {
                                types.add(forName(value));
                            }
                        }
                    }
                } else throw new UnsupportedOperationException("unsupported include filter " + includeFilter);
            }

            return types;
        }

        private MetadataReader getMetadataReader(final Class<?> aClass) {
            return new MetadataReader() {
                public Resource getResource() { return null; }
                public ClassMetadata getClassMetadata() { return new org.springframework.core.type.StandardClassMetadata(aClass); }
                public AnnotationMetadata getAnnotationMetadata() { return new org.springframework.core.type.StandardAnnotationMetadata(aClass); }
            };
        }

        public Collection<String> getValues(Store store) {
            Collection<String> values = Lists.newArrayList();
            Map<String, Multimap<String, String>> storeMap = store.getStoreMap();
            for (String key : storeMap.keySet()) {
                Multimap<String,String> map = storeMap.get(key);
                if (map != null) {
                    values.addAll(map.values());
                }
            }
            return values;
        }
    }

    static Field field(Class<?> type, String field) {
        Field f = getAllFields(type, withName(field)).iterator().next();
        f.setAccessible(true);
        return f;
    }

    static <T> T field(Object object, String field) throws IllegalAccessException {
        return (T) field(object.getClass(), field).get(object);
    }

    static <T> T field(Object object, Field field) throws IllegalAccessException {
        return (T) field.get(object);
    }

}
