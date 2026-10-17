package com.suite.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

/** Writes target/extent-report/index.html with a pass/fail summary. */
public class ExtentListener implements ITestListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    @Override
    public synchronized void onStart(ITestContext context) {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report/index.html");
            spark.config().setDocumentTitle("API-DB Validation Report");
            spark.config().setReportName("REST API Automation - Data Validation Suite");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Stack", "Java, RestAssured, TestNG, JDBC, MySQL");
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        if (result.getParameters().length > 0) {
            name += " " + Arrays.toString(result.getParameters());
        }
        CURRENT.set(extent.createTest(name, result.getMethod().getDescription()));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        CURRENT.get().pass("Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        CURRENT.get().fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest t = CURRENT.get();
        if (t == null) {
            t = extent.createTest(result.getMethod().getMethodName());
        }
        t.skip(result.getThrowable() != null ? result.getThrowable().toString() : "Skipped");
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
        }
    }
}
