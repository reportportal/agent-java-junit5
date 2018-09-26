package com.epam.reportportal.junit5;

public interface TestItemType {

    String BEFORE_ALL = "BEFORE_CLASS";
    String SUITE = "TEST";
    String AFTER_ALL = "AFTER_CLASS";
    String BEFORE_EACH = "BEFORE_METHOD";
    String TEST = "STEP";
    String AFTER_EACH = "AFTER_METHOD";

}
