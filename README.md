The Polyglot Parser Generator
=============================

This is an SLR and LR(1) parser generator with some support for
common LR(k) constructs. It can generate outputs for arbitrary
target languages. It will use all cores on your build system, and is
engineered for maximum practicality and performance.

Principle and Language
----------------------

(TODO)

Execution
---------

Polyglot is published on Maven Central as
`org.anarres.polyglot:polyglot-core:${version}`.

The Gradle plugin, Maven plugin and Ant task are published
as `polyglot-gradle`, `polyglot-maven` and `polyglot-ant`
respectively.  There is also a commandline version, published as
`org.anarres.polyglot:polyglot-cmd:${version}`.

### Gradle

Gradle is the Right Answer(tm).

```
buildscript {
	classpath 'org.anarres.polyglot:polyglot-gradle:1.0.0-SNAPSHOT'
}
apply plugin: 'org.anarres.polyglot'
```

See [PolyglotPlugin](polyglot-gradle/src/main/java/org/anarres/polyglot/gradle/PolyglotPlugin.java).

### Maven

If you understand Maven better than I do, please help to correct this.

```
<plugin>
    <groupId>org.anarres.polyglot</groupId>
    <artifactId>polyglot-maven</artifactId>
    <version>${polyglot.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>polyglot</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

You will also need to set up a dependency on
`org.anarres.polyglot:polyglot-runtime:${polyglot.version}`.

See [PolyglotMojo](polyglot-maven/src/main/java/org/anarres/polyglot/maven/PolyglotMojo.java).

### Ant

My commiserations on still using ant. The ant task is called
`org.anarres.polyglot.ant.Polyglot`

You will also need to set up a dependency on
`org.anarres.polyglot:polyglot-runtime:${polyglot.version}`.

See [PolyglotTask](polyglot-ant/src/main/java/org/anarres/polyglot/ant/Polyglot.java).

Implementation Details
----------------------

Polyglot aims for the maximum possible performance for the practical
developer. It will attempt fast strategies (SLR), and if they fails,
it will attempt slower strategies (LR(1), LR(k)). It will use all
CPU cores on the system, so your computer will go out to lunch during
a build. However, the performance for LR(1) universes in excess of
10 million items is quite acceptable.

The polyglot core is also designed to be almost steady state in the
garbage collector, although the template engine (currently Velocity)
is quite inefficient and should be replaced.

Credits
-------

Polyglot is based on the specification (but not the code) of
[SableCC](http://www.sablecc.org/) by Etienne Gagnon, and aims for
backwards compatibility with all SableCC grammars and applications
built on the generated code.

