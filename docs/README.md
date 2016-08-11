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

### Executing Java expressions

Josman has some limited support for executing Java commands with `$eval{EXPR}`: strings in this format will be replaced by the evaluation of the corresponding Java expression.
 
The workflow to compute the evaluations is the following:
 
1. Tell Maven to compute expressions by inserting this into your `pom.xml` ( by default expressions are evaluated at `prepare-package` phase):

```xml

<plugin>
	<groupId>eu.trentorise.opendata</groupId>
	<artifactId>josman-maven-plugin</artifactId>
	<version>#{version}</version>
	<executions>
		<execution>
			<goals>
				<goal>eval</goal>
			</goals>											
		</execution>
	</executions>
</plugin>

``` 
2. Evaluated expressions are stored as javadocs resource in CSV file `target/apidocs/resources/josman-eval.csv`
3. Once the CSV file is present, it is then possible to generate the site by issuing `mvn josman:site`

Evaluation is done using the classpath environment for tests, and evaluations results are put in file `target/apidocs/resources/josman-eval.csv` so they can be permanently packaged in the javadoc jar. 


### Storing expressions in javadoc

Having evaluated expressions in the published javadoc jar makes possible to regenerate the site without the need to recalculate expressions for older versions of the software. To produce the javadoc jar you will need this in your `pom.xml` (the `jar` goal runs at `package` time): 

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

If you want to create the file `target/apidocs/resources/josman-eval.csv` with some custom process (i.e. because you have [issues with capturing logging](https://github.com/opendatatrentino/josman-maven-plugin/issues/17)) just don't put the `eval` goal in the plugin configuration:

```xml

	<plugin>
		<groupId>eu.trentorise.opendata</groupId>
		<artifactId>josman-maven-plugin</artifactId>
		<version>#{version}</version>		
	</plugin>


```
You can create the file before `packaging` phase, like during `testing` or in `prepare-package` phase.  
  
#### Expression syntax
 
You can write `$eval{EXPR}` where `EXPR` is a Java static method without parameters, or a static field value. The evaluation result will then be converted to string with `String.valueOf()`. Each evaluation is computed exactly once at maven `prepare-package` phase. If you want an evaluation to be computed each time the site is generated (to display, i.e. current date), use instead `$evalNow{EXPR}`.  
 
 
<b>Supported syntax</b>:
  
<ul>
  <li>methods: `$eval{my.package.MyClass.myMethod()}`</li>
  <li>fields: `$eval{my.package.MyClass.myField}`</li>
  <li>Spaces inside the parenthesis: `$eval{ my.package.MyClass.myField }`</li>
  <li>Escape with `$_`: `$_eval{something}` will produce `$eval{something}` without trying to execute anything
  <li>Always re-evaluate : `$evalNow{EXPR}`</li>  
  </li>  
</ul>
  
<b>Unsupported syntax</b>:
  
<ul>
  <li>method with parameters: `$eval{my.package.MyClass.myMethod("bla bla")}`</li>
  <li>method chains: `$eval{my.package.MyClass.myMethod().anotherMethod()}`</li>
  <li>classes: `$eval{my.package.MyClass}`</li>
  <li>unqualified classes: `$eval{MyClass.myMethod()}`</li>
  <li>new instances: `$eval{new my.package.MyClass()}`</li>
</ul>

### Sending site to Github

A good companion to Josman is <a href="https://github.github.com/maven-plugins/site-plugin/" target="_blank"> GitHub Site Plugin</a> that allows sending the generated website to origin repository in the `gh-pages` branch, so that it will be served by Github on `myorganization.github.io/myrepo` urls.

To send website:

```
mvn com.github.github:site-maven-plugin:site
```

