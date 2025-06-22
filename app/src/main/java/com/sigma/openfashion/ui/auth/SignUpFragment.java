// SignUpFragment.java (обновлённый фрагмент)

package com.sigma.openfashion.ui.auth;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;
import com.sigma.openfashion.ui.utils.SwipeGestureListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * SignUpFragment: экран регистрации пользователя.
 */
public class SignUpFragment extends BaseFragment {
    private TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText, nameEditText, lastnameEditText, signUpAddressEditText, signUpPhoneEditText;
    private MaterialButton createAccountButton;
    private MaterialTextView errorText;
    private ProgressBar signUpProgress;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    private GestureDetector gestureDetector;

    public SignUpFragment() {
        super(R.layout.fragment_sign_up);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.LOGO_ONLY);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        gestureDetector = new GestureDetector(requireContext(), new SwipeGestureListener() {
            @Override
            public void onSwipeRight() {
                navigateToAuthFragment();
            }

            @Override
            public void onSwipeLeft() {
            }
        });

        view.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emailEditText           = view.findViewById(R.id.emailEditText);
        nameEditText            = view.findViewById(R.id.nameEditText);
        lastnameEditText        = view.findViewById(R.id.lastnameEditText);
        signUpAddressEditText   = view.findViewById(R.id.signUpAddressEditText);
        signUpPhoneEditText     = view.findViewById(R.id.signUpPhoneEditText);
        passwordEditText        = view.findViewById(R.id.signUpPasswordEditText);
        confirmPasswordEditText = view.findViewById(R.id.signUpConfirmPasswordEditText);
        createAccountButton     = view.findViewById(R.id.createAccountButton);
        errorText               = view.findViewById(R.id.signUpErrorText);
        signUpProgress          = view.findViewById(R.id.signUpProgress);

        supabaseService = new SupabaseService();
        prefs           = SharedPrefHelper.getInstance(requireContext());

        view.findViewById(R.id.linearLayoutOnClick).setOnClickListener(v -> {
            NavHostFragment.findNavController(SignUpFragment.this)
                    .navigate(R.id.action_signUp_to_auth);
        });

        // Следим за изменением текста
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Проверяем все поля
                boolean allFilled = !emailEditText.getText().toString().trim().isEmpty()
                        && !nameEditText.getText().toString().trim().isEmpty()
                        && !signUpAddressEditText.getText().toString().trim().isEmpty()
                        && !signUpPhoneEditText.getText().toString().trim().isEmpty()
                        && !passwordEditText.getText().toString().trim().isEmpty()
                        && !confirmPasswordEditText.getText().toString().trim().isEmpty();

                createAccountButton.setEnabled(allFilled);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        // Назначаем слушатель на поля
        emailEditText.addTextChangedListener(watcher);
        nameEditText.addTextChangedListener(watcher);
        signUpAddressEditText.addTextChangedListener(watcher);
        signUpPhoneEditText.addTextChangedListener(watcher);
        passwordEditText.addTextChangedListener(watcher);
        confirmPasswordEditText.addTextChangedListener(watcher);

        createAccountButton.setEnabled(false);
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
        String first_name  = nameEditText.getText() != null
                ? nameEditText.getText().toString().trim()
                : "";
        String last_name  = lastnameEditText.getText() != null
                ? lastnameEditText.getText().toString().trim()
                : "";
        String adress  = signUpAddressEditText.getText() != null
                ? signUpAddressEditText.getText().toString().trim()
                : "";
        String phone  = signUpPhoneEditText.getText() != null
                ? signUpPhoneEditText.getText().toString().trim()
                : "";

        if (!Validator.validateEmail(email, requireContext())) {
            emailEditText.setError(getString(R.string.Incorrect_email_format));
            return;
        }
        if (!Validator.validateUsername(first_name, requireContext())) {
            nameEditText.setError(getString(R.string.Incorrect_name_format));
            return;
        }
        if (!last_name.trim().isEmpty() && !Validator.validateUsername(last_name, requireContext())) {
            lastnameEditText.setError(getString(R.string.Incorrect_name_format));
            return;
        }
        if (!Validator.validatePhone(phone, requireContext())) {
            signUpPhoneEditText.setError(getString(R.string.Incorrect_phone_format));
            return;
        }
        if (!Validator.validatePassword(password, requireContext())) {
            passwordEditText.setError(getString(R.string.Incorrect_password_format));
            return;
        }
        if (!Validator.validatePassword(confirm, requireContext())) {
            confirmPasswordEditText.setError(getString(R.string.Incorrect_password_format));
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordEditText.setError(getString(R.string.The_passwords_dont_match));
            passwordEditText.setError(getString(R.string.The_passwords_dont_match));
            return;
        }
        hideError();
        showProgress(true);

        // Регистрируемся в Supabase Auth
        supabaseService.signUp(email, password, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                handleSignUpSuccess(jsonResponse, first_name, last_name, adress, phone);
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
    private void handleSignUpSuccess(String json, String first_name, String last_name, String adress, String phone) {
        runOnUi(() -> {
            try {
                JSONObject obj     = new JSONObject(json);
                JSONObject userObj = obj.getJSONObject("user");

                if (userObj != null) {
                    String access_token  = obj.optString("access_token", null);
                    String refresh_token = obj.optString("refresh_token", null);
                    String id            = userObj.optString("id", null);
                    String email         = userObj.optString("email", null);

                    prefs.saveRefreshToken(refresh_token);
                    prefs.saveJwtToken(access_token);
                    prefs.saveUserId(id);
                    prefs.saveUserEmail(email);
                    prefs.saveKeyUserFirstName(first_name);
                    prefs.saveKeyUserLastName(last_name);

                    supabaseService.setJwtToken(access_token);
                    supabaseService.upsertProfile(id, first_name, last_name, email, phone, adress, new SupabaseService.QueryCallback() {
                        @Override
                        public void onSuccess(String jsonResponse) {
                            runOnUi(() -> {
                                showProgress(false);
                                NavHostFragment.findNavController(SignUpFragment.this)
                                        .navigate(R.id.action_signUp_to_pin);
                            });
                        }
                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                } else {
                    showProgress(false);
                    errorText.setText(getString(R.string.Check_your_email_and_log_in_to_your_account));
                    errorText.setVisibility(View.VISIBLE);
                    createAccountButton.setText(getString(R.string.Back));
                    createAccountButton.setOnClickListener(v -> {
                        navigateToAuthFragment();
                    });
                }
            } catch (JSONException e) {
                showError(getString(R.string.Parse_Error) + e.getMessage());
            }
        });
    }

    private void showProgress(boolean show) {
        signUpProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        createAccountButton.setEnabled(!show);
    }

    private void showError(String message) {
        runOnUi(() -> {
            showProgress(false);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void hideError() {
        runOnUi(() -> errorText.setVisibility(View.GONE));
    }

    private void navigateToAuthFragment() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_signUp_to_auth);
    }
}
