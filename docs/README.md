<p class="josman-to-strip">
WARNING: THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://opendatatrentino.github.io/josman-maven-plugin/" target="_blank">PROJECT WEBSITE</a>
</p>


### Getting started

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

```bash
    mvn josman:site
```

It will generate a website with documentation only for the current snapshot. By default Josman runs in [`dev`](#modes) mode, which is designed for speed: it doesn't fail on errors and fetches/copies as little as possible (note this behaviour is the opposite of previous [0.7.0 default](https://github.com/opendatatrentino/josman-maven-plugin/issues/29) ).

### Workflow

Josman is modeled after this workflow, where you:

1. create an open source project on Github
2. keep docs in source code, in folder `docs/` (i.e. see [josman docs](docs))
3. edit your markdown files and set relative links between them so they display nice in Github
4. release your library using <a href="http://semver.org" target="_blank">semantic versioning</a> and tag it with tagname `projectName-x.y.z` (i.e. `my-project-1.2.3`)
5. run Josman to create a corresponding Github website (i.e. http://opendatatrentino.github.io/josman-maven-plugin) out of the docs. 
6. spam the world with links to your new shiny project website
7. If you need to improve past versions, create a branch named `branch-x.y` (i.e `branch-1.2`)

Project wiki (i.e. [josman wiki](../../wiki)) is used for information about contributing to the project.

This way we 

* fully exploit all the existing editing features of Github
* reuse version information from git repo and Maven while generating the website
* let Josman perform the tedious tasks like checking versions, fixing links, publishing javadoc, ..
* evolve documention in separate branches
    * if you need to add functionality, create new branch named `branch-x.y+1`


### Flags

You can fine tune Josman by using four flags:

- `josman.snapshot` : Generates documentation for the latest snapshot version  
- `josman.releases` : Generates documentation for all past released versions (except the ignored ones)
- `josman.failOnError`: Fails when first error / warning is encountered
- `josman.javadoc`: copies javadoc to the website (if available, must be first built with `mvn javadoc:jar`)

For example to have Josman fail on errors you can call Maven like this:

```bash
mvn josman:site -Djosman.failOnError
```
You can also specify flags also in `pom.xml`  configuration: 

```
            <plugin>
                <groupId>eu.trentorise.opendata</groupId>
                <artifactId>josman-maven-plugin</artifactId>                
                <configuration>
                    <josman.failOnError>true</josman.failOnError>    
                </configuration>
            </plugin>
```

If you specify both `josman.snapshot` and `josman.releases`, and snapshot and latest release have equal major and minor numbers but snapshot has greater patch, then the snapshot will override the latest release.   

### Modes

You can quickly enable a set of flags by specifying a `josman.mode`. There are four modes `dev` (the default), `ci`, `staging` and `release` :


|  Flag    \  Mode   | dev (default)  |      CI    | staging  |  release  |
|--------------------|----------------|------------|----------|-----------|
| **failOnError**    |    _false_     |    _false_ | _true_   |   _true_  |
| **javadoc**        |    _false_     |    _true_  | _true_   |   _true_  |
| **releases**       |    _false_     |    _false_ | _true_   |   _true_  |
| **snapshot**       |    _true_      |    _true_  | _true_   |   _false_ |


**NOTE:** Josman modes have no relation with Maven profiles. 

You can run Josman in a specific mode like this:
  
```bash
mvn josman:site -Djosman.mode=release
```

Note single flags will override the `josman.mode`:

```bash
mvn josman:site -Djosman.mode=release -Djosman.failOnError=false
```

### Ignoring versions

To ignore some version, you can use `ignoredVersions` like this:

```
            <plugin>
                <groupId>eu.trentorise.opendata</groupId>
                <artifactId>josman-maven-plugin</artifactId>                
                <configuration>You can express the
                    <ignoredVersions>
                        <ignoredVersion>0.0.1</ignoredVersion>
                    </ignoredVersions>  
                </configuration>
            </plugin>

```

### Customizing organization

By default the organization is taken to be the Github organization. If you want to specify another, just use the `<organization>` tag in Maven `pom.xml`.  To use a custom logo for the organization, you can create a file named `docs/org-200px.png`. 

### Markdown

Some examples can be found in [tests](SomeTest.md) page. Keep in mind not all Github features are supported by <a href="https://github.com/vsch/flexmark-java" target="_blank"> FlexMark </a>, the library we use to convert to HTML (previously [we used Pegdown](https://github.com/opendatatrentino/josman-maven-plugin/issues/28)).

### Variables

You can insert Maven variables like `$'{project.version}` into pages.

There are also the special variables  `$'{josman.majorMinorVersion}` and `$'{josman.repoRelease}`

To write variables names verbatim as we done above, use `$'` adding the apex after the dollar. 

### Expressions

Josman has some limited support for executing Java expressions.  Strings in the format `$'eval{EXPR}` or `$'evalNow{EXPR}` will be replaced by the evaluation of the corresponding Java expression `EXPR`. Examples:

`$'evalNow{eu.trentorise.opendata.josman.test.JosmansTest.calcDate()}` = $evalNow{eu.trentorise.opendata.josman.test.JosmansTest.calcDate()}

`$'eval{eu.trentorise.opendata.josman.test.JosmansTest.sayHello()}` = $eval{eu.trentorise.opendata.josman.test.JosmansTest.sayHello()}


#### Expression workflow
 
The workflow to compute the expressions is the following:
 
1) Insert `eval` goal this into your `pom.xml` (by default expressions will run at `prepare-package` phase):

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

Having evaluated expressions in the published javadoc jar makes possible to regenerate the site without the need to recalculate expressions for older versions of the software. To produce the Javadoc jar you will need this in your `pom.xml` (note the `jar` goal runs at `package` time): 

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

 
To run from the command-line a full evaluation that will store evals in the javadoc and copy also the javadoc in the generated website, you can do like this: 
 
```bash
 mvn josman:eval javadoc:jar josman:site -Djosman.javadoc
```
 
#### Custom evaluation

If you want to create the evaluations file `target/apidocs/resources/josman-eval.csv` with some custom process (i.e. because you have [issues capturing logs](https://github.com/opendatatrentino/josman-maven-plugin/issues/17)), just don't put the `eval` goal in the plugin configuration:

```xml

<plugin>
	<groupId>eu.trentorise.opendata</groupId>
	<artifactId>josman-maven-plugin</artifactId>
	<version>${project.version}</version>		
</plugin>

```
You can create then the CSV file by yourself before `packaging` phase, for example during `test` phase or in `prepare-package`. For an example of such complex usage, you can see <a href="https://github.com/diversicon-kb/divercli" target="blank">DiverCLI project</a> .
  
#### Expression syntax
 
To write an expression in the docs, you can write `$'eval{EXPR}` where `EXPR` is a fully qualified Java static method _without parameters_, or a static field value. The evaluation result will then be converted to string with `String.valueOf()`. Each evaluation is computed exactly once at Maven `prepare-package` phase. If you want an expression to be computed each time the site is generated (i.e. to display current date), use instead `$'evalNow{EXPR}`.  
 
 
<b>Supported syntax</b>:
  

* methods: `$'eval{my.package.MyClass.myMethod()}`
* fields: `$'eval{my.package.MyClass.myField}`
* Spaces inside the parenthesis: `$'eval{ my.package.MyClass.myField }`
* Escape with `$'`: `$'eval {EXPR}` will produce verbatim `$'eval{EXPR}` without trying to execute anything
* Always re-evaluate : `$'evalNow{EXPR}`  
  
  
**NOT SUPPORTED YET SYNTAX:**

Remember current syntax support is limited, in particular these form will _not_ work: 

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

