package com.sigma.openfashion.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.sigma.openfashion.ui.auth.PinEntryFragment;
import com.sigma.openfashion.ui.auth.SignUpFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProfileFragment extends BaseFragment {
    private TextInputEditText emailEditText, nameEditText, lastnameEditText, signUpAddressEditText, signUpPhoneEditText;
    private MaterialButton buttonApply, buttonLogout;
    private MaterialTextView errorText;
    private ProgressBar errorProgress;
    private TextView textViewId;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    private String oldEmail;


    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.BUTTON_MENU | HeaderConfig.BUTTON_SEARCH | HeaderConfig.BUTTON_ORDER);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailEditText           = view.findViewById(R.id.emailEditText);
        nameEditText            = view.findViewById(R.id.nameEditText);
        lastnameEditText        = view.findViewById(R.id.lastnameEditText);
        signUpAddressEditText   = view.findViewById(R.id.signUpAddressEditText);
        signUpPhoneEditText     = view.findViewById(R.id.signUpPhoneEditText);
        buttonApply             = view.findViewById(R.id.buttonApply);
        buttonLogout            = view.findViewById(R.id.buttonLogout);
        errorText               = view.findViewById(R.id.errorText);
        errorProgress           = view.findViewById(R.id.errorProgress);
        textViewId              = view.findViewById(R.id.textViewId);

        supabaseService = new SupabaseService();
        prefs           = SharedPrefHelper.getInstance(requireContext());

        textViewId.setText("ID: " + prefs.getUserId());

        buttonLogout.setOnClickListener(view1 -> exit());
        buttonApply.setOnClickListener(view1 -> {
            String email    = emailEditText.getText() != null
                    ? emailEditText.getText().toString().trim()
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

            supabaseService.upsertProfile(prefs.getUserId(), first_name, last_name, email, phone, adress, new SupabaseService.QueryCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    runOnUi(() -> {
                        showProgress(false);
                        Toast.makeText(requireContext(), getString(R.string.sucress), Toast.LENGTH_LONG).show();
                    });
                }
                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });

            /*if (!oldEmail.equals(email)) {
                supabaseService.updateEmail(email, new SupabaseService.QueryCallback() {
                    @Override
                    public void onSuccess(String jsonResponse) {
                        runOnUi(() -> {
                            showProgress(false);
                            oldEmail = email;
                            Toast.makeText(requireContext(), getString(R.string.sucress), Toast.LENGTH_LONG).show();
                        });
                    }
                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                });
            }*/
        });

        showProgress(true);

        supabaseService.setJwtToken(prefs.getJwtToken());
        supabaseService.getProfile(prefs.getUserId(), new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONArray array = new JSONArray(jsonResponse);
                        JSONObject obj = array.getJSONObject(0);
                        String first_name   = obj.optString("first_name", null);
                        String last_name = obj.optString("last_name", null);

                        prefs.saveKeyUserFirstName(first_name);
                        prefs.saveKeyUserLastName(last_name);

                        oldEmail = obj.getString("email");
                        emailEditText.setText(oldEmail);
                        nameEditText.setText(obj.getString("first_name"));
                        lastnameEditText.setText(obj.getString("last_name"));
                        signUpAddressEditText.setText(obj.getString("address"));
                        signUpPhoneEditText.setText(obj.getString("phone"));
                    } catch (JSONException e) {
                        showError(getString(R.string.Parse_Error) + e.getMessage());
                    }
                });
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void exit() {
        showProgress(true);
        supabaseService.setJwtToken(prefs.getJwtToken());
        supabaseService.logout(prefs.getRefreshToken(), new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    String tempMail = prefs.getUserEmail();
                    prefs.clearAll();
                    prefs.saveUserEmail(tempMail);
                    NavHostFragment.findNavController(ProfileFragment.this)
                            .navigate(R.id.action_profile_to_auth);
                });
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void showProgress(boolean show) {
        errorProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonApply.setEnabled(!show);
        buttonLogout.setEnabled(!show);
    }

    private void showError(String message) {
        runOnUi(() -> {
            showProgress(false);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(message);
        });
    }

    private void hideError() {
        runOnUi(() -> errorText.setVisibility(View.GONE));
    }
}
