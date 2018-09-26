package com.epam.reportportal.junit5;

import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

import java.io.Serializable;
import java.util.Calendar;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;

import io.reactivex.Maybe;
import rp.com.google.common.annotations.VisibleForTesting;
import rp.com.google.common.base.Supplier;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ReportPortalJupiterService {

    private final MemorizingSupplier<Launch> launch;
    private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(ReportPortalJupiterService.class);
    private static boolean isLaunchStarted = false;
    private static final Logger logger = Logger.getLogger(ReportPortalJupiterService.class);

    public ReportPortalJupiterService() {
        this.launch = new MemorizingSupplier<>(() -> {
            //this reads property, so we want to
            //init ReportPortal object each time Launch object is going to be created
            final ReportPortal reportPortal = ReportPortal.builder().build();
            StartLaunchRQ rq = new StartLaunchRQ();
            ListenerParameters parameters = reportPortal.getParameters();
            rq.setName(parameters.getLaunchName());
            rq.setStartTime(Calendar.getInstance().getTime());
            rq.setTags(parameters.getTags());
            rq.setMode(parameters.getLaunchRunningMode());
            if (!isNullOrEmpty(parameters.getDescription())) {
                rq.setDescription(parameters.getDescription());
            }
            rq.setStartTime(Calendar.getInstance().getTime());
            return reportPortal.newLaunch(rq);
        });
    }

    /**
     * If not already done, start launch for the root context of the specified extension context.
     *
     **/
    public void startLaunchIfRequired() {
        // if no launch exists for this unique ID
        if (!isLaunchStarted) {
            launch.get().start();
            // add shutdown hook to finish launch when tests complete
            Runtime.getRuntime().addShutdownHook(getShutdownHook(launch.get()));
            isLaunchStarted = true;
        }
    }

    /**
     * Create a {@link Thread} object that encapsulates the implementation to finish the specified launch object.
     *
     * @param launch launch object
     * @return thread object that will finish the specified launch object
     */
    private static Thread getShutdownHook(final Launch launch) {
        return new Thread(() -> {
            FinishExecutionRQ rq = new FinishExecutionRQ();
            rq.setEndTime(Calendar.getInstance().getTime());
            launch.finish(rq);
        });
    }

    /**
     * Start suite (set of tests in the same class).
     *
     * @param context context of suite
     */
    public void startSuite(ExtensionContext context) {
        logger.debug("start SUITE '" + context.getDisplayName() + "'");
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(context.getDisplayName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(TestItemType.SUITE);
        final Maybe<String> suiteID = launch.get().startTestItem(rq);
        context.getStore(namespace).put(TestItemType.SUITE, suiteID);
    }

    /**
     * Finish suite (set of tests in the same class).
     *
     * @param context context of suite
     * @param status status of suite
     */
    public void finishSuite(ExtensionContext context, String status) {
        logger.debug("finish SUITE '" + context.getDisplayName() + "', status " + status);
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(status);
        launch.get().finishTestItem(getItemID(context, TestItemType.SUITE), rq);
        context.getStore(namespace).remove(TestItemType.SUITE);
    }

    /**
     * Start test item
     *
     * @param context launch object
     * @param type type of test item ("BEFORE_CLASS", "AFTER_CLASS", "BEFORE_METHOD", "AFTER_METHOD", "STEP").
     */
    public void startTestItem(ExtensionContext context, String type) {
        RPTestItem rpTestItem = new RPTestItem(context, type);
        startTestItem(context, rpTestItem);
    }

    /**
     * Start test item with specific name
     *
     * @param context launch object
     * @param type type of test item ("BEFORE_CLASS", "AFTER_CLASS", "BEFORE_METHOD", "AFTER_METHOD", "STEP").
     * @param testName name of test item
     */
    public void startTestItem(ExtensionContext context, String type, String testName) {
        RPTestItem rpTestItem = new RPTestItem(context, type, testName);
        startTestItem(context, rpTestItem);
    }

    /**
     * Start test item with specific name and description
     *
     * @param context launch object
     * @param type type of test item ("BEFORE_CLASS", "AFTER_CLASS", "BEFORE_METHOD", "AFTER_METHOD", "STEP").
     * @param testName name of test item
     * @param description description of test item
     */
    public void startTestItem(ExtensionContext context, String type, String testName, String description) {
        RPTestItem rpTestItem = new RPTestItem(context, type, testName, description);
        startTestItem(context, rpTestItem);
    }

    /**
     * Start test item
     *
     * @param context launch object
     * @param rpTestItem object of test item
     */
    private void startTestItem(ExtensionContext context, RPTestItem rpTestItem) {
        logger.debug("start item '" + rpTestItem.name + "', type " + rpTestItem.type);
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(rpTestItem.name);
        rq.setDescription(rpTestItem.description);
        rq.setUniqueId(rpTestItem.id);
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(rpTestItem.type);
        Maybe<String> itemID = launch.get().startTestItem(getItemID(context, TestItemType.SUITE), rq);
        context.getStore(namespace).put(rpTestItem.type, itemID);
    }

    /**
     * finish test item
     *
     * @param context launch object
     * @param type type of finished test item ("BEFORE_CLASS", "AFTER_CLASS", "BEFORE_METHOD", "AFTER_METHOD", "STEP").
     * @param status status of test item
     */
    public void finishTestItem(ExtensionContext context, String type, String status) {
        logger.debug("finish item '" + context.getDisplayName() + "', type " + type + ", status " + status);
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(status);
        launch.get().finishTestItem(getItemID(context, type), rq);
        context.getStore(this.namespace).remove(type);
    }

    /**
     * method for retrieve id of test item from context store
     *
     * @param context launch object
     * @param type type of test item
     *
     * @return ID of test item
     */
    @SuppressWarnings("unchecked")
    private Maybe<String> getItemID(ExtensionContext context, String type) {
        return (Maybe<String>) context.getStore(namespace).get(type);
    }


    public void sendStackTraceToRP(ExtensionContext context) {
        ReportPortal.emitLog(itemId -> {
            SaveLogRQ rq = new SaveLogRQ();
            rq.setTestItemId(itemId);
            rq.setLevel("ERROR");
            rq.setLogTime(Calendar.getInstance().getTime());
            if (context.getExecutionException().isPresent()) {
                rq.setMessage(getStackTraceAsString(context.getExecutionException().get()));
            } else {
                rq.setMessage("Test has failed without exception");
            }
            rq.setLogTime(Calendar.getInstance().getTime());

            return rq;
        });
    }

    @VisibleForTesting
    static class MemorizingSupplier<T> implements Supplier<T>, Serializable {
        final Supplier<T> delegate;
        transient volatile boolean initialized;
        transient T value;
        private static final long serialVersionUID = 0L;

        MemorizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        public T get() {
            if (!this.initialized) {
                synchronized (this) {
                    if (!this.initialized) {
                        T t = this.delegate.get();
                        this.value = t;
                        this.initialized = true;
                        return t;
                    }
                }
            }
            return this.value;
        }

        public synchronized void reset() {
            this.initialized = false;
        }

        public String toString() {
            return "Suppliers.memoize(" + this.delegate + ")";
        }
    }
}
