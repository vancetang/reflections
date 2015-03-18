some useful use cases below:
  * bootstrap in a multi module environment
  * collect pre scanned metadata
  * serialize Reflections into a java source file, and use it to statically reference java elements
  * query the store directly, avoid definition of types in class loader
  * find resources in your classpath (for example all properties files)
  * optional parallel scanning



### bootstrap in a multi module environment ###
In a multi module project, where each module is responsible for it's properties, jpa entities and maybe guice modules, use Reflections to collect that metadata and bootstrap the application
```
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("your.package.here"),
                         ClasspathHelper.forClass(Entity.class), 
                         ClasspathHelper.forClass(Module.class))
                .setScanners(new ResourcesScanner(), 
                             new TypeAnnotationsScanner(), 
                             new SubTypesScanner()));

        Set<String> propertiesFiles = reflections.getResources(Pattern.compile(".*\\.properties"));
        Properties allProperties = createOneBigProperties(propertiesFiles);

        Set<Class<?>> jpaEntities = reflections.getTypesAnnotatedWith(Entity.class);
        SessionFactory sessionFactory = createOneBigSessionFactory(jpaEntities, allProperties);

        Set<Class<? extends Module>> guiceModules = reflections.getSubTypesOf(Module.class);
        Injector injector = createOneBigInjector(guiceModules);
```

### collect pre scanned metadata ###
first configure your project's parent pom in the build.plugins section with Reflections, like this
```
            <plugin>
                <groupId>org.reflections</groupId>
                <artifactId>reflections-maven</artifactId>
                <version>0.9.8</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>reflections</goal>
                        </goals>
                        <phase>process-classes</phase>
                    </execution>
                </executions>
            </plugin>
```
than, on runtime, collect these pre saved metadata and instantiate Reflections
```
        Reflections reflections =
                isProduction() ? Reflections.collect() : new Reflections("your.package.here");
```
of course, saving the scanned metadata can be done without maven, by simply calling the save method
```
    public static void main(String[] args) {
        //from time to time, I run this main to regenerate saved metadata for Reflections
        new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
                .setScanners(/*whatever*/))
                .save("src/main/resources/resource1-reflections.xml");

        new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("my.project.prefix.model"))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("my.project.prefix.model")))
                .setScanners(new TypesScanner(), new TypeElementsScanner())
                .setSerializer(new JavaCodeSerializer()))
                .save("src/main/java/my.project.prefix.model.MyModelStore");
    }
```

### serialize Reflections into a java source file, and use it to statically reference java elements ###
```
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(new FilterBuilder().include("model.package"))
                .setScanners(new TypesScanner(), new TypeElementsScanner())
                .setUrls(asList(ClasspathHelper.forPackage("model.package"))));

        String filename = System.getProperty("user.dir") + "/src/test/java/model.package.reflections.MyModelStore";
        reflections.save(filename, new JavaCodeSerializer());
```
replace "model.package" with your model's package prefix

this serializes types and types elements into interfaces respectively to fully qualified name, for example:
```
//generated using Reflections JavaCodeSerializer
public interface MyTestModelStore {
public interface org extends IPackage {
    public interface reflections extends IPackage {
		public interface TestModel$AC1 extends IClass {}
		public interface TestModel$C4 extends IClass {
			public interface f1 extends IField {}
			public interface m1 extends IMethod {}
...
}
```
than use the different resolve methods to resolve the serialized element into Class, Field or Method. for example:
```
Class<? extends IMethod> imethod = MyTestModelStore.org.reflections.TestModel$C4.m1.class;
Method method = JavaCodeSerializer.resolve(imethod);
```
can be useful to represent ognl statically and not in by strings or to use in annotations for marking fields or methods in a static manner

### query the store directly, avoid definition of types in class loader ###
querying through Reflections results in classes defined by the class loader. this is usually not a problem, but in cases class definition is not desirable, you can query the store directly using strings only
```
        Reflections reflections = new Reflections(...); //see in other use cases
        Set<String> serializableFqns = reflections.getStore().getSubTypesOf("java.io.Serializable");
```
plus, you can create your specialized query methods by querying the store directly
```
        Map<String, Multimap<String, String>> storeMap = reflections.getStore().getStoreMap();
//or
        Multimap<String, String> scannerMap = reflections.getStore().get(ResourcesScanner.class);
```

### find resources in your classpath ###
```
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("your.package.here"))
                .setScanners(new ResourcesScanner());

        Set<String> propertiesFiles = reflections.getResources(Pattern.compile(".*\\.properties"));
        Set<String> hibernateCfgFiles = reflections.getResources(Pattern.compile(".*\\.cfg\\.xml"));
```

### optional parallel scanning ###
```
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(...)
                .setScanners(...)
                .setUrls(...)
                .setExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
```
will result in this log line when scanning:
```
[main] INFO  org.reflections.Reflections - Reflections took 164 ms to scan 1 urls, producing 13 keys and 23 values [using 2 cores]
```