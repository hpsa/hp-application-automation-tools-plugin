// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.client;

import hudson.util.TimeUnit2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RetryModelTest {

    private RetryModel retryModel;
    private TestTimeProvider testTimeProvider;
    private TestEventPublisher testEventPublisher;

    @Before
    public void init() {
        testEventPublisher = new TestEventPublisher();
        retryModel = new RetryModel(testEventPublisher);
        testTimeProvider = new TestTimeProvider();
        retryModel.setTimeProvider(testTimeProvider);
    }

    @Test
    public void testRetryModel() {
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 1 minute
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(25));
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 10 minutes
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(4));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(4));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(4));
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 60 minutes
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(25));
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 60 minutes
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.MINUTES.toMillis(25));
        Assert.assertFalse(retryModel.isQuietPeriod());
    }

    @Test
    public void testRetryModelSuccess() {
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 1 minute
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(25));
        Assert.assertTrue(retryModel.isQuietPeriod());
        Assert.assertEquals(0, testEventPublisher.getResumeCount());
        retryModel.success();
        Assert.assertFalse(retryModel.isQuietPeriod());
        Assert.assertEquals(1, testEventPublisher.getResumeCount());
    }

    private static class TestTimeProvider implements RetryModel.TimeProvider {

        private long time = System.currentTimeMillis();

        public void addOffset(long time) {
            this.time += time;
        }

        @Override
        public long getTime() {
            return time;
        }
    }
}
