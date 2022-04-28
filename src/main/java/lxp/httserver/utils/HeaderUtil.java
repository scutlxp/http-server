package lxp.httserver.utils;

import io.netty.handler.codec.http.HttpHeaders;

import java.util.Map;

public class HeaderUtil {
    public static int headerSize(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return 0;
        }

        int size = 0;
        for (Map.Entry<String, String> entry : headers.entries()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null) {
                continue;
            }
            size += name.length();
            if (value != null) {
                size += value.length();
            }
        }

        return size;
    }
}
