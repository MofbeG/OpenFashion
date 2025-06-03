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
 * ResetPasswordFragment: ввод нового пароля после проверки OTP.
 */
public class ResetPasswordFragment extends Fragment {

    private TextInputEditText newPasswordEditText, confirmNewPasswordEditText;
    private MaterialButton updatePasswordButton;
    private MaterialTextView errorText;
    private ProgressBar resetProgress;

    private SupabaseService supabaseService;
    private String email; // из аргументов

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        newPasswordEditText       = view.findViewById(R.id.newPasswordEditText);
        confirmNewPasswordEditText = view.findViewById(R.id.confirmNewPasswordEditText);
        updatePasswordButton      = view.findViewById(R.id.updatePasswordButton);
        errorText                 = view.findViewById(R.id.resetErrorText);
        resetProgress             = view.findViewById(R.id.resetProgress);

        supabaseService = new SupabaseService();
        Bundle args = getArguments();
        if (args != null) {
            email = args.getString("email");
        }

        updatePasswordButton.setOnClickListener(v -> attemptResetPassword());
    }

    private void attemptResetPassword() {
        String newPass = newPasswordEditText.getText() != null ? newPasswordEditText.getText().toString().trim() : "";
        String confirm = confirmNewPasswordEditText.getText() != null ? confirmNewPasswordEditText.getText().toString().trim() : "";

        if (TextUtils.isEmpty(newPass) || newPass.length() > 8) {
            newPasswordEditText.setError("Пароль: 1–8 символов");
            return;
        }
        if (!newPass.equals(confirm)) {
            confirmNewPasswordEditText.setError("Пароли не совпадают");
            return;
        }
        hideError();
        showProgress(true);

        supabaseService.resetPassword(email, newPass, new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                handleResetSuccess();
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void handleResetSuccess() {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            // После сброса пароля возвращаемся к экрану входа
            NavHostFragment.findNavController(ResetPasswordFragment.this)
                    .navigate(R.id.action_resetPassword_to_auth);
        });
    }

    private void showProgress(boolean show) {
        resetProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        updatePasswordButton.setEnabled(!show);
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
