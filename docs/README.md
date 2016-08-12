<p class="josman-to-strip">
WARNING: THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://opendatatrentino.github.io/josman-maven-plugin/" target="_blank">PROJECT WEBSITE</a>
</p>


### Configuration

Put this in the `plugins` section of your `pom.xml`:

```xml

<build>        
    <plugins>
        <plugin>
            <groupId>eu.trentorise.opendata</groupId>
            <artifactId>josman-maven-plugin</artifactId>
            <version>${project.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>eval</goal>
                    </goals>											
                </execution>
            </executions>
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

This will generate a website with documentation for all the released versions you have. It will not consider current snapshot, so if you just want to test how the website will look like with current docs, use `snapshot` flag:

```
mvn josman:site -Dsite.snapshot
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

### Markdown

Some examples can be found in [tests](tests.md) page. Keep in mind not all Github features are supported by <a href="https://github.com/sirthias/pegdown" target="_blank"> PegDown </a>, the library we use to convert to HTML.

### Variables

You can insert variables into pages, but current support is limited to 

`$'{project.version}`, `$'{josman.majorMinorVersion}`, and `$'{josman.repoRelease}`

which supercede the deprecated `#'{version}`, `#'{majorMinorVersion}`, and `#'{repoRelease}`.  

To write variables names verbatim, use `$'` adding the apex after the dollar. 

### Expressions

Josman has some limited support for executing Java expressions.  Strings in the format `$'eval{EXPR}` or `$'evalNow{EXPR}` will be replaced by the evaluation of the corresponding Java expression `EXPR`. Examples:

`$'evalNow{eu.trentorise.opendata.josman.test.JosmansTest.calcDate()}` = $evalNow{eu.trentorise.opendata.josman.test.JosmansTest.calcDate()}

`$'eval{eu.trentorise.opendata.josman.test.JosmansTest.sayHello()}` = $eval{eu.trentorise.opendata.josman.test.JosmansTest.sayHello()}


#### Expression workflow
 
The workflow to compute the expressions is the following:
 
1) Insert `eval` goal this into your `pom.xml` :

```xml
<plugin>
	<groupId>eu.trentorise.opendata</groupId>
	<artifactId>josman-maven-plugin</artifactId>
	<version>${project.version}</version>
	<executions>
		<execution>
			<goals>
				<goal>eval</goal>
			</goals>											
		</execution>
	</executions>
</plugin>
```
By default expressions will run at `prepare-package` phase.
 
2) Evaluate expressions:

```bash
	mvn josman:eval
``` 

Expressions will run with classpath environment for tests, and results will be put in this CSV file: 

```
	target/apidocs/resources/josman-eval.csv
```
so they can be permanently packaged in the javadoc jar.

3) Generate the site:
 
```bash
 	mvn josman:site
```
For `$'eval{EXPR}` forms, site generation will look for relative `EXPR` in the CSV file, while for `$'evalNow{EXPR}` forms the `EXPR` will be re-executed at each site generation.

#### Storing expressions in javadoc

Having evaluated expressions in the published javadoc jar makes possible to regenerate the site without the need to recalculate expressions for older versions of the software. To produce the javadoc jar you will need this in your `pom.xml` (note the `jar` goal runs at `package` time): 

```xml

<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-javadoc-plugin</artifactId>
	<version>2.10.1</version>
 	<executions>
		<execution>					
			<goals>
				<goal>jar</goal>
			</goals>
		</execution>
	</executions>  
</plugin>

```  
 
#### Custom evaluation

If you want to create the file `target/apidocs/resources/josman-eval.csv` with some custom process (i.e. because you have [issues capturing logs](https://github.com/opendatatrentino/josman-maven-plugin/issues/17)) just don't put the `eval` goal in the plugin configuration:

```xml

<plugin>
	<groupId>eu.trentorise.opendata</groupId>
	<artifactId>josman-maven-plugin</artifactId>
	<version>${project.version}</version>		
</plugin>

```
You can create the CSV file by yourself before `packaging` phase, for example during `test` phase or in `prepare-package`.
  
#### Expression syntax
 
You can write `$'eval{EXPR}` where `EXPR` is a Java static method without parameters, or a static field value. The evaluation result will then be converted to string with `String.valueOf()`. Each evaluation is computed exactly once at maven `prepare-package` phase. If you want an expression to be computed each time the site is generated (i.e. to display current date), use instead `$'evalNow{EXPR}`.  
 
 
<b>Supported syntax</b>:
  

* methods: `$'eval{my.package.MyClass.myMethod()}`
* fields: `$'eval{my.package.MyClass.myField}`
* Spaces inside the parenthesis: `$'eval{ my.package.MyClass.myField }`
* Escape with `$'`: `$'eval {EXPR}` will produce `$'eval{EXPR}` without trying to execute anything
* Always re-evaluate : `$'evalNow{EXPR}`  
  
<b>Unsupported syntex</b>:
  

* method with parameters: `$'eval{my.package.MyClass.myMethod("bla bla")}`
* method chains: `$'eval{my.package.MyClass.myMethod().anotherMethod()}`
* classes: `$'eval{my.package.MyClass}`
* unqualified classes: `$'eval{MyClass.myMethod()}`
* new instances: `$'eval{new my.package.MyClass()}`


### Publishing site

A good companion to Josman is <a href="https://github.github.com/maven-plugins/site-plugin/" target="_blank"> GitHub Site Plugin</a> that allows sending the generated website to origin repository in the `gh-pages` branch, so that it will be served by Github on `myorganization.github.io/myrepo` urls.

To send website:

```bash
	mvn com.github.github:site-maven-plugin:site
```

