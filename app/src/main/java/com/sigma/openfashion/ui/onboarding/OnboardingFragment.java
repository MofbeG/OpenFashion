package com.sigma.openfashion.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.sigma.openfashion.R;

/**
 * OnboardingFragment: показываем короткую презентацию, затем переходим на Auth.
 */
public class OnboardingFragment extends Fragment {

    public OnboardingFragment() {
        super(R.layout.fragment_onboarding);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton nextBtn = view.findViewById(R.id.onboardingNextButton);
        nextBtn.setOnClickListener(v -> {
            NavHostFragment.findNavController(OnboardingFragment.this)
                    .navigate(R.id.action_onboarding_to_auth);
        });
    }
}
