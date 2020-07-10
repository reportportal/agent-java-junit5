package com.epam.reportportal.junit5.features.configname;

import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

public class CustomDisplayNameGenerator implements DisplayNameGenerator {
	public static final String DISPLAY_NAME_CLASS = "My custom display name for class";
	public static final String DISPLAY_NAME_NESTED_CLASS = "My custom display name for nested class";
	public static final String DISPLAY_NAME_METHOD = "My custom display name for method";

	@Override
	public String generateDisplayNameForClass(Class<?> testClass) {
		return DISPLAY_NAME_CLASS;
	}

	@Override
	public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
		return DISPLAY_NAME_NESTED_CLASS;
	}

	@Override
	public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
		return DISPLAY_NAME_METHOD;
	}
}
