<p class="josman-to-strip">
WARNING: THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://opendatatrentino.github.io/josman-maven-plugin/" target="_blank">PROJECT WEBSITE</a>
</p>


### Configuring the plugin

Put this in the `plugins` section of your `pom.xml`:

```
    <build>        
        <plugins>
            <plugin>
                <groupId>eu.trentorise.opendata.josman</groupId>
                <artifactId>josman-maven-plugin</artifactId>
                <version>#{version}</version>
            </plugin>

            ...
        </plugins>
        ...
    </build>
```

In case updates are available, version numbers follows <a href="http://semver.org/" target="_blank">semantic versioning</a> rules.

You can then invoke the plugin by calling the goal site:

```
    mvn josman:site
```

This will generate a website with documentation for all the released versions you have. It will not consider current snapshot, so if you just want to test how the website will look like for the current snapshot, use `snapshot` flag:

```
            <plugin>
                <groupId>eu.trentorise.opendata.josman</groupId>
                <artifactId>josman-maven-plugin</artifactId>                
                <configuration>
                    <snapshot>true</snapshot>    
                </configuration>
            </plugin>
```

To ignore some version, you can use `ignoredVersions`:

```
            <plugin>
                <groupId>eu.trentorise.opendata.josman</groupId>
                <artifactId>josman-maven-plugin</artifactId>                
                <configuration>
                    <ignoredVersions>
                        <ignoredVersion>0.0.1</ignoredVersion>
                    </ignoredVersions>  
                </configuration>
            </plugin>

```

### Sending site to Github

A good companion to Josman is <a href="https://github.github.com/maven-plugins/site-plugin/" target="_blank"> GitHub Site Plugin</a> that allows sending the generated website to origin repository in the gh-pages branch, so that it will be served by Github on `myorganization.github.io/myrepo` urls.
