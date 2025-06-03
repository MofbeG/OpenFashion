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
 * SignUpFragment: экран регистрации пользователя.
 */
public class SignUpFragment extends Fragment {

    private TextInputEditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private MaterialButton createAccountButton;
    private MaterialTextView errorText;
    private ProgressBar signUpProgress;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usernameEditText        = view.findViewById(R.id.usernameEditText);
        emailEditText           = view.findViewById(R.id.signUpEmailEditText);
        passwordEditText        = view.findViewById(R.id.signUpPasswordEditText);
        confirmPasswordEditText = view.findViewById(R.id.signUpConfirmPasswordEditText);
        createAccountButton     = view.findViewById(R.id.createAccountButton);
        errorText               = view.findViewById(R.id.signUpErrorText);
        signUpProgress          = view.findViewById(R.id.signUpProgress);

        supabaseService = new SupabaseService();
        prefs           = SharedPrefHelper.getInstance(requireContext());

        createAccountButton.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        String username = usernameEditText.getText() != null ? usernameEditText.getText().toString().trim() : "";
        String email    = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
        String confirm  = confirmPasswordEditText.getText() != null ? confirmPasswordEditText.getText().toString().trim() : "";

        // Валидация
        if (TextUtils.isEmpty(username) || username.length() > 20) {
            usernameEditText.setError("Имя: 1–20 символов");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Введите email");
            return;
        }
        if (!email.matches("^[a-z0-9]+@[a-z0-9]+\\.[a-z]{2,}$")) {
            emailEditText.setError("Неверный формат email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() > 8) {
            passwordEditText.setError("Пароль: 1–8 символов");
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordEditText.setError("Пароли не совпадают");
            return;
        }
        hideError();
        showProgress(true);

        // Сначала регистрируемся в Supabase Auth
        supabaseService.signUp(email, password, new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                handleSignUpSuccess(jsonResponse, username);
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void handleSignUpSuccess(String json, String username) {
        requireActivity().runOnUiThread(() -> {
            try {
                JSONObject obj       = new JSONObject(json);
                JSONObject session   = obj.optJSONObject("session");
                JSONObject userObj   = obj.optJSONObject("user");
                String accessToken   = session != null ? session.optString("access_token", null) : null;
                String userId        = userObj != null ? userObj.optString("id", null) : null;

                if (accessToken != null && userId != null) {
                    // Сохраняем токен и id
                    prefs.saveJwtToken(accessToken);
                    prefs.saveUserId(userId);
                    prefs.saveUserEmail(userObj.optString("email", ""));

                    // Создаём профиль в таблице profiles
                    createProfile(userId, username, userObj.optString("email", ""));

                } else {
                    showError("Неверный ответ сервера");
                }
            } catch (JSONException e) {
                showError("Ошибка парсинга: " + e.getMessage());
            }
        });
    }

    /** После регистрации создаём профиль в таблице profiles */
    private void createProfile(String userId, String username, String email) {
        supabaseService.setJwtToken(prefs.getJwtToken());
        supabaseService.upsertProfile(userId, username, "", "", new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    // Переходим на экран ввода PIN‑кода
                    NavHostFragment.findNavController(SignUpFragment.this)
                            .navigate(R.id.action_signUp_to_pin);
                });
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void showProgress(boolean show) {
        signUpProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        createAccountButton.setEnabled(!show);
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
