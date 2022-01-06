package com.beaver.apigw.common.exception;

public class ConfigException extends Exception {
    public ConfigException(String msg) {
        super(msg);
    }

    public ConfigException(String msg, Exception e) {
        super(msg, e);
    }
}