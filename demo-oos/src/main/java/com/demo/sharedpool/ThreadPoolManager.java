package com.demo.sharedpool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public interface ThreadPoolManager {

	Executor executor = Executors.newFixedThreadPool(10);
}
