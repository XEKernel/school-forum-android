package com.schoolforum.app.ui.login;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolforum.app.R;
import com.schoolforum.app.model.BaseResponse;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 注册Fragment
 */
public class RegisterFragment extends Fragment {
    private static final String TAG = "RegisterFragment";

    private TextInputEditText etQq, etUsername, etPassword, etConfirmPassword, etBirthday, etEmail, etCode;
    private AutoCompleteTextView etGender, etSchool, etYear, etClass;
    private View btnSendCode, btnRegister;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private FileLogger logger;
    private final Gson gson = new Gson();
    
    private String selectedBirthday;
    private String selectedSchoolId;
    private int selectedYear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        logger = FileLogger.getInstance(requireContext());
        initViews(view);
        setupDropdowns();
        loadSchools();
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }

    private void initViews(View view) {
        etQq = view.findViewById(R.id.etQq);
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        etBirthday = view.findViewById(R.id.etBirthday);
        etEmail = view.findViewById(R.id.etEmail);
        etCode = view.findViewById(R.id.etCode);
        etGender = view.findViewById(R.id.etGender);
        etSchool = view.findViewById(R.id.etSchool);
        etYear = view.findViewById(R.id.etYear);
        etClass = view.findViewById(R.id.etClass);
        btnSendCode = view.findViewById(R.id.btnSendCode);
        btnRegister = view.findViewById(R.id.btnRegister);
        progressBar = getActivity().findViewById(R.id.progressBar);

        btnSendCode.setOnClickListener(v -> sendVerificationCode());
        btnRegister.setOnClickListener(v -> register());
        
        etBirthday.setOnClickListener(v -> showDatePicker());
    }

    private void setupDropdowns() {
        String[] genders = {"男", "女", "其他"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, genders);
        etGender.setAdapter(genderAdapter);

        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear; year >= currentYear - 5; year--) {
            years.add(String.valueOf(year));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, years);
        etYear.setAdapter(yearAdapter);
        etYear.setOnItemClickListener((parent, view, position, id) -> {
            selectedYear = Integer.parseInt(years.get(position));
            loadClasses(selectedSchoolId, selectedYear);
        });
    }

    private void loadSchools() {
        ApiClient.getInstance(requireContext()).get("/schools", null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        try {
                            SchoolsResponse response = gson.fromJson(responseBody, SchoolsResponse.class);
                            if (response.success && response.schools != null) {
                                setupSchoolDropdown(response.schools);
                                log("Loaded " + response.schools.size() + " schools");
                            }
                        } catch (Exception e) {
                            log("Failed to parse schools: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    log("Failed to load schools: " + error);
                }
            }, null);
    }

    private void setupSchoolDropdown(List<School> schools) {
        List<String> schoolNames = new ArrayList<>();
        List<String> schoolIds = new ArrayList<>();
        
        for (School school : schools) {
            schoolNames.add(school.name);
            schoolIds.add(school.id);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, schoolNames);
        etSchool.setAdapter(adapter);
        etSchool.setOnItemClickListener((parent, view, position, id) -> {
            selectedSchoolId = schoolIds.get(position);
            if (selectedYear > 0) {
                loadClasses(selectedSchoolId, selectedYear);
            }
        });
    }

    private void loadClasses(String schoolId, int year) {
        if (schoolId == null || year == 0) return;
        
        List<String> classNames = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            classNames.add("高一(" + i + ")班");
            classNames.add("高二(" + i + ")班");
            classNames.add("高三(" + i + ")班");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, classNames);
        etClass.setAdapter(adapter);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择出生日期")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedBirthday = sdf.format(new Date(selection));
            etBirthday.setText(selectedBirthday);
        });

        datePicker.show(getChildFragmentManager(), "date_picker");
    }

    private void sendVerificationCode() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("请输入有效的邮箱地址");
            return;
        }

        showLoading(true);
        
        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        
        ApiClient.getInstance(requireContext()).postForm("/send-verification-code", params, 
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                            if (response.success) {
                                Toast.makeText(getContext(), "验证码已发送", Toast.LENGTH_SHORT).show();
                                startCountDown();
                            } else {
                                Toast.makeText(getContext(), response.message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "发送失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }, null);
    }

    private void startCountDown() {
        btnSendCode.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                ((com.google.android.material.button.MaterialButton) btnSendCode)
                    .setText(millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                btnSendCode.setEnabled(true);
                ((com.google.android.material.button.MaterialButton) btnSendCode).setText("获取验证码");
            }
        }.start();
    }

    private void register() {
        String qq = etQq.getText() != null ? etQq.getText().toString().trim() : "";
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";
        String gender = etGender.getText() != null ? etGender.getText().toString() : "";
        String className = etClass.getText() != null ? etClass.getText().toString() : "";

        if (TextUtils.isEmpty(qq)) {
            etQq.setError("请输入QQ号");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("密码至少6位");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次密码不一致");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            etCode.setError("请输入验证码");
            return;
        }
        if (TextUtils.isEmpty(selectedSchoolId)) {
            Toast.makeText(getContext(), "请选择学校", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedYear == 0) {
            Toast.makeText(getContext(), "请选择入学年份", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(className)) {
            Toast.makeText(getContext(), "请选择班级", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("qq", qq);
        params.put("username", username);
        params.put("password", password);
        params.put("email", email);
        params.put("verificationCode", code);
        params.put("school", selectedSchoolId);
        params.put("enrollmentYear", String.valueOf(selectedYear));
        params.put("className", className);
        if (!TextUtils.isEmpty(selectedBirthday)) {
            params.put("birthday", selectedBirthday);
        }
        if (!TextUtils.isEmpty(gender)) {
            params.put("gender", gender.equals("男") ? "male" : gender.equals("女") ? "female" : "other");
        }

        ApiClient.getInstance(requireContext()).postForm("/register", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            RegisterResponse response = gson.fromJson(responseBody, RegisterResponse.class);
                            if (response.success && response.user != null) {
                                UserManager userManager = UserManager.getInstance(requireContext());
                                // 保存用户和 token，实现自动登录
                                userManager.saveUserWithTokens(response.user, response.accessToken, response.refreshToken, null);
                                Toast.makeText(getContext(), "注册成功", Toast.LENGTH_SHORT).show();
                                if (getActivity() instanceof LoginActivity) {
                                    ((LoginActivity) getActivity()).onLoginSuccess();
                                }
                            } else {
                                Toast.makeText(getContext(), response.message != null ? response.message : "注册失败", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            log("Register parse error: " + e.getMessage());
                            Toast.makeText(getContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }, null);
    }

    private static class School {
        @SerializedName("id")
        String id;
        @SerializedName("name")
        String name;
    }
    
    private static class SchoolsResponse extends BaseResponse {
        @SerializedName("schools")
        List<School> schools;
    }
    
    private static class RegisterResponse extends BaseResponse {
        @SerializedName("user")
        User user;
        @SerializedName("accessToken")
        String accessToken;
        @SerializedName("refreshToken")
        String refreshToken;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnRegister.setEnabled(!show);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
