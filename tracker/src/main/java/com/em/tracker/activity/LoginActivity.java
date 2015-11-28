package com.em.tracker.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.em.tracker.AppController;
import com.em.tracker.R;
import com.em.tracker.helper.CallBack;
import com.em.tracker.helper.ResponseEntity;
import com.em.tracker.services.EMService;
import com.em.tracker.services.impl.EMServiceImpl;
import com.em.tracker.utils.EmUtils;
import com.em.tracker.vo.Login;
import com.em.tracker.vo.Route;
import com.em.tracker.vo.Tracker;
import com.em.tracker.vo.TrackerRoute;

import java.util.List;

public class LoginActivity extends ActionBarActivity {
    private EditText userText;
    private EditText passText;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        checkInternetConnection();
        userText = (EditText) findViewById(R.id.userid);
        passText = (EditText) findViewById(R.id.password);
        Button login = (Button) findViewById(R.id.sign_in_button);
        final CheckBox saveCredentials = (CheckBox) findViewById(R.id.staySignedIn);
        if (EmUtils.isCredentialSaved()) {
            userText.setText(EmUtils.getUserId());
            passText.setText(EmUtils.getPassword());
            saveCredentials.setChecked(true);
        }
        progressDialog = new ProgressDialog(this);
        final Activity _this = this;
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String userName = userText.getText().toString();
                String password = passText.getText().toString();
                Resources res = getResources();
                if (TextUtils.isEmpty(userName)) {
                    userText.setError(res.getString(R.string.error_field_required));
                } else if (TextUtils.isEmpty(password)) {
                    passText.setError(res.getString(R.string.error_field_required));
                } else {
                    Login login = new Login();
                    login.setUserName(userName);
                    login.setPassword(password);
                    if (saveCredentials.isChecked()) {
                        EmUtils.setUserId(userName);
                        EmUtils.setPassword(password);
                    } else {
                        EmUtils.setUserId("");
                        EmUtils.setPassword("");
                    }
                    final EMService emService = new EMServiceImpl();
                    com.em.tracker.utils.EmUtils.showProgressDialog(progressDialog);
                    emService.authenticateTracker(login, new CallBack<ResponseEntity<Tracker>>() {
                        @Override
                        public void callBack(ResponseEntity<Tracker> responseEntity) {
                            if (!responseEntity.getError()) {
                                AppController.getInstance().setTracker(responseEntity.getResponseData());
                                EmUtils.showProgressDialog(progressDialog);
                                emService.getTrackerRoute(responseEntity.getResponseData().getTrackerData().getTrackerId(), new CallBack<ResponseEntity<TrackerRoute>>() {
                                    @Override
                                      public void callBack(ResponseEntity<TrackerRoute> responseEntity) {
                                        if (!responseEntity.getError()) {
                                            emService.getRoute(responseEntity.getResponseData().getRouteId(), new CallBack<ResponseEntity<List<Route>>>() {
                                                @Override
                                                public void callBack(ResponseEntity<List<Route>> responseEntity) {
                                                    if (!responseEntity.getError()) {
                                                        Route route = responseEntity.getResponseData().get(0);
                                                        AppController.getInstance().setRoute(route);
                                                        Intent i = new Intent(LoginActivity.this, LauncherActivity.class);
                                                        startActivity(i);
                                                    } else {
                                                        Toast.makeText(getApplicationContext(), responseEntity.getErrorMsg(), Toast.LENGTH_SHORT).show();
                                                    }
                                                    EmUtils.hideProgressDialog(progressDialog);
                                                }
                                            }, new CallBack<VolleyError>() {
                                                @Override
                                                public void callBack(VolleyError volleyError) {
                                                    EmUtils.hideProgressDialog(progressDialog);
                                                    Toast.makeText(getApplicationContext(), "Could not connect to server.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            Toast.makeText(getApplicationContext(), responseEntity.getErrorMsg(), Toast.LENGTH_SHORT).show();
                                        }
                                        EmUtils.hideProgressDialog(progressDialog);
                                    }
                                }, new CallBack<VolleyError>() {
                                    @Override
                                    public void callBack(VolleyError volleyError) {
                                        EmUtils.hideProgressDialog(progressDialog);
                                        Toast.makeText(getApplicationContext(), "Could not connect to server.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(getApplicationContext(), responseEntity.getErrorMsg(), Toast.LENGTH_SHORT).show();
                            }
                            com.em.tracker.utils.EmUtils.hideProgressDialog(progressDialog);
                        }
                    }, new com.em.tracker.helper.CallBack<VolleyError>() {
                        @Override
                        public void callBack(VolleyError volleyError) {
                            com.em.tracker.utils.EmUtils.hideProgressDialog(progressDialog);
                            Toast.makeText(getApplicationContext(), "Could not connect to server.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, com.em.tracker.activity.LoginSettingActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
}