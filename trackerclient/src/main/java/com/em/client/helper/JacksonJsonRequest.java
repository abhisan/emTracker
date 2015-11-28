package com.em.client.helper;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class JacksonJsonRequest<K, T> extends Request<T> {

    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/json; charset=%s", PROTOCOL_CHARSET);
    private final Response.Listener<T> mListener;
    private final K mRequestBody;
    private final Class<T> mClass;
    private final Map<String, String> mHeaders;
    private ObjectMapper mMapper = new ObjectMapper();


    public JacksonJsonRequest(int method, String url, K requestBody,
                              Response.ErrorListener errorListener, Response.Listener<T> listener,
                              Map<String, String> headers, Class<T> claz) {
        super(method, url, errorListener);
        mListener = listener;
        mRequestBody = requestBody;
        mHeaders = headers;
        mClass = claz;
        mMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse networkResponse) {
        String jsonString = new String(networkResponse.data);
        try {
            T result;
            if (getTypeReference() != null)
                result = mMapper.readValue(jsonString, getTypeReference());
            else {
                result = mMapper.readValue(jsonString, mClass);
            }
            return Response.success(result, HttpHeaderParser.parseCacheHeaders(networkResponse));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected TypeReference getTypeReference() {
        return null;
    }

    @Override
    protected void deliverResponse(T t) {
        mListener.onResponse(t);
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() {
        try {
            return mRequestBody == null ? null : mMapper.writeValueAsBytes(mRequestBody);
        } catch (JsonProcessingException e) {

        }
        return null;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders == null) ? super.getHeaders() : mHeaders;
    }
}