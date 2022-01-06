package com.beaver.apigw.common.conf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class UpStreamServer {
    private int keepalive;

    private String ip;

    private int port;

    public UpStreamServer(String url, int keepalive) {
        this.keepalive = keepalive;
        int pidx = url.lastIndexOf(':');
        if (pidx >= 0) {
            // otherwise : is at the end of the string, ignore
            if (pidx < url.length() - 1) {
                this.port = Integer.parseInt(url.substring(pidx + 1));
            }
            this.ip = url.substring(0, pidx);
        }
    }


}
