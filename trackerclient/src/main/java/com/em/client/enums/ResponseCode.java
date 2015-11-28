package com.em.client.enums;

public enum ResponseCode {
    OK(200), AUTH_FAILED(400), INVALID_RESOURCE(401);
    int resCode;

    ResponseCode(int resCode) {
        this.resCode = resCode;
    }

    public int getResCode() {
        return resCode;
    }
}
