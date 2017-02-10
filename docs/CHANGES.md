
JOSMAN MAVEN PLUGIN #{version} RELEASE NOTES
-----------------------------------

http://opendatatrentino.github.io/josman-maven-plugin  



## Release Notes

### 0.8.0 

todo date

BREAKING CHANGES:

- now snapshot is created by default
- deprecated `site.snapshot` in favor of `josman.site`
- introduced `dev`,`ci`,`staging`, `release` modalities: 
  See https://github.com/opendatatrentino/josman-maven-plugin/issues/29

Major changes: 

- merged Josman project into this one.
- implemented `$'eval{EXPR}` and `$'evalNow{EXPR}` command
- more resilient to missing elements in snapshot mode
- Maven variables are now injected


Other changes: 

- made organization logo customizable
- switched owner to KidF, added credits
- fixed tables not being rendered
- not fetching tags in snapshot mode
- now copying all images
- replaced pegdown with flexmark (which is 100% compatible with pegdown !)
- deprecated `#` variables
- added variables  ` $'{project.version}, $'{josman.majorMinorVersion}, $'{josman.repoRelease} `
- improved logo
- better coloring for code
- fixed hashtags links not correctly generated
- removed external links icons, put 'Github' instead of 'Repo'
- regular underscores are now shown in `pre` tags

All closed issues: https://github.com/opendatatrentino/josman-maven-plugin/milestone/1?closed=1

### 0.7.0
- using tod-super-pom 1.3.0

### 0.6.1

May 5th, 2015

- fixed reading local javadocs


### 0.6.0

May 5th, 2015

- Made basic plugin


