Reflections Mojo configuration on your pom.xml file
```
    <build>
        <plugins>
            <plugin>
                <groupId>org.reflections</groupId>
                <artifactId>reflections-maven</artifactId>
                <version>latest version...</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>reflections</goal>
                        </goals>
                        <phase>process-classes</phase>

                    <configuration>
                    </configuration>

                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

By default, the Reflections Maven plugin saves the marshalled xml file of scanned metadata into ${project.build.outputDirectory}/META-INF/reflections/${project.artifactId}-reflections.xml

