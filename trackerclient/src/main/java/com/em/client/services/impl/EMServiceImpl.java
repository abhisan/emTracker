package com.em.client.services.impl;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.em.client.AppController;
import com.em.client.helper.CallBack;
import com.em.client.helper.JacksonJsonRequest;
import com.em.client.helper.ResponseEntity;
import com.em.client.services.EMService;
import com.em.client.utils.EmUtils;
import com.em.client.vo.Login;
import com.em.client.vo.Route;
import com.em.client.vo.User;
import com.em.client.vo.UserRoute;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EMServiceImpl implements EMService {

    public void authenticateUser(Login login, final CallBack<ResponseEntity<User>> successCallBack, final CallBack<VolleyError> failureCallBack) {
        String serviceUrl = EmUtils.getServerUrl() + "login";
        JacksonJsonRequest jacksonJsonRequest = new JacksonJsonRequest(
                Request.Method.POST,
                serviceUrl,
                login,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        failureCallBack.callBack(error);
                    }
                },
                new Response.Listener<ResponseEntity<User>>() {
                    @Override
                    public void onResponse(ResponseEntity<User> response) {
                        if (response != null) {
                            successCallBack.callBack(response);
                        }
                    }
                },
                null, ResponseEntity.class) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json; charset=utf-8");
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }

            @Override
            protected TypeReference getTypeReference() {
                return new TypeReference<ResponseEntity<User>>() {
                };
            }
        };
        AppController.getInstance().addToRequestQueue(jacksonJsonRequest);
    }

    public void getRoute(String routeId, final CallBack<ResponseEntity<List<Route>>> successCallBack, final CallBack<VolleyError> failureCallBack) {
        String serviceUrl = EmUtils.getServerUrl() + "routes/" + routeId;
        JacksonJsonRequest jacksonJsonRequest = new JacksonJsonRequest(
                Request.Method.GET,
                serviceUrl,
                null,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        failureCallBack.callBack(error);
                    }
                },
                new Response.Listener<ResponseEntity<List<Route>>>() {
                    @Override
                    public void onResponse(ResponseEntity<List<Route>> response) {
                        if (response != null) {
                            successCallBack.callBack(response);
                        }
                    }
                },
                null, ResponseEntity.class) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json; charset=utf-8");
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }

            @Override
            protected TypeReference getTypeReference() {
                return new TypeReference<ResponseEntity<List<Route>>>() {
                };
            }
        };
        AppController.getInstance().addToRequestQueue(jacksonJsonRequest);
    }

    public void getUserRoute(String userName, final CallBack<ResponseEntity<UserRoute>> successCallBack, final CallBack<VolleyError> failureCallBack) {
        String serviceUrl = EmUtils.getServerUrl() + "userStopRoute/" + userName;
        JacksonJsonRequest jacksonJsonRequest = new JacksonJsonRequest(
                Request.Method.GET,
                serviceUrl,
                null,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        failureCallBack.callBack(error);
                    }
                },
                new Response.Listener<ResponseEntity<UserRoute>>() {
                    @Override
                    public void onResponse(ResponseEntity<UserRoute> response) {
                        if (response != null) {
                            successCallBack.callBack(response);
                        }
                    }
                },
                null, ResponseEntity.class) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json; charset=utf-8");
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }

            @Override
            protected TypeReference getTypeReference() {
                return new TypeReference<ResponseEntity<UserRoute>>() {
                };
            }
        };
        AppController.getInstance().addToRequestQueue(jacksonJsonRequest);
    }
}