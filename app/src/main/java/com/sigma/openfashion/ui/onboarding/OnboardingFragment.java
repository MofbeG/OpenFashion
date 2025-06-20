package com.sigma.openfashion.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.sigma.openfashion.R;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import java.util.Arrays;
import java.util.List;

/**
 * OnboardingFragment: показываем короткую презентацию, затем переходим на Auth.
 */
public class OnboardingFragment extends BaseFragment {

    private ViewPager2 viewPager;
    private MaterialButton nextButton;

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
    public void onResume() {
        super.onResume();
        setHeaderVisibility(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager  = view.findViewById(R.id.viewPager);
        nextButton = view.findViewById(R.id.onboardingNextButton);

        List<OnboardingPage> pages = Arrays.asList(
                new OnboardingPage("Добро пожаловать", "Описание первой страницы", R.drawable.ic_launcher_background),
                new OnboardingPage("Функции", "Описание второй страницы", R.drawable.ic_launcher_background),
                new OnboardingPage("Готово!", "Начинаем работу", R.drawable.ic_launcher_background)
        );

        viewPager.setAdapter(new OnboardingAdapter(pages));

        nextButton.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < pages.size() - 1) {
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                NavHostFragment.findNavController(OnboardingFragment.this)
                        .navigate(R.id.action_onboarding_to_auth);
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == pages.size() - 1) {
                    nextButton.setText(getString(R.string.Start));
                } else {
                    nextButton.setText(getString(R.string.Next));
                }
            }
        });
    }
}
