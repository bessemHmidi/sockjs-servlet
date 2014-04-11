/**
 * Copyright (C) 2014 Red Hat, Inc, and individual contributors.
 * Copyright (C) 2011-2012 VMware, Inc.
 */

package org.projectodd.sockjs;

public class JsonpReceiver extends ResponseReceiver {

    public JsonpReceiver(SockJsRequest req, SockJsResponse res, Server.Options options, String callback) {
        super(req, res, options);
        this.callback = callback;
        protocol = "jsonp-polling";
        maxResponseSize = 1;
    }

    @Override
    public boolean doSendFrame(String payload) {
        System.err.println("!!! JsonpReceiver doSendFrame " + payload);
        return super.doSendFrame(callback + "(" + Utils.jsonStringify(payload) + ");\r\n");
    }

    private String callback;
}