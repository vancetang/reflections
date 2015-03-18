**version 0.9.9-RC1 available in  [Maven Central repository - Repo1](http://repo1.maven.org/maven2/org/reflections/reflections/0.9.9-RC1/), in the [Downloads](http://code.google.com/p/reflections/downloads/list) and in [Bintray](http://dl.bintray.com/content/ronmamo/reflections). Thank god for [Bintray](http://bintray.com).**

**project sources moved to GitHub -  http://github.com/ronmamo/reflections

---**

# Java runtime metadata analysis, in the spirit of [Scannotations](http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/) #

Reflections scans your classpath, indexes the metadata, allows you to query it on runtime and may save and collect that information for many modules within your project.

Using Reflections you can query your metadata such as:
  * get all subtypes of some type
  * get all types/constructos/methods/fields annotated with some annotation, optionally with annotation parameters matching
  * get all resources matching matching a regular expression
  * get all methods with specific signature including parameters, parameter annotations and return type

## How to use? ##
Add Reflections to your project. for maven projects just add this dependency:
```
     <dependency>
         <groupId>org.reflections</groupId>
         <artifactId>reflections</artifactId>
         <version>0.9.9-RC1</version>
     </dependency>
```
(Otherwise add the relevant jars to your projects,  [this](http://code.google.com/p/reflections/wiki/ClasspathNotes) might help, or see [uberjar](https://code.google.com/p/reflections/downloads/detail?name=reflections-0.9.9-RC1-uberjar.jar))

A typical use of Reflections would be:
```
     Reflections reflections = new Reflections("my.project.prefix");

     Set<Class<? extends SomeType>> subTypes = 
               reflections.getSubTypesOf(SomeType.class);

     Set<Class<?>> annotated = 
               reflections.getTypesAnnotatedWith(SomeAnnotation.class);
```


Basically, to use Reflections first instantiate it with one of the constructors, then depending on the scanners, use the convenient query methods:
```
     Reflections reflections = new Reflections("my.package.prefix");
     //or
     Reflections reflections = new Reflections(ClasspathHelper.forPackage("my.package.prefix"), 
          new SubTypesScanner(), new TypesAnnotationScanner(), new FilterBuilder().includePackage(...), ...);

     //or using the ConfigurationBuilder
     new Reflections(new ConfigurationBuilder()
          .filterInputsBy(new FilterBuilder().includePackage("my.project.prefix"))
          .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
          .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...));

     //then query, for example:
     Set<Class<? extends Module>> modules = reflections.getSubTypesOf(com.google.inject.Module.class);
     Set<Class<?>> singletons =             reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
     
     Set<String> properties =       reflections.getResources(Pattern.compile(".*\\.properties"));
     Set<Constructor> injectables = reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
     Set<Method> deprecateds =      reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
     Set<Field> ids =               reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);

     Set<Method> someMethods =      reflections.getMethodsMatchParams(long.class, int.class);
     Set<Method> voidMethods =      reflections.getMethodsReturn(void.class);
     Set<Method> pathParamMethods = reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
     Set<Method> floatToString =    reflections.getConverters(Float.class, String.class);
```

You can use other scanners defined in Reflections as well, such as: SubTypesScanner, TypeAnnotationsScanner (both default), ResourcrsScanner, MethodAnnotationsScanner, ConstructorAnnotationsScanner, FieldAnnotationsScanner, MethodParameterScanner or any custom scanner.

**Browse the [javadoc](http://reflections.googlecode.com/svn/trunk/reflections/javadoc/apidocs/index.html?org/reflections/Reflections.html) for more info. Also, browse the [tests directory](http://code.google.com/p/reflections/source/browse/#svn/trunk/reflections/src/test/java/org/reflections) to see some more examples.**


---


### ReflectionUtils ###
Reflections also contains some convenient java reflection helper methods for getting types/constructors/methods/fields/annotations matching some predicates, generally in the form of **getAllXXX(type, withYYY)**

for example:
```
     import static org.reflections.ReflectionUtils.*;

     Set<Method> getters = getAllMethods(someClass,
          withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0));

     //or
     Set<Method> listMethods = getAllMethods(List.class,
          withParametersAssignableTo(Collection.class), withReturnType(boolean.class));

     Set<Fields> fields = getAllFields(SomeClass.class, withAnnotation(annotation), withTypeAssignableTo(type));
```

**See more in the [ReflectionUtils javadoc](http://reflections.googlecode.com/svn/trunk/reflections/javadoc/apidocs/org/reflections/ReflectionUtils.html)**


---


### Reflections Maven plugin ###
With simple configuration you can save all scanned metadata into xml files. Later on, when your project is bootstrapping you can let Reflections collect all those resources and re-create that metadata for you, making it available at runtime without re-scanning the classpath - thus reducing the bootstrapping time.

Use this maven configuration in your pom file:
```
    <build>
        <plugins>
            <plugin>
                <groupId>org.reflections</groupId>
                <artifactId>reflections-maven</artifactId>
                <version>the latest version...</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>reflections</goal>
                        </goals>
                        <phase>process-classes</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

Then, on runtime:
```
        Reflections reflections =
                isProduction() ? Reflections.collect() : new Reflections("your.package.here");
```

**Check the [ReflectionsMojo](http://code.google.com/p/reflections/wiki/ReflectionsMojo) wiki page**


---


### ReflectionsSpring ###
Give a try to the new Reflections spring component-scan integration.
```
          <reflections:component-scan base-package="my.package.prefix"
              collect="false" save="false" parallel="true">
          </reflections:component-scan>
```
**More info [here](https://code.google.com/p/reflections/wiki/ReflectionsSpring)

---**

### Other use cases ###
Reflections can also:
  * scan urls in parallel
  * serialize scanned metadata to xml/json
  * collect saved metadata on bootstrap time for fastest load time without scanning
  * save your model entities metadata as .java file, so you can reference types/fields/methods/annotation in a static manner

**See the [UseCases](http://code.google.com/p/reflections/wiki/UseCases) wiki page**


---


### Contribute ###
You can easily extend Reflections by creating your specialized Scanner class and provide a query method on the Store object

Patches and extension are welcomed!

The license is [WTFPL](http://www.wtfpl.net/), just do what the fuck you want to. this library is given as an act of giving and generosity, [Dāna](http://en.wikipedia.org/wiki/D%C4%81na)
[![](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WLN75KYSR6HAY)

_Cheers_