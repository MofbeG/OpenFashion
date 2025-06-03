package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.sigma.openfashion.data.SupabaseService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Экран входа (AuthFragment).
 */
public class AuthFragment extends Fragment {

    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton signInButton, signUpButton;
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
        signUpButton       = view.findViewById(R.id.signUpButton);
        forgotPasswordText = view.findViewById(R.id.forgotPasswordText);
        errorText          = view.findViewById(R.id.errorText);
        authProgress       = view.findViewById(R.id.authProgress);

        supabaseService = new SupabaseService();
        prefs           = SharedPrefHelper.getInstance(requireContext());

        signInButton.setOnClickListener(v -> attemptSignIn());
        signUpButton.setOnClickListener(v ->
                NavHostFragment.findNavController(AuthFragment.this)
                        .navigate(R.id.action_auth_to_signUp)
        );
        forgotPasswordText.setOnClickListener(v ->
                NavHostFragment.findNavController(AuthFragment.this)
                        .navigate(R.id.action_auth_to_passwordRecovery)
        );
    }

    private void attemptSignIn() {
        String email    = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";

        // Валидация
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Введите email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Введите пароль");
            return;
        }
        if (password.length() > 8) {
            passwordEditText.setError("Пароль не более 8 символов");
            return;
        }
        hideError();
        showProgress(true);

        // Вызов SupabaseService.signIn
        supabaseService.signIn(email, password, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                handleSignInSuccess(jsonResponse);
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void handleSignInSuccess(String json) {
        requireActivity().runOnUiThread(() -> {
            try {
                JSONObject obj       = new JSONObject(json);
                String accessToken   = obj.optString("access_token", null);
                JSONObject userObj   = obj.optJSONObject("user");
                String userId        = userObj != null ? userObj.optString("id", null) : null;
                String userEmail     = userObj != null ? userObj.optString("email", null) : null;

                if (accessToken != null && userId != null) {
                    prefs.saveJwtToken(accessToken);
                    prefs.saveUserId(userId);
                    prefs.saveUserEmail(userEmail);

                    // Очищаем поля
                    showProgress(false);
                    // Переходим на экран PIN‑кода (чтобы установить/проверить PIN)
                    NavHostFragment.findNavController(AuthFragment.this)
                            .navigate(R.id.action_auth_to_pin);
                } else {
                    showError("Ошибка авторизации");
                }
            } catch (JSONException e) {
                showError("Ошибка парсинга: " + e.getMessage());
            }
        });
    }

    private void showProgress(boolean show) {
        authProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        signInButton.setEnabled(!show);
        signUpButton.setEnabled(!show);
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
}
