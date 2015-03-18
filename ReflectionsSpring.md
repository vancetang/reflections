## Reflections Spring component-scan integration ##

1. Add the reflections-spring jar

For Maven:
```
        <dependency>
            <groupId>org.reflections</groupId>
            <artifactId>reflections-spring</artifactId>
            <version>0.9.9-RC1</version>
        </dependency>
```

2. Edit your spring beans xml file to use Reflections for component-scan instead of 'context:component-scan'

```
       <beans
          ...
          xmlns:reflections="http://org.reflections"
          ...
          xsi:schemaLocation="...
             http://org.reflections http://org.reflections/reflections.xsd">
          ...


          <reflections:component-scan base-package="my.package.prefix"
              collect="false" save="false" parallel="true">
              
              <!-- sorry for that, a little hackery until next version-->
              <reflections:exclude-filter type="regex" expression="org.springframework.(?!stereotype).*"/>

          </reflections:component-scan>

```
**Depending on your ide, you might need to register the reflections.xsd. it is located in the jar as well.**

Some comments:
  * **collect="true"** means that Reflections will try to avoid scanning by looking for a file in the current classpath at META-INF/reflections/-reflections.xml. this resource can be added using the reflections-maven plugin for any module. if not found, Reflections will scan the classpath based on the base-package attribute.

  * using a combination of **collect="true" save="true"** is ideal for development time. Reflections will scan the first time and serialize its store to xml - preferably next to the beans xml file and under META-INF/reflections/{base-package}-reflections.xml. next time you bootstrap the system collect should find that xml and load even faster. watch the logs to see what is happening during the spring bootstrap. you might need to delete this file when adding new beans.

  * by using **parallel="true"**, Reflections will use all CPUs for scanning each url