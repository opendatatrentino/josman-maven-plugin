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
mvn josman:site -Dsite.snapshot=true
```

Or in `pom.xml`  configuration: 

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

### Supported Markdown features

Some examples can be found in [tests](tests.md) page. Keep in mind not all Github features are supported by <a href="https://github.com/sirthias/pegdown" target="_blank"> PegDown </a>, the Markdown-to-Html conversion library we use (some further tweaks to the generated HTML are done with <a href="http://jodd.org/doc/jerry" target="_blank"> Jerry</a>)

### Executing Java commands

Josman has some limited support for executing Java commands with `$exec{cmd}`: 
 Strings in this format will be replaced by the evaluation of the corresponding Java static 
 method or static field value. 
  
  <b>Supported syntax</b>:
  
  <ul>
  <li>methods: `$exec{my.package.MyClass.myMethod()}`</li>
  <li>fields: `$exec{my.package.MyClass.myField}`</li>
  <li>Spaces inside the parenthesis: `$exec{ my.package.MyClass.myField }`</li>
  <li>Escape with `$_`: `$_exec{something}` will produce `$exec{something}` without trying to execute anything
  </li>  
  </ul>
  
  <b>NOT supported</b>:
  <ul>
  <li>method with parameters: `$exec{my.package.MyClass.myMethod("bla bla")}`</li>
  <li>method chains: `$exec{my.package.MyClass.myMethod().anotherMethod()}`</li>
  <li>classes: `$exec{my.package.MyClass}`</li>
  <li>unqualified classes: `$exec{MyClass.myMethod()}`</li>
  <li>new instances: `$exec{new my.package.MyClass()}`</li>
  </ul>

### Sending site to Github

A good companion to Josman is <a href="https://github.github.com/maven-plugins/site-plugin/" target="_blank"> GitHub Site Plugin</a> that allows sending the generated website to origin repository in the `gh-pages` branch, so that it will be served by Github on `myorganization.github.io/myrepo` urls.

To send website:

```
mvn com.github.github:site-maven-plugin:site
```

