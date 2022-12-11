package com.styra.run.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTests {
    private static final TestServer testServer = new TestServer(3000);

    @BeforeAll
    static void setup() throws Exception {
        testServer.start();
    }

    @AfterAll
    static void teardown() throws Exception {
        testServer.stop();
    }

    @Test
    void test() throws IOException, InterruptedException {
        var process = new ProcessBuilder("make", "test")
                .directory(new File("./styra-run-sdk-tests"))
                .start();
        var finished = process.waitFor(5, TimeUnit.MINUTES);
        assertTrue(finished);

        var stdOut = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
        var stdErr = new String(process.getErrorStream().readAllBytes(), Charset.defaultCharset());
        System.err.println(stdErr);

        assertEquals(0, process.exitValue(), stdOut);
    }
}
