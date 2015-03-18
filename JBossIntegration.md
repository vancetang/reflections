## JBoss vfs integration ##

### JBoss 7 ###
_for urls like 'vfs://some/thing'_

1. include jboss vfs jar in classpath:

for maven:
```
        <dependency>
            <groupId>org.jboss</groupId>
            <artifactId>jboss-vfs</artifactId>
            <version>3.0.1.GA</version> <!-- jdk1.6+ -->
        </dependency>
```

2. add default UrlType to Vfs:
```
        Vfs.addDefaultURLTypes(
                new Vfs.UrlType() {
                    public boolean matches(URL url) {
                        return url.getProtocol().equals("vfs");
                    }

                    public Vfs.Dir createDir(URL url) {
                        VirtualFile content;
                        try {
                            content = (VirtualFile) url.openConnection().getContent();
                        } catch (Throwable e) {
                            throw new ReflectionsException("could not open url connection as VirtualFile [" + url + "]", e);
                        }

                        Vfs.Dir dir = null;
                        try {
                            dir = createDir(new java.io.File(content.getPhysicalFile().getParentFile(), content.getName()));
                        } catch (IOException e) { /*continue*/ }
                        if (dir == null) {
                            try {
                                dir = createDir(content.getPhysicalFile());
                            } catch (IOException e) { /*continue*/ }
                        }
                        return dir;
                    }

                    Vfs.Dir createDir(java.io.File file) {
                        try {
                            return file.exists() && file.canRead() ? file.isDirectory() ? new SystemDir(file) : new ZipDir(new JarFile(file)) : null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                });

```



---


**for other jboss version, this might help:**

_for urls like 'vfsfile://some/thing' or 'vfszip://some/thing'_
```
    Vfs.addDefaultUrlType(new UrlTypeVFS());
```