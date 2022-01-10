# agent-java-junit5
[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/agent-java-junit5.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22agent-java-junit5%22)
[![CI Build](https://github.com/reportportal/agent-java-junit5/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/agent-java-junit5/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/agent-java-junit5/branch/develop/graph/badge.svg?token=tq832Jsqef)](https://codecov.io/gh/reportportal/agent-java-junit5)
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
---
# ReportPortal [JUnit5](https://junit.org/junit5/) Integration

The repository contains [JUnit5 Extension](https://junit.org/junit5/docs/current/user-guide/#extensions) for [ReportPortal](http://reportportal.io/) integration.

## Getting Started

To start using Report Portal with JUnit 5 create a service location file: 
1. Create folders **_/META-INF/services_** in **_resources_**
2. Put there a file named **_org.junit.jupiter.api.extension.Extension_**
3. Put a default implementation reference as a single row into the file: **_com.epam.reportportal.junit5.ReportPortalExtension_** 

Example:
__/META-INF/services/org.junit.jupiter.api.extension.Extension__
```none
com.epam.reportportal.junit5.ReportPortalExtension
```

If you desire to configure test *name*, *description* and *tags*:

Extend *ReportPortalExtension*, override *buildStartStepRq()* or other methods (see javadoc comments) and replace 
*com.epam.reportportal.junit5.ReportPortalExtension* with fully qualified custom Extension class name in this file.

### Maven

```xml
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
            <version>3.0.0-M5</version>
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

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

testCompile 'com.epam.reportportal:agent-java-junit5:$LATEST_VERSION'
```

#### Automatic Extension Registration (optional)

```groovy
test {
    useJUnitPlatform()
    systemProperty 'junit.jupiter.extensions.autodetection.enabled', true
}
```

## Disabled tests reporting
By default reporting of @Disabled tests is switched off. To switch it on - add next parameter to an execution goal:
- Maven: -DreportDisabledTests=true
- Gradle: -PreportDisabledTests=true 

# Step-by-step integration manual for JUnit5

This manual will walk you through the steps for integration of Report Portal with JUnit5 based project

First, make sure you have installed Report Portal, the installation steps could be found [here](http://reportportal.io/docs/Installation-steps)

We’ll assume that Report Portal is installed and running on <http://localhost:8080>

## Step 1 - Create new project (Maven)

> If you want to integrate Report Portal with existing project, go to step 2

#### 1.1 Start new maven project

![Start new maven project](integration_manual_files/step_new_maven_project.png)

#### 1.2 Enter GroupId and ArtifactId

![Entering groupId and artifactId](integration_manual_files/step_group_and_artifact_id.png)

#### 1.3 Enter project name

![Entering project name](integration_manual_files/step_project_name.png)

## Step 2 - Configure pom.xml

#### 2.1 Add following dependencies:

*Report Portal agent implementation for JUnit 5*
```xml
<dependency>
    <groupId>com.epam.reportportal</groupId>
    <artifactId>agent-java-junit5</artifactId>
    <version>$LATEST_VERSION</version>
</dependency>
```
> Latest version of the agent, could be found [here](https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22agent-java-junit5%22)


The Report Portal agent for JUnit 5 includes the JUnit 5 library dependency, and so we won't need other explicit JUnit 5 dependencies  
However, if you are adding Report Portal agent to existing project, with JUnit 5 dependency already declared, pay attention to dependency transitivity and the conflicts that might arise

> More about JUnit 5 dependencies structure could be found [here](https://junit.org/junit5/docs/current/user-guide/#dependency-metadata)

#### 2.2 Add Report Portal dedicated logger wrapper
Report Portal provides it's own logger implementation for major logging frameworks like *log4j* and *logback*

If you prefer using **Logback** logging library, add following dependencies:

*ReportPortal logback logger dependency*
```xml
<dependency>
    <groupId>com.epam.reportportal</groupId>
    <artifactId>logger-java-logback</artifactId>
    <version>5.1.1</version>
</dependency>
```
> Up to date version could be found [here](https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22logger-java-logback%22)

*The logback itself*
```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.10</version>
</dependency>
```

If you prefer using **Log4j** logging library, add following dependencies:

*ReportPortal log4j logger dependency*
```xml
<dependency>
    <groupId>com.epam.reportportal</groupId>
    <artifactId>logger-java-log4j</artifactId>
    <version>5.1.4</version>
</dependency>
```
> Up to date version could be found [here](https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22logger-java-log4j%22)

*The log4j itself*
```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.17.1</version>
</dependency>

<dependency>
     <groupId>org.apache.logging.log4j</groupId>
     <artifactId>log4j-core</artifactId>
     <version>2.17.1</version>
</dependency>
```

## Step 3 - Add the test with logging

#### 3.1 Add simple test method

Create a test class `MyTests` in the test directory and add JUnit 5 test method there

```java
package com.mycompany.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

public class MyTests {

    private static final Logger LOGGER = LogManager.getLogger(MyTests.class);

    @Test
    void testMySimpleTest() {
        LOGGER.info("Hello from my simple test");
    }
}
```

#### 3.2 Add `log4j2.xml` file to `resources` folder
*Example:*
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <PatternLayout
                    pattern="%d [%t] %-5level %logger{36} - %msg%n%throwable"/>
        </Console>
        <ReportPortalLog4j2Appender name="ReportPortalAppender">
            <PatternLayout
                    pattern="%d [%t] %-5level %logger{36} - %msg%n%throwable"/>
        </ReportPortalLog4j2Appender>
    </Appenders>
    <Loggers>
        <Root level="DEBUG">
            <AppenderRef ref="ConsoleAppender"/>
            <AppenderRef ref="ReportPortalAppender"/>
        </Root>
    </Loggers>
</Configuration>
```
It's needed to add `ReportPortalAppender` into this (as shown in the example)

By this moment, your project tree should look somewhat like the this:

![Project structure](integration_manual_files/step_project_structure.png)

## Step 4 - Configuring ReportPortal

#### 4.1 Open ReportPortal UI

Go to *http:$IP_ADDRESS_OF_REPORT_PORTAL:8080* (by default it is *http://localhost:8080*)

Login as **Admin** user and create the project (more details [here](http://reportportal.io/docs/Deploy-ReportPortal) and [here](http://reportportal.io/docs/Creation-of-project))

![RP. Add Project](integration_manual_files/step_add_project.png)

![RP. Add Project 2](integration_manual_files/step_add_project2.png)

#### 4.2 Add users to your project:

Go to *Administrative* -> *My Test Project* -> *Members* -> *Add user*
> Example link *http://localhost:8080/ui/#administrate/project-details/my_test_project/members*

![RP. Add user](integration_manual_files/step_add_user.png)

## Step 5 - Link ReportPortal with your tests

#### 5.1 - Add `reportportal.properties`

After you have created new user in your project, you can get `reportportal.properties` file example from the user *Profile* page

To do that, login as created user and go to *User icon* in header -> *Profile*

There, in *Configuration Examples* section, you can find the example of `reportportal.properties` file for that user

![RP. User profile](integration_manual_files/step_user_profile.png)

Returning back to the code. In your project, create file named `reportportal.properties` in `resources` folder and copy&paste the contents form the user profile page

*Example:*
```properties
[reportportal.properties]
rp.endpoint = http://localhost:8080
rp.uuid = d50810f1-ace9-44fc-b1ba-a0077fb3cc44
rp.launch = jack_TEST_EXAMPLE
rp.project = my_test_project
rp.enable = true
```

> More details on `reportportal.properties` file could be found [here](https://github.com/reportportal/client-java)

#### 5.2 - Register Report Portal agent in JUnit 5
There are two options how you can enable ReportPortal extension in your tests:
- By specifying `@ExtendWith` annotation
- By service location

##### Register ReportPortal extension with annotation
Each test marked with `@ExtendWith(ReportPortalExtension.class)` will be reporter to ReportPortal.
This is an inheritable annotation, that means you can put it on a superclass and all child classes will
also use a specified extension.

For example:  
```java
import com.epam.reportportal.junit5.ReportPortalExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(ReportPortalExtension.class)
public class EnumParametersTest {

	public enum TestParams {
		ONE,
		TWO
	}

	@ParameterizedTest
	@EnumSource(TestParams.class)
	public void testParameters(TestParams param) {
		System.out.println("Test: " + param.name());
	}

}
```

##### Register ReportPortal extension through service location
For those who implement their own custom extensions we decided to remove `/META-INF/services` file from our project.
To use our standard non-custom extension you need to add this file into your project by yourself.
To do that create a file named `org.junit.jupiter.api.extension.Extension` in `src/test/resources/META-INF/services` folder
with the following content:
```text
com.epam.reportportal.junit5.ReportPortalExtension
```

As a final step you need to tell JUnit to register our Report Portal agent, and there are multiple ways for doing that:

* method 1 - via Maven Surefire/Failsafe plugin (maven only)
* method 2 - via JVM system property
* method 3 - via `junit-platform.properties` file
* method 4 - via Gradle (gradle only)

###### Method 1 - using Maven Surefire/Failsafe plugin (maven only)

Add a `build` section and Maven Surefire plugin with the following configuration section to `pom.xml`

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0-M5</version>
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
The `junit.jupiter.extensions.autodetection.enabled = true` configuration parameter of the Surefire plugin links Report Portal agent with the tests

*Full pom.xml file example*

```xml
[pom.xml]
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mycompany</groupId>
    <artifactId>MyProject</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>com.epam.reportportal</groupId>
            <artifactId>agent-java-junit5</artifactId>
            <version>$LATEST_VERSION</version>
        </dependency>

        <dependency>
            <groupId>com.epam.reportportal</groupId>
            <artifactId>logger-java-log4j</artifactId>
            <version>5.1.4</version>
        </dependency>

        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>2.17.1</version>
        </dependency>

        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.17.1</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M5</version>
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

</project>
```

Now the Report Portal agent is linked to your tests and when you run the tests with maven, the results should be sent to Report Portal

> *Important note*. With this approach, only the tests executed via maven will be sent to Report Portal, while tests ran by IDE will not trigger the Report Portal agent and therefore the test-report won't be generated  

To have test results to be sent to Report Portal when executed from IDE (without maven), follow the steps below

###### Method 2 - using JVM system property  

Another way to link test runs with Report Portal is to add JVM system property to the run arguments for test runs  
Here is the example of adding run arguments with IntelliJ IDEA  

In Intellij IDEA go to *Run* -> *Edit Configurations* -> click on "+" sign -> select JUnit


![IntelliJ IDEA add JUnit Run Configuration with JVM system property](integration_manual_files/step_add_run_configuration.png)

Enter the name of the run, select classes and/or methods to be executed in this configuration and add the following line into *VM Options* field:
```shell
-Djunit.jupiter.extensions.autodetection.enabled=true
```

When you are done adding local run configuration, simply go to *Run* -> *Run <test_run_name>* and that test run results should be sent to Report Portal

###### Method 3 - using `junit-platform.properties` file

There is another option of linking Report Portal with your JUnit 5 tests

Add `junit-platform.properties` file to your `resources` folder and add the following line there:

```shell
junit.jupiter.extensions.autodetection.enabled=true
```

![JUnit 5 platform properties file](integration_manual_files/note_junit5_properties.png)

> More details about JUnit 5 extensions detection configuration could be found [here](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic)

With this approach, the test report will be generated and sent to Report Portal in any type of test run, whether it was via maven, gradle or via IDE

###### Method 4 - using Gradle test task (Gradle only)

Starting from gradle version `4.6` it provides native support for JUnit 5 tests via `useJUnitPlatform()`, more details [here](https://junit.org/junit5/docs/current/user-guide/#running-tests-build)

Assuming that you have the gradle project setup, add/edit your `test` task in `gradle.build` file with the following code:

```gradle
test {
    testLogging.showStandardStreams = true
    useJUnitPlatform()
    systemProperty 'junit.jupiter.extensions.autodetection.enabled', true
}
```

> Please note, that using this method, the test report will be generated only if you ran your tests with Gradle (e.g. `gradle clean test`)

*Full `build.gradle` file example:*

```groovy
[build.gradle]
apply plugin: 'java'
sourceCompatibility = 1.8
targetCompatibility = 1.8

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compile 'com.epam.reportportal:logger-java-log4j:5.1.4'
    compile 'org.apache.logging.log4j:log4j-api:2.17.1'
    compile 'org.apache.logging.log4j:log4j-core:2.17.1'
    compile 'com.epam.reportportal:agent-java-junit5:$LATEST_VERSION'
}

test {
    testLogging.showStandardStreams = true
    useJUnitPlatform()
    systemProperty 'junit.jupiter.extensions.autodetection.enabled', true
}
```

## Step 6 - Observing test run report

After you linked the Report Portal JUnit 5 agent using one of the approaches described above and ran your tests, you should be able to see the results in your ReportPortal UI instance  
To do that, login to ReportPortal, and go to *Left Panel* -> *Launches*

You should see the launch there, with the name equal to the value of `rp.launch` from your `reportportal.properties` file

*Example:*

![RP. Launches](integration_manual_files/step_launches.png)

You can also see the test classes and individual test results by clicking on the launch name and going deeper

![RP. Test Results](integration_manual_files/step_test_results.png)

# Copyright Notice

Licensed under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) license (see the LICENSE.md file).
