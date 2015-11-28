package com.em.tracker.services;

import com.android.volley.VolleyError;
import com.em.tracker.helper.CallBack;
import com.em.tracker.helper.ResponseEntity;
import com.em.tracker.vo.Login;
import com.em.tracker.vo.Route;
import com.em.tracker.vo.Tracker;
import com.em.tracker.vo.TrackerRoute;

import java.util.List;

public interface EMService {
    public void authenticateTracker(Login login, final CallBack<ResponseEntity<Tracker>> successCallBack, final CallBack<VolleyError> failureCallBack);

    public void getTrackerRoute(String trackerId, final CallBack<ResponseEntity<TrackerRoute>> successCallBack, final CallBack<VolleyError> failureCallBack);

    public void getRoute(String routeId, final CallBack<ResponseEntity<List<Route>>> successCallBack, final CallBack<VolleyError> failureCallBack);
}
