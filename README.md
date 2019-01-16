# agent-java-junit5
[![Build Status](https://travis-ci.org/reportportal/agent-java-junit5.svg?branch=master)](https://travis-ci.org/reportportal/agent-java-junit5)
[ ![Download](https://api.bintray.com/packages/epam/reportportal/agent-java-junit5/images/download.svg) ](https://bintray.com/epam/reportportal/agent-java-junit5/_latestVersion)
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![UserVoice](https://img.shields.io/badge/uservoice-vote%20ideas-orange.svg?style=flat)](https://rpp.uservoice.com/forums/247117-report-portal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
---
# ReportPortal [JUnit5](https://junit.org/junit5/) Integration

The repository contains [JUnit5 Extension](https://junit.org/junit5/docs/current/user-guide/#extensions) for [ReportPortal](http://reportportal.io/) integration.

## Getting Started

### Maven

```xml
<repositories>
    <repository>
        <id>bintray</id>
        <url>http://dl.bintray.com/epam/reportportal</url>
    </repository>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
   <groupId>com.epam.reportportal</groupId>
   <artifactId>agent-java-junit5</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

#### Automatic Extension Registration (optional)

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.22.0</version>
            <configuration>
                <properties>
                    <configurationParameters>
                        junit.jupiter.extensions.autodetection.enabled = true
                    </configurationParameters>
                </properties>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Gradle

```yml
repositories {
    jcenter()
    mavenLocal()
    maven { url "http://dl.bintray.com/epam/reportportal" }
    maven { url "https://jitpack.io" }
}

testCompile 'com.epam.reportportal:agent-java-junit5:$LATEST_VERSION'
```

#### Automatic Extension Registration (optional)

```yml
test {
    useJUnitPlatform()
    systemProperty 'junit.jupiter.extensions.autodetection.enabled', true
}
```