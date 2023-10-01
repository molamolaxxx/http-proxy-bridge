package com.mola;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.http.client.config.RequestConfig.custom;

/**
 * @author : molamola
 * @Project: InvincibleSchedulerEngine
 * @Description:
 * @date : 2020-09-22 18:07
 **/
public enum HttpCommonService {

    INSTANCE;

    private Logger logger = LoggerFactory.getLogger(HttpCommonService.class);

    private AtomicBoolean monitorThreadStart = new AtomicBoolean(false);

    private HttpClientConnectionManager connectionManager = buildConnectionManager();

    private Thread monitorThread = new IdleConnectionMonitorThread(connectionManager);

    private volatile HttpClient httpClient;

    HttpCommonService() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(buildKeepAliveStrategy());
        this.httpClient = httpClientBuilder.build();
    }

    public void useProxy(String proxy) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(buildKeepAliveStrategy());
        if (proxy != null) {
            this.httpClient = httpClientBuilder.setProxy(HttpHost.create(proxy)).build();
        }
        this.httpClient = httpClientBuilder.build();
    }

    private HttpClientConnectionManager buildConnectionManager() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(50);
        connectionManager.setMaxTotal(100);
        return connectionManager;
    }

    private ConnectionKeepAliveStrategy buildKeepAliveStrategy() {
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 5 * 1000;
        };
    }

    private void bootMonitorThread() {
        if (monitorThreadStart.compareAndSet(false, true)) {
            logger.info("[HttpService#bootMoniterThread] monitor thread start");
            monitorThread.start();
        }
    }

    public String mapToUrl(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        try {
            boolean isFirst = true;
            for (String key : params.keySet()) {
                if (isFirst) {
                    sb.append(key + "=" + params.get(key));
                    isFirst = false;
                } else {
                    if (params.get(key) != null) {
                        sb.append("&" + key + "=" + params.get(key));
                    } else {
                        sb.append("&" + key + "=");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[HttpUtil$mapToUrl] convert map to url exception", e);
        }
        return sb.toString();
    }

    public String get(String baseUrl, String serviceName, Map<String, Object> params, int timeout) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl).append(serviceName);
        if (null != params && params.size() > 0) {
            sb.append("?").append(mapToUrl(params));
        }
        return get(sb.toString(), timeout);

    }

    private HttpClientContext buildContext(int timeout) {
        HttpClientContext context = HttpClientContext.create();
        RequestConfig config = custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout).build();
        context.setRequestConfig(config);
        return context;
    }

    public String get(String url, int timeout) throws Exception {
        return get(url, timeout, null);
    }

    public String get(String url, int timeout, Header[] headers) throws Exception {
        bootMonitorThread();
        URI uri = new URIBuilder(url).build();
        HttpGet httpGet = new HttpGet(uri);
        if (null != headers) {
            httpGet.setHeaders(headers);
        }
        return httpClient.execute(httpGet, response -> {
            int statusCode = response.getStatusLine().getStatusCode();
            if (isSuccess(statusCode)) {
                return EntityUtils.toString(response.getEntity());
            } else {
                String errorMsg = EntityUtils.toString(response.getEntity());
                throw new RuntimeException(String.format("requestGet remote error, url=%s, code=%d, error msg=%s",
                        uri.toString(), statusCode, errorMsg));
            }
        }, buildContext(timeout));
    }


    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private class IdleConnectionMonitorThread extends Thread {
        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown = false;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            super("Http-Connection-Monitor-Thread");
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(10, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
