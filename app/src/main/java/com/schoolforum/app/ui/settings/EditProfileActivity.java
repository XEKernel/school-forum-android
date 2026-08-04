package com.schoolforum.app.ui.settings;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * 编辑资料页面
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_AVATAR = 1001;

    private CircleImageView ivAvatar;
    private TextInputEditText etUsername, etSignature;
    private RadioGroup rgGender;
    private TextView tvSchoolValue, tvEnrollmentYearValue, tvGradeAuto, tvClassValue, tvBirthdayValue;

    private String currentUserId;
    private int selectedEnrollmentYear;
    private String selectedSchool;
    private String selectedGrade;
    private String selectedClass;
    private String birthday;
    private File newAvatarFile;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        currentUserId = UserManager.getInstance(this).getCurrentUserId();
        
        initViews();
        loadUserProfile();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        etUsername = findViewById(R.id.etUsername);
        etSignature = findViewById(R.id.etSignature);
        rgGender = findViewById(R.id.rgGender);
        tvSchoolValue = findViewById(R.id.tvSchoolValue);
        tvEnrollmentYearValue = findViewById(R.id.tvEnrollmentYearValue);
        tvGradeAuto = findViewById(R.id.tvGradeAuto);
        tvClassValue = findViewById(R.id.tvClassValue);
        tvBirthdayValue = findViewById(R.id.tvBirthdayValue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnChangeAvatar).setOnClickListener(v -> pickAvatar());
        findViewById(R.id.itemSchool).setOnClickListener(v -> showSchoolDialog());
        findViewById(R.id.itemEnrollmentYear).setOnClickListener(v -> showEnrollmentYearDialog());
        findViewById(R.id.itemClass).setOnClickListener(v -> showClassDialog());
        findViewById(R.id.itemBirthday).setOnClickListener(v -> showBirthdayDialog());
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;

        // 先加载学校列表，再加载用户资料
        ApiClient.getInstance(this).get("/schools", null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("schools")) {
                                schoolList = gson.fromJson(response.get("schools"),
                                    new com.google.gson.reflect.TypeToken<java.util.List<SchoolInfo>>(){}.getType());
                            }
                            // 学校列表加载后，加载用户资料
                            loadUserData();
                        } catch (Exception e) {
                            // 即使学校列表加载失败，也继续加载用户资料
                            loadUserData();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> loadUserData());
                }
            }, null);
    }

    private void loadUserData() {
        ApiClient.getInstance(this).get("/user/profile/" + currentUserId, null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                JsonObject user = response.getAsJsonObject("user");
                                populateUserdata(user);
                            } else {
                                String msg = response.has("message") ? response.get("message").getAsString() : "加载失败";
                                Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(EditProfileActivity.this, "解析数据失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show());
                }
            }, null);
    }

    private void populateUserdata(JsonObject user) {
        // 头像
        if (user.has("avatar") && !user.get("avatar").isJsonNull()) {
            String avatar = user.get("avatar").getAsString();
            if (avatar != null && !avatar.isEmpty()) {
                String url = avatar.startsWith("http") ? avatar : MainActivity.getBaseUrl() + avatar;
                Glide.with(this).load(url).placeholder(R.mipmap.ic_launcher_round).into(ivAvatar);
            }
        }

        // 昵称
        if (user.has("username") && !user.get("username").isJsonNull()) {
            etUsername.setText(user.get("username").getAsString());
        }

        // 签名（可能在 settings.signature 中）
        String signature = "";
        if (user.has("signature") && !user.get("signature").isJsonNull()) {
            signature = user.get("signature").getAsString();
        } else if (user.has("settings") && !user.get("settings").isJsonNull()) {
            JsonObject settings = user.getAsJsonObject("settings");
            if (settings.has("signature") && !settings.get("signature").isJsonNull()) {
                signature = settings.get("signature").getAsString();
            }
        }
        etSignature.setText(signature);

        // 性别
        String gender = "secret";
        if (user.has("gender") && !user.get("gender").isJsonNull()) {
            gender = user.get("gender").getAsString();
        }
        if ("male".equals(gender)) {
            ((RadioButton) findViewById(R.id.rbGenderMale)).setChecked(true);
        } else if ("female".equals(gender)) {
            ((RadioButton) findViewById(R.id.rbGenderFemale)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbGenderSecret)).setChecked(true);
        }

        // 学校（需要从学校列表中查找；用户资料存的是学校名称，兼容旧数据存 id）
        if (user.has("school") && !user.get("school").isJsonNull()) {
            String userSchool = user.get("school").getAsString();
            if (userSchool != null && schoolList != null) {
                for (SchoolInfo school : schoolList) {
                    if (userSchool.equals(school.name) || userSchool.equals(school.id)) {
                        selectedSchoolInfo = school;
                        tvSchoolValue.setText(school.name);
                        break;
                    }
                }
            }
        } else {
            tvSchoolValue.setText("未设置");
        }

        // 入学年份
        if (user.has("enrollmentYear") && !user.get("enrollmentYear").isJsonNull()) {
            selectedEnrollmentYear = user.get("enrollmentYear").getAsInt();
            tvEnrollmentYearValue.setText(selectedEnrollmentYear + "年");
            updateGradeDisplay();
        } else {
            tvEnrollmentYearValue.setText("未设置");
            tvGradeAuto.setText("");
        }
        
        // 班级
        if (user.has("className") && !user.get("className").isJsonNull()) {
            selectedClass = user.get("className").getAsString();
            tvClassValue.setText(selectedClass);
        } else {
            tvClassValue.setText("未设置");
        }

        // 生日
        if (user.has("birthday") && !user.get("birthday").isJsonNull()) {
            birthday = user.get("birthday").getAsString();
            tvBirthdayValue.setText(birthday);
        } else {
            tvBirthdayValue.setText("未设置");
        }
    }

    private void pickAvatar() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_AVATAR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AVATAR && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                Glide.with(this).load(uri).into(ivAvatar);
                // 保存文件用于上传
                try {
                    newAvatarFile = com.schoolforum.app.utils.ImageUtils.compressImage(this, uri);
                } catch (Exception e) {
                    Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showSchoolDialog() {
        // 从服务器获取学校列表
        ApiClient.getInstance(this).get("/schools", null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                // schools 是对象数组 [{id, name, classInfo}, ...]
                                if (response.has("schools")) {
                                    List<SchoolInfo> schools = gson.fromJson(response.get("schools"), 
                                        new com.google.gson.reflect.TypeToken<List<SchoolInfo>>(){}.getType());
                                    if (schools != null && !schools.isEmpty()) {
                                        showSchoolOptions(schools);
                                        return;
                                    }
                                }
                            }
                            String msg = response.has("message") ? response.get("message").getAsString() : "获取学校列表失败";
                            Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(EditProfileActivity.this, "解析学校列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "获取学校列表失败: " + error, Toast.LENGTH_SHORT).show());
                }
            }, null);
    }
    
    private List<SchoolInfo> schoolList;
    private SchoolInfo selectedSchoolInfo;
    
    private void showSchoolOptions(List<SchoolInfo> schools) {
        schoolList = schools;
        String[] names = new String[schools.size()];
        for (int i = 0; i < schools.size(); i++) {
            names[i] = schools.get(i).name;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("选择学校")
            .setItems(names, (dialog, which) -> {
                selectedSchoolInfo = schools.get(which);
                selectedSchool = selectedSchoolInfo.name;
                tvSchoolValue.setText(selectedSchool);
            })
            .show();
    }
    
    // 学校信息类
    private static class SchoolInfo {
        String id;
        String name;
        List<ClassInfo> classInfo;
    }
    
    // 班级信息类
    private static class ClassInfo {
        int year;
        int classCount;
    }

    /**
     * 显示入学年份选择
     */
    private void showEnrollmentYearDialog() {
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        // 生成年份列表：当前年份到当前年份-6年
        String[] years = new String[7];
        for (int i = 0; i < 7; i++) {
            years[i] = String.valueOf(currentYear - i);
        }
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("选择入学年份")
            .setItems(years, (dialog, which) -> {
                selectedEnrollmentYear = currentYear - which;
                tvEnrollmentYearValue.setText(selectedEnrollmentYear + "年");
                updateGradeDisplay();
            })
            .show();
    }

    /**
     * 根据入学年份计算年级并显示
     */
    private void updateGradeDisplay() {
        if (selectedEnrollmentYear > 0) {
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            int gradeLevel = currentYear - selectedEnrollmentYear;
            
            if (gradeLevel <= 0) {
                selectedGrade = selectedEnrollmentYear + "级";
                tvGradeAuto.setText("（未入学）");
            } else if (gradeLevel <= 3) {
                selectedGrade = "高" + (gradeLevel == 1 ? "一" : gradeLevel == 2 ? "二" : "三");
                tvGradeAuto.setText("（" + selectedGrade + "）");
            } else {
                selectedGrade = selectedEnrollmentYear + "级";
                tvGradeAuto.setText("（已毕业）");
            }
        } else {
            tvGradeAuto.setText("");
        }
    }

    /**
     * 显示班级选择（仅选择班级号）
     */
    private void showClassDialog() {
        // 根据选择的学校获取班级数量
        int maxClassCount = 0;
        if (selectedSchoolInfo != null && selectedSchoolInfo.classInfo != null) {
            for (ClassInfo info : selectedSchoolInfo.classInfo) {
                if (info.year == selectedEnrollmentYear) {
                    maxClassCount = info.classCount;
                    break;
                }
            }
        }
        
        if (maxClassCount > 0) {
            // 显示班级列表
            String[] classes = new String[maxClassCount];
            for (int i = 0; i < maxClassCount; i++) {
                classes[i] = (i + 1) + "班";
            }
            new android.app.AlertDialog.Builder(this)
                .setTitle("选择班级")
                .setItems(classes, (dialog, which) -> {
                    selectedClass = (which + 1) + "班";
                    tvClassValue.setText(selectedClass);
                })
                .setNegativeButton("取消", null)
                .show();
        } else {
            // 手动输入班级
            android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("输入班级，如：1班");
            if (selectedClass != null) {
                input.setText(selectedClass.replace("班", ""));
            }
            new android.app.AlertDialog.Builder(this)
                .setTitle("输入班级")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String inputText = input.getText().toString().trim();
                    if (!inputText.isEmpty()) {
                        selectedClass = inputText.endsWith("班") ? inputText : inputText + "班";
                        tvClassValue.setText(selectedClass);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        }
    }

    private void showBirthdayDialog() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) -> {
            birthday = String.format("%d-%02d-%02d", y, m + 1, d);
            tvBirthdayValue.setText(birthday);
        }, year, month, day).show();
    }

    private void saveProfile() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String signature = etSignature.getText() != null ? etSignature.getText().toString().trim() : "";

        if (username.isEmpty()) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String gender = "secret";
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rbGenderMale) gender = "male";
        else if (selectedId == R.id.rbGenderFemale) gender = "female";

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("signature", signature);
        params.put("gender", gender);
        // school 字段应存学校名称（id 是配置代号如 'XXXX'，存储名称才能正确展示）
        if (selectedSchoolInfo != null) params.put("school", selectedSchoolInfo.name);
        if (selectedEnrollmentYear > 0) params.put("enrollmentYear", String.valueOf(selectedEnrollmentYear));
        if (selectedClass != null) params.put("className", selectedClass);
        if (birthday != null) params.put("birthday", birthday);

        if (newAvatarFile != null) {
            // 上传头像和资料
            uploadWithAvatar(params);
        } else {
            // 只更新资料
            updateProfile(params);
        }
    }

    private void uploadWithAvatar(Map<String, String> params) {
        ApiClient.getInstance(this).postMultipart("/user/update-profile", 
            new HashMap<>(params), java.util.Collections.singletonList(newAvatarFile), "avatar",
            new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    String data = response.body() != null ? response.body().string() : "";
                    handleSaveResult(data);
                }
            });
    }

    private void updateProfile(Map<String, String> params) {
        ApiClient.getInstance(this).postForm("/user/update-profile", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    handleSaveResult(data);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show());
                }
            }, null);
    }

    private void handleSaveResult(String data) {
        runOnUiThread(() -> {
            try {
                JsonObject response = gson.fromJson(data, JsonObject.class);
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    // 用服务端返回的最新用户数据刷新本地缓存（避免其他页面显示旧值）
                    if (response.has("user") && response.get("user").isJsonObject()) {
                        try {
                            com.schoolforum.app.model.User updatedUser = gson.fromJson(
                                    response.getAsJsonObject("user").toString(),
                                    com.schoolforum.app.model.User.class);
                            if (updatedUser != null && updatedUser.getId() != null) {
                                UserManager.getInstance(this).saveUser(updatedUser);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String message = response.has("message") ? response.get("message").getAsString() : "保存失败";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
