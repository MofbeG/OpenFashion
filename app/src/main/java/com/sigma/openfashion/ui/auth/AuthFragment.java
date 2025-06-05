package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.Validator;
import com.sigma.openfashion.data.SupabaseService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Экран входа пользователя.
 */
public class AuthFragment extends Fragment {

    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton signInButton, signInEmailButton;
    private MaterialTextView errorText, forgotPasswordText;
    private ProgressBar authProgress;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emailEditText      = view.findViewById(R.id.emailEditText);
        passwordEditText   = view.findViewById(R.id.passwordEditText);
        signInButton       = view.findViewById(R.id.signInButton);
        forgotPasswordText = view.findViewById(R.id.forgotPasswordText);
        signInEmailButton  = view.findViewById(R.id.signInEmailButton);
        errorText          = view.findViewById(R.id.errorText);
        authProgress       = view.findViewById(R.id.authProgress);

        supabaseService = new SupabaseService();
        prefs           = SharedPrefHelper.getInstance(requireContext());

        signInButton.setOnClickListener(v -> attemptSignIn());
        view.findViewById(R.id.linearLayoutOnClick).setOnClickListener(v ->
                NavHostFragment.findNavController(AuthFragment.this)
                        .navigate(R.id.action_auth_to_signUp)
        );
        forgotPasswordText.setOnClickListener(v ->
                NavHostFragment.findNavController(AuthFragment.this)
                        .navigate(R.id.action_auth_to_passwordRecovery)
        );
        signInEmailButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isAuthEmail", true);
            NavHostFragment.findNavController(AuthFragment.this)
                    .navigate(R.id.action_auth_to_passwordRecovery, bundle);
        });

        String email = prefs.getUserEmail();
        if (email != null && !email.isEmpty())
            emailEditText.setText(email);
    }

    private void attemptSignIn() {
        String email    = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";

        if (!Validator.validateEmail(email, requireContext())) {
            emailEditText.setError(getString(R.string.Incorrect_email_format));
            return;
        }
        if (!Validator.validatePassword(password, requireContext())) {
            emailEditText.setError(getString(R.string.Incorrect_password_format));
            return;
        }
        hideError();
        showProgress(true);

        supabaseService.signIn(email, password, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                handleSignInSuccess(jsonResponse);
            }

            @Override
            public void onError(String errorMessage) {
                try {
                    JSONObject obj       = new JSONObject(errorMessage);
                    String msg           = obj.getString("msg");
                    showError(errorMessage);
                } catch (JSONException e) {
                    showError(getString(R.string.Parse_Error) + e.getMessage());
                }
            }
        });
    }

    private void handleSignInSuccess(String json) {
        requireActivity().runOnUiThread(() -> {
            try {
                JSONObject obj       = new JSONObject(json);
                String accessToken   = obj.optString("access_token", null);
                String refresh_token = obj.optString("refresh_token", null);
                JSONObject userObj   = obj.optJSONObject("user");
                String userId        = userObj != null ? userObj.optString("id", null) : null;

                if (userId != null) {
                    prefs.saveJwtToken(accessToken);
                    prefs.saveRefreshToken(refresh_token);
                    prefs.saveUserId(userId);
                    prefs.saveUserPin(null);
                    showProgress(false);

                    supabaseService.setJwtToken(accessToken);
                    supabaseService.getProfile(userId, new SupabaseService.QueryCallback() {
                        @Override
                        public void onSuccess(String jsonResponse) {
                            if (jsonResponse.trim().equals("[]")) {
                                runOnUi(() -> navigateToRegEntry(json));
                            } else {
                                runOnUi(() -> navigateToPinEntry());
                            }
                        }
                        @Override
                        public void onError(String errorMessage) {
                            showError(getString(R.string.Error__) + errorMessage);
                        }
                    });
                } else {
                    showError(getString(R.string.Invalid_server_response));
                }
            } catch (JSONException e) {
                showError(getString(R.string.Parse_Error) + e.getMessage());
            }
        });
    }

    private void showProgress(boolean show) {
        authProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        signInButton.setEnabled(!show);
        forgotPasswordText.setEnabled(!show);
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(message);
        });
    }

    private void hideError() {
        requireActivity().runOnUiThread(() -> errorText.setVisibility(View.GONE));
    }

    private void navigateToRegEntry(String json) {
        Bundle bundle = new Bundle();
        bundle.putString("json", json);
        NavHostFragment.findNavController(AuthFragment.this)
                .navigate(R.id.action_auth_to_reg, bundle);
    }

    private void navigateToPinEntry() {
        NavHostFragment.findNavController(AuthFragment.this)
                .navigate(R.id.action_auth_to_pin);
    }

    private void runOnUi(Runnable block) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(block);
        }
    }
}
