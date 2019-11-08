package com.m.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.m.helper.Auth.AuthStateManager;
import com.m.androidNativeApp.MainActivity;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

public class LoginActivity extends AppCompatActivity {

    private String K_REDIRECT_URI = "com.m.androidnativeapp:/oauth2redirect";
    private int RC_AUTH = 100;
    private static AuthState mAuthState;
    private AuthorizationService mAuthService;
    private AuthStateManager mAuthStateManager;
    private MobileService mobileService;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthStateManager = AuthStateManager.getInstance(this);
        context = getApplicationContext();
        mobileService = MobileService.getInstance(context.getApplicationContext());

        if (mAuthStateManager.getCurrent().isAuthorized()) {
            mAuthStateManager.getCurrent().createTokenRefreshRequest();
            System.out.println("User is authorized already");

            startActivity(new Intent(this, MainActivity.class));
            finish();


        } else {
            mAuthService = new AuthorizationService(this);
            doAuth();
        }
    }

    public void doAuth() {

        final AuthorizationServiceConfiguration.RetrieveConfigurationCallback retrieveCallback =
                (authorizationServiceConfiguration, e) -> {
                    if (e != null) {
                        System.out.println("Failed to retrieve configuration for " + mobileService.getKIssuer());
                    } else {
                        makeAuthRequest(authorizationServiceConfiguration);
                    }
                };

        String discoveryEndpoint = mobileService.getKIssuer() + ".well-known/openid-configuration";
        AuthorizationServiceConfiguration.fetchFromUrl(Uri.parse(discoveryEndpoint), retrieveCallback);
    }

    public void makeAuthRequest(AuthorizationServiceConfiguration serviceConfiguration) {
        AuthorizationRequest.Builder authorizationRequest = new AuthorizationRequest.Builder(
                serviceConfiguration,
                mobileService.getKClientId(),
                ResponseTypeValues.CODE,
                Uri.parse(K_REDIRECT_URI));
        AuthorizationRequest authRequest = authorizationRequest.build();
        Intent authIntent = mAuthService.getAuthorizationRequestIntent(authRequest);
        startActivityForResult(authIntent, RC_AUTH);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTH) {
            AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
            AuthorizationException ex = AuthorizationException.fromIntent(data);
            mAuthStateManager.updateAfterAuthorization(resp, ex);
            mAuthState = new AuthState(resp, ex);

            exchangeAuthorizationCode(resp);


        } else {
            System.out.println("Error, wrong response code");
        }
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }

    private void performTokenRequest(TokenRequest request) {
        mAuthService.performTokenRequest(
                request,
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(
                            @Nullable TokenResponse tokenResponse,
                            @Nullable AuthorizationException ex) {
                        receivedTokenResponse(tokenResponse, ex);
                        mAuthStateManager.updateAfterTokenResponse(tokenResponse, ex);
                    }
                });
    }

    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        mAuthState.update(tokenResponse, authException);
        startActivity(new Intent(this, MainActivity.class));
    }

}