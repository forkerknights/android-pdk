package com.github.forkerknights;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.pinterest.android.pdk.PDKBoard;
import com.pinterest.android.pdk.PDKCallback;
import com.pinterest.android.pdk.PDKClient;
import com.pinterest.android.pdk.PDKException;
import com.pinterest.android.pdk.PDKResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private static final String[] SCOPES = new String[]{
            PDKClient.PDKCLIENT_PERMISSION_READ_PUBLIC,
            PDKClient.PDKCLIENT_PERMISSION_WRITE_PUBLIC
    };
    private static final String GREETING_TAG = "GREETING_SAMPLE";

    private PDKClient pdkClient;
    private Handler mHandler = new Handler();
    private Button btnLogin;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String appID = getString(R.string.app_id);
        Button btnBoards = (Button) findViewById(R.id.btnListBoards);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnBoards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnBoards_onClick();
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnLogin_onClick();
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnLogout_onClick();
            }
        });
        if(appID.equals("defineIt")) {
            Toast.makeText(this, "You need to define the application id", Toast.LENGTH_LONG).show();
        } else {
            PDKClient.setDebugMode(true);
            pdkClient = PDKClient.configureInstance(this, appID);
            pdkClient.onConnect(this, new PDKCallback() {
                // If for whatever reason the onConnect fails we trigger the silent login
                @Override
                public void onExecutionAborted(String cause) {
                    launchSilentLogin();
                }

                @Override
                public void onFailure(PDKException exception) {
                    launchSilentLogin();
                }

                // If it succeeds recover the user

                @Override
                public void onSuccess(PDKResponse response) {
                    onLoginSuccess(response, false); // This doesn't count as a silent login
                }
            });
        }
    }

    private void launchSilentLogin() {
        PDKClient.getInstance().silentLogin(this, Arrays.asList(SCOPES), new PDKCallback() {
            @Override
            public void onSuccess(PDKResponse response) {
                onLoginSuccess(response, true);
            }

            @Override
            public void onFailure(PDKException exception) {
                onLoginFailure(exception, true);
            }
        });
    }

    private void btnLogout_onClick() {
        Toast.makeText(this, "Good bye", Toast.LENGTH_LONG).show();
        PDKClient.getInstance().logout();
        btnLogin.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.GONE);
    }

    private void btnLogin_onClick() {
        pdkClient.login(this, Arrays.asList(SCOPES), new PDKCallback() {
            @Override
            public void onSuccess(PDKResponse response) {
                onLoginSuccess(response, false);
            }

            @Override
            public void onFailure(PDKException exception) {
                onLoginFailure(exception, false);
            }
        });
    }

    private void onLoginSuccess(PDKResponse response, boolean silent) {
        Log.d(GREETING_TAG, "onLoginSuccess (silent="+silent+")");
        btnLogin.setVisibility(View.GONE);
        btnLogout.setVisibility(View.VISIBLE);
        String user = response.getUser().getFirstName();
        if(silent) {
            Toast.makeText(this, "Welcome back "+user, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Hello "+user, Toast.LENGTH_LONG).show();
        }
    }

    private void onLoginFailure(PDKException exception, boolean silent) {
        Log.d(GREETING_TAG, "onLoginFailure (message="+exception.getMessage()+", silent="+silent+")");
        btnLogin.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.GONE);
        if(!silent) {
            // exception.printStackTrace();
            Toast.makeText(this, "Unable to login", Toast.LENGTH_LONG).show();
            Toast.makeText(this,"Reason: "+exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void btnBoards_onClick() {
        if(!PDKClient.isAuthenticated()) {
            Toast.makeText(this, "You need to login first", Toast.LENGTH_LONG).show();
        } else {
            PDKClient.getInstance().getMyBoards("name", new PDKCallback() {
                @Override
                public void onSuccess(PDKResponse response) {
                    fillBoardsLv(response);
                }
            });
        }
    }

    private void fillBoardsLv(PDKResponse response) {
        List<String> boards = new ArrayList<>();
        for(PDKBoard board:response.getBoardList()) {
            boards.add(board.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, boards);
        ((ListView)findViewById(R.id.lvBoards)).setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        pdkClient.onOauthResponse(requestCode, resultCode, data);
    }
}
