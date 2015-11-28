package com.em.tracker.services.impl;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.em.tracker.AppController;
import com.em.tracker.helper.CallBack;
import com.em.tracker.helper.JacksonJsonRequest;
import com.em.tracker.helper.ResponseEntity;
import com.em.tracker.utils.EmUtils;
import com.em.tracker.vo.Login;
import com.em.tracker.vo.Route;
import com.em.tracker.vo.Tracker;
import com.em.tracker.vo.TrackerRoute;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EMServiceImpl implements com.em.tracker.services.EMService {

    public void authenticateTracker(Login login, final CallBack<com.em.tracker.helper.ResponseEntity<com.em.tracker.vo.Tracker>> successCallBack, final com.em.tracker.helper.CallBack<VolleyError> failureCallBack) {
        String serviceUrl = EmUtils.getServerUrl() + "trackerLogin";
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
                new Response.Listener<ResponseEntity<Tracker>>() {
                    @Override
                    public void onResponse(ResponseEntity<Tracker> response) {
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
                return new TypeReference<com.em.tracker.helper.ResponseEntity<com.em.tracker.vo.Tracker>>() {
                };
            }
        };
        AppController.getInstance().addToRequestQueue(jacksonJsonRequest);
    }

    public void getTrackerRoute(String routeId, final CallBack<ResponseEntity<TrackerRoute>> successCallBack, final CallBack<VolleyError> failureCallBack) {
        String serviceUrl = EmUtils.getServerUrl() + "trackerRoute/" + routeId;
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
                new Response.Listener<ResponseEntity<TrackerRoute>>() {
                    @Override
                    public void onResponse(ResponseEntity<TrackerRoute> response) {
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
                return new TypeReference<ResponseEntity<TrackerRoute>>() {
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
}