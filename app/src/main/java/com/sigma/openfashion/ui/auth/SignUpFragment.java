// SignUpFragment.java (обновлённый фрагмент)

package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
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
 * SignUpFragment: экран регистрации пользователя.
 */
public class SignUpFragment extends Fragment {

    private static final String TAG = "SignUpFragment";

    private TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText;
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
        String email    = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim()
                : "";
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString().trim()
                : "";
        String confirm  = confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString().trim()
                : "";

        if (!Validator.validateEmail(email, requireContext())) {
            emailEditText.setError("Неверный формат email");
            return;
        }
        if (!Validator.validatePassword(password, requireContext())) {
            passwordEditText.setError("Неверный формат пароля");
            return;
        }
        if (!Validator.validatePassword(confirm, requireContext())) {
            confirmPasswordEditText.setError("Неверный формат пароля");
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordEditText.setError("Пароли не совпадают");
            passwordEditText.setError("Пароли не совпадают");
            return;
        }
        hideError();
        showProgress(true);

        // Регистрируемся в Supabase Auth
        supabaseService.signUp(email, password, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                handleSignUpSuccess(jsonResponse);
            }
            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    /**
     * Обработка ответа при регистрации:
     * - Если вернулся поля "session", берём из него access_token и user.id, создаём профиль и идём к PIN.
     * - Если поля "session" нет, значит нужно подтвердить email: возвращаем на экран входа с сообщением.
     */
    private void handleSignUpSuccess(String json) {
        requireActivity().runOnUiThread(() -> {
            try {
                JSONObject obj     = new JSONObject(json);
                String userId      = obj.optString("id", null);
                String userEmail   = obj.optString("email", null);

                prefs.saveUserId(userId);
                prefs.saveUserEmail(userEmail);
                prefs.saveUserPin(null);

                showProgress(false);
                errorText.setText("Проверьте почту и залогиньтесь в аккаунт");
                errorText.setVisibility(View.VISIBLE);
                createAccountButton.setText("Назад");
                createAccountButton.setOnClickListener(v -> {
                    NavHostFragment.findNavController(SignUpFragment.this)
                            .navigate(R.id.action_signUp_to_auth);
                });
            } catch (JSONException e) {
                showError("Ошибка парсинга: " + e.getMessage());
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
