package com.epam.reportportal.junit5;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;

public class RPTestItem {

    public String name;
    public final String id;
    public final String type;
    public final String description;
    final ExtensionContext context;
    private static final Logger logger = Logger.getLogger(RPTestItem.class);

    public RPTestItem(ExtensionContext context, String type) {
        this.context = context;
        this.type = type;
        this.name = buildName();
        this.description = buildDescription();
        this.id = buildUniqueId();
    }

    public RPTestItem(ExtensionContext context, String type, String name) {
        this.context = context;
        this.type = type;
        this.name = name;
        this.description = buildDescription();
        this.id = buildUniqueId();
    }

    public RPTestItem(ExtensionContext context, String type, String name, String description) {
        this.context = context;
        this.type = type;
        this.name = name;
        this.description = description;
        this.id = buildUniqueId();
    }

    /**
     * Change default Jupiter display name of Parametrized and Repeated tests
     *
     * @return RP test item name
     */
    private String buildName() {
        String name;
        if (context.getElement().get() instanceof Method) {
            List<Class> annotations = Arrays.stream(context.getRequiredTestMethod().getAnnotations()).map(Annotation::annotationType).collect(Collectors.toList());
            if (annotations.contains(ParameterizedTest.class) || annotations.contains(RepeatedTest.class)) {
                int index = context.getUniqueId().indexOf("#");
                String countOfInvocation = context.getUniqueId().substring(index + 1);
                name = context.getRequiredTestMethod().getName() + "()[" + countOfInvocation;
            } else {
                name = context.getDisplayName();
            }
        } else {
            name = context.getDisplayName();
        }
        logger.debug("name: " + name);
        return name;
    }

    /**
     * Add Jupiter display name of Parametrized and Repeated tests as a test description to RP
     *
     * @return description of RP test item
     */
    private String buildDescription() {
        String description = "";
        if (context.getElement().get() instanceof Method) {
            List<Class> annotations = Arrays.stream(context.getRequiredTestMethod().getAnnotations()).map(Annotation::annotationType).collect(Collectors.toList());
            if (annotations.contains(ParameterizedTest.class) || annotations.contains(RepeatedTest.class)) {
                description = context.getDisplayName();
            }
        }
        return description;
    }

    /**
     * The unique ID from Jupiter does not consider methods with Before/After annotations.
     * For example, the unique ID of method with @Test the same as its method with @BeforeEach.
     * This method build unique ID for each method.
     *
     * @return RP test item unique id
     */
    private String buildUniqueId() {
        String testedClass = context.getTestClass().get().getName();
        String uniqueId;
        if (TestItemType.BEFORE_ALL.equals(this.type) || TestItemType.AFTER_ALL.equals(this.type)) {
            uniqueId = testedClass + "." + this.type;
        } else {
            uniqueId = testedClass + "." + this.name + "." + this.type;
        }
        logger.debug("uniqueId: " + uniqueId);
        return uniqueId;
    }
}