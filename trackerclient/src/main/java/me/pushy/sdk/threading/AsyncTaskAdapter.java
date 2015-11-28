/*
 * Decompiled with CFR 0_102.
 * 
 * Could not load the following classes:
 *  android.os.Handler
 *  android.os.Looper
 *  android.os.Message
 *  android.os.Process
 *  android.util.Log
 */
package me.pushy.sdk.threading;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AsyncTaskAdapter<Params, Progress, Result> {
    private static final String LOG_TAG = "AsyncTaskAdapter";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTaskAdapter #" + this.mCount.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(128);
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 1, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
    private static final int MESSAGE_POST_RESULT = 1;
    private static final int MESSAGE_POST_PROGRESS = 2;
    private static final InternalHandler sHandler = new InternalHandler();
    private static volatile Executor sDefaultExecutor = THREAD_POOL_EXECUTOR;
    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;
    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();
    private volatile Status mStatus = Status.PENDING;

    public AsyncTaskAdapter() {
        this.mWorker = new WorkerRunnable<Params, Result>() {

            @Override
            public Result call() throws Exception {
                AsyncTaskAdapter.this.mTaskInvoked.set(true);
                Process.setThreadPriority((int) 10);
                return (Result) AsyncTaskAdapter.this.postResult(AsyncTaskAdapter.this.doInBackground(this.mParams));
            }
        };
        this.mFuture = new FutureTask<Result>(this.mWorker) {

            @Override
            protected void done() {
                try {
                    AsyncTaskAdapter.this.postResultIfNotInvoked(this.get());
                } catch (InterruptedException e) {
                    Log.w((String) "AsyncTaskAdapter", (Throwable) e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()", e.getCause());
                } catch (CancellationException e) {
                    AsyncTaskAdapter.this.postResultIfNotInvoked(null);
                }
            }
        };
    }

    public static void init() {
        sHandler.getLooper();
    }

    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }

    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    private void postResultIfNotInvoked(Result result) {
        boolean wasTaskInvoked = this.mTaskInvoked.get();
        if (!wasTaskInvoked) {
            this.postResult(result);
        }
    }

    private Result postResult(Result result) {
        Message message = sHandler.obtainMessage(1, new AsyncTaskAdapterResult<Object>(this, result));
        message.sendToTarget();
        return result;
    }

    public final Status getStatus() {
        return this.mStatus;
    }

    protected /* varargs */ abstract Result doInBackground(Params... var1);

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected /* varargs */ void onProgressUpdate(Progress... values) {
    }

    protected void onCancelled(Result result) {
        this.onCancelled();
    }

    protected void onCancelled() {
    }

    public final boolean isCancelled() {
        return this.mCancelled.get();
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        this.mCancelled.set(true);
        return this.mFuture.cancel(mayInterruptIfRunning);
    }

    public final Result get() throws InterruptedException, ExecutionException {
        return this.mFuture.get();
    }

    public final Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.mFuture.get(timeout, unit);
    }

    public final /* varargs */ AsyncTaskAdapter<Params, Progress, Result> execute(Params... params) {
        return this.executeOnExecutor(sDefaultExecutor, params);
    }

    public final /* varargs */ AsyncTaskAdapter<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
        if (this.mStatus != Status.PENDING) {
            switch (this.mStatus) {
                case RUNNING: {
                    throw new IllegalStateException("Cannot execute task: the task is already running.");
                }
                case FINISHED: {
                    throw new IllegalStateException("Cannot execute task: the task has already been executed (a task can be executed only once)");
                }
            }
        }
        this.mStatus = Status.RUNNING;
        this.onPreExecute();
        this.mWorker.mParams = params;
        exec.execute(this.mFuture);
        return this;
    }

    protected final /* varargs */ void publishProgress(Progress... values) {
        if (!this.isCancelled()) {
            sHandler.obtainMessage(2, new AsyncTaskAdapterResult<Progress>(this, values)).sendToTarget();
        }
    }

    private void finish(Result result) {
        if (this.isCancelled()) {
            this.onCancelled(result);
        } else {
            this.onPostExecute(result);
        }
        this.mStatus = Status.FINISHED;
    }

    public static enum Status {
        PENDING,
        RUNNING,
        FINISHED;
        private Status() {
        }
    }

    private static class AsyncTaskAdapterResult<Data> {
        final AsyncTaskAdapter mTask;
        final Data[] mData;
        /* varargs */ AsyncTaskAdapterResult(AsyncTaskAdapter task, Data... data) {
            this.mTask = task;
            this.mData = data;
        }
    }

    private static abstract class WorkerRunnable<Params, Result>
            implements Callable<Result> {
        Params[] mParams;

        private WorkerRunnable() {
        }
    }

    private static class InternalHandler
            extends Handler {
        private InternalHandler() {
        }

        public void handleMessage(Message msg) {
            AsyncTaskAdapterResult result = (AsyncTaskAdapterResult) msg.obj;
            switch (msg.what) {
                case 1: {
                    result.mTask.finish(result.mData[0]);
                    break;
                }
                case 2: {
                    result.mTask.onProgressUpdate(result.mData);
                }
            }
        }
    }

}

