package com.em.client.activity.em;

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
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.em.R;
import com.em.client.AppController;
import com.em.client.activity.LoginSettingActivity;
import com.em.client.helper.CallBack;
import com.em.client.helper.ResponseEntity;
import com.em.client.services.EMService;
import com.em.client.services.impl.EMServiceImpl;
import com.em.client.utils.EmUtils;
import com.em.client.vo.Login;
import com.em.client.vo.Route;
import com.em.client.vo.User;

import java.util.List;

public class EmLoginActivity extends ActionBarActivity {
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
        progressDialog = new ProgressDialog(this);
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
                    final EMService emService = new EMServiceImpl();
                    EmUtils.showProgressDialog(progressDialog);
                    emService.authenticateUser(login, new CallBack<ResponseEntity<User>>() {
                        @Override
                        public void callBack(ResponseEntity<User> responseEntity) {
                            if (!responseEntity.getError()) {
                                final User user = responseEntity.getResponseData();
                                AppController.getInstance().setUser(user);
                                //TODO: Read route id form server
                                emService.getRoute("RT001", new CallBack<ResponseEntity<List<Route>>>() {
                                    @Override
                                    public void callBack(ResponseEntity<List<Route>> responseEntity) {
                                        if (!responseEntity.getError()) {
                                            AppController.getInstance().setRoute(responseEntity.getResponseData().get(0));
                                            Intent i = new Intent(EmLoginActivity.this, EmLauncherActivity.class);
                                            i.putExtra("data", "value");
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
            Intent i = new Intent(this, LoginSettingActivity.class);
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