package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.sigma.openfashion.R;
import com.sigma.openfashion.Validator;
import com.sigma.openfashion.data.SupabaseService;

/**
 * PasswordRecoveryFragment: ввод email для получения OTP‑кода.
 */
public class PasswordRecoveryFragment extends Fragment {

    private TextInputEditText emailEditText;
    private MaterialButton sendOtpButton;
    private MaterialTextView errorText;
    private ProgressBar progressBar;
    private TextView recoveryTitle;

    private boolean isAuthEmail;
    private SupabaseService supabaseService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_recovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recoveryTitle   = view.findViewById(R.id.recoveryTitle);
        emailEditText   = view.findViewById(R.id.recoveryEmailEditText);
        sendOtpButton   = view.findViewById(R.id.sendOtpButton);
        errorText       = view.findViewById(R.id.recoveryErrorText);
        progressBar     = view.findViewById(R.id.recoveryProgress);

        supabaseService = new SupabaseService();
        Bundle args = getArguments();
        if (args != null) {
            isAuthEmail = args.getBoolean("isAuthEmail");
        }

        if (isAuthEmail)
            recoveryTitle.setText("Вход по email");

        sendOtpButton.setOnClickListener(v -> attemptSendOtp());
    }

    private void attemptSendOtp() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";

        if (!Validator.validateEmail(email, requireContext())) {
            emailEditText.setError("Неверный формат email");
            return;
        }
        hideError();
        showProgress(true);

        supabaseService.requestOtp(email, new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                handleOtpSent(email);
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void handleOtpSent(String email) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            // Передаём email дальше, чтобы OTPVerificationFragment знала, куда проверять
            Bundle bundle = new Bundle();
            bundle.putString("email", email);
            bundle.putBoolean("isAuthEmail", isAuthEmail);
            NavHostFragment.findNavController(PasswordRecoveryFragment.this)
                    .navigate(R.id.action_recovery_to_otp, bundle);
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        sendOtpButton.setEnabled(!show);
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
