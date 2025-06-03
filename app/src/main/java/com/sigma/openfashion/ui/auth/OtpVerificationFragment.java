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
import com.sigma.openfashion.data.SupabaseService;

/**
 * OtpVerificationFragment: ввод и проверка OTP‑кода.
 */
public class OtpVerificationFragment extends Fragment {

    private TextInputEditText otpEditText;
    private MaterialButton verifyOtpButton;
    private MaterialTextView errorText;
    private ProgressBar progressBar;

    private SupabaseService supabaseService;
    private String email; // получим из аргументов

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        otpEditText       = view.findViewById(R.id.otpEditText);
        verifyOtpButton   = view.findViewById(R.id.verifyOtpButton);
        errorText         = view.findViewById(R.id.otpErrorText);
        progressBar       = view.findViewById(R.id.otpProgress);

        supabaseService = new SupabaseService();
        // Читаем email из аргументов
        Bundle args = getArguments();
        if (args != null) {
            email = args.getString("email");
        }

        verifyOtpButton.setOnClickListener(v -> attemptVerifyOtp());
    }

    private void attemptVerifyOtp() {
        String otp = otpEditText.getText() != null ? otpEditText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(otp) || otp.length() != 6) {
            otpEditText.setError("Введите 6 цифр");
            return;
        }
        hideError();
        showProgress(true);

        supabaseService.verifyOtp(email, otp, new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                handleOtpSuccess();
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void handleOtpSuccess() {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            // Переходим на экран сброса пароля, передав email дальше
            Bundle bundle = new Bundle();
            bundle.putString("email", email);
            NavHostFragment.findNavController(OtpVerificationFragment.this)
                    .navigate(R.id.action_otp_to_resetPassword, bundle);
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        verifyOtpButton.setEnabled(!show);
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
