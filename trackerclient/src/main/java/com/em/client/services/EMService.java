package com.em.client.services;

import com.android.volley.VolleyError;
import com.em.client.helper.CallBack;
import com.em.client.helper.ResponseEntity;
import com.em.client.vo.Login;
import com.em.client.vo.Route;
import com.em.client.vo.User;
import com.em.client.vo.UserRoute;

import java.util.List;

public interface EMService {
    public void authenticateUser(Login login, final CallBack<ResponseEntity<User>> successCallBack, final CallBack<VolleyError> failureCallBack);

    public void getUserRoute(String userName, final CallBack<ResponseEntity<UserRoute>> successCallBack, final CallBack<VolleyError> failureCallBack);

    public void getRoute(String routeId, final CallBack<ResponseEntity<List<Route>>> successCallBack, final CallBack<VolleyError> failureCallBack);
}
