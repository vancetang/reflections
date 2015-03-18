for detailed explanation look at the [pom](http://code.google.com/p/reflections/source/browse/trunk/reflections/pom.xml)

---


# Runtime classpath #

runtime mandatory dependencies:
  * google guava
  * slf4j-api

runtime optional dependencies that you would probably use:
  * javassist - actually needed only when scanning is done. if using Reflections.collect() javassist is not needed
  * dom4j - effectively needed when XmlSerializer is used

runtime optional dependencies that you might use:
  * gson - effectively needed when JsonSerializer is used
  * jboss-vfs - effectively needed when using JBoss6UrlType
  * slf4j-simple - marked as optional to maven, use any implementation you like