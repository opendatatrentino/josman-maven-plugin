<p class="josman-to-strip">
WARNING: THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://opendatatrentino.github.io/josman-maven-plugin/" target="_blank">PROJECT WEBSITE</a>
</p>


#### About


Maven plugin for managing Java open source programs.


|**Usage**|**License**|**Roadmap**|**Contributing**|
|-----------|---------|-----------|----------------|
| See [docs](docs) |Business-friendly [Apache License v2.0](LICENSE.txt) | See [project milestones](../../milestones) | See [the wiki](../../wiki)|


#### Main ideas

Josman is modeled after this workflow, where you:

1. create an open source project on Github
2. keep docs in source code, in folder `docs/` (i.e. see [josman docs](docs))
3. edit your markdown files and set relative links between them so they display nice in Github
4. create a branch named `branch-x.y` (i.e `branch-1.2`)
5. release your library using <a href="http://semver.org" target="_blank">semantic versioning</a> and tag it with tagname `projectName-x.y.z` (i.e. `josman-1.2.3`)
6. run Josman to create a corresponding github website (i.e. http://opendatatrentino.github.io/josman) out of the docs. 
7. spam the world with links to your new shiny project website

Project wiki (i.e. [josman wiki](../../wiki)) is used for information about contributing to the project.

This way we 

* fully exploit all the existing editing features of Github
* reuse version information from git repo and Maven while generating the website
* let Josman perform the tedious tasks like checking versions, fixing links, publishing javadoc, ..
* evolve documention in separate branches
    * so if you have to patch something or just improve docs, just work in relative `branch-x.y` and then run Josman to publish it (even if it is not released).
    * if you need to add functionality, create new branch named `branch-x.y+1`


#### Credits

* David Leoni - DISI at University of Trento - david.leoni@unitn.it
