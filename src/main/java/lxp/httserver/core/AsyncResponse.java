package lxp.httserver.core;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lxp.httserver.utils.HeaderUtil;
import lxp.httserver.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class AsyncResponse {
    protected int mCode = 200;
    protected byte[] mBody;
    protected volatile HttpHeaders mHeaders = new DefaultHttpHeaders();
    protected volatile HttpHeaders mTrailingHeaders = new DefaultHttpHeaders();

    /**
     * 防止end方法重复调用
     */
    private volatile int writeAble = 1;

    private static final AtomicIntegerFieldUpdater<AsyncResponse> WRITE_ABLE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AsyncResponse.class, "writeAble");

    private List<EndListener> endListeners = new ArrayList<>();

    public void addHeader(String name, String value) {
        mHeaders.add(name, value);
    }

    public void setHeader(String name, String value) {
        mHeaders.set(name, value);
    }

    public void setHeader(String name, List<String> values) {
        mHeaders.set(name, values);
    }

    public void addTrailingHeader(String name, String value) {
        mTrailingHeaders.add(name, value);
    }

    public void setTrailingHeader(String name, String value) {
        mTrailingHeaders.set(name, value);
    }

    public void setTrailingHeader(String name, List<String> values) {
        mTrailingHeaders.set(name, values);
    }

    public boolean isWritable() {
        return writeAble == 1;
    }

    public int getStatusCode() {
        return mCode;
    }

    public String getHeader(String name) {
        return mHeaders.get(name);
    }

    public int getHeaderSize() {
        return HeaderUtil.headerSize(mHeaders);
    }

    public int getBodySize() {
        return mBody == null ? 0 : mBody.length;
    }

    public byte[] getBody() {
        return mBody;
    }

    public void addEndListener(EndListener endListener) {
        if (endListener != null) {
            endListeners.add(endListener);
        }
    }

    public void end(int code, HttpHeaders headers, HttpHeaders trailingHeaders, byte[] body) {
        if (!WRITE_ABLE_UPDATER.compareAndSet(this, 1, 0)) {
            LoggerUtils.HTTP_SERVER_LOG.warn("call AsyncResponse.end() method more than 1 times, please check code");
            return;
        }

        mCode = code;
        mBody = body;

        if (headers != null) {
            headers.setAll(mHeaders);
            mHeaders = headers;
        }

        if (trailingHeaders != null) {
            trailingHeaders.setAll(mTrailingHeaders);
            mTrailingHeaders = trailingHeaders;
        }

        try {
            doWriteResponse();
        } finally {
            callEndListener();
        }
    }

    public void end(int code, HttpHeaders headers, byte[] body) {
        end(code, headers, null, body);
    }

    private void callEndListener() {
        for (EndListener endListener : endListeners) {
            try {
                endListener.onEnd(this);
            } catch (Exception e) {
                LoggerUtils.HTTP_SERVER_LOG.error("call end listener error,", e);
            }
        }
    }

    protected abstract void doWriteResponse();

    public interface EndListener {
        void onEnd(AsyncResponse asyncResponse);
    }
}
