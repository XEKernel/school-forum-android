package com.schoolforum.app.ui.report;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 举报页面
 */
public class ReportActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_TYPE = "targetType";
    public static final String EXTRA_TARGET_ID = "targetId";
    public static final String TARGET_TYPE_POST = "post";
    public static final String TARGET_TYPE_COMMENT = "comment";

    private String targetType;
    private String targetId;

    private RadioGroup radioGroupReason;
    private TextInputEditText etDescription;
    private View btnSubmit;

    private final Gson gson = new Gson();

    public static Intent createIntent(Context context, String targetType, String targetId) {
        Intent intent = new Intent(context, ReportActivity.class);
        intent.putExtra(EXTRA_TARGET_TYPE, targetType);
        intent.putExtra(EXTRA_TARGET_ID, targetId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        targetType = getIntent().getStringExtra(EXTRA_TARGET_TYPE);
        targetId = getIntent().getStringExtra(EXTRA_TARGET_ID);

        if (targetType == null || targetId == null) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        radioGroupReason = findViewById(R.id.radioGroupReason);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        int selectedId = radioGroupReason.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "请选择举报原因", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = getReasonFromRadioButton(selectedId);
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String reporterId = UserManager.getInstance(this).getCurrentUserId();

        if (reporterId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("reporterId", reporterId);
        params.put("targetType", targetType);
        params.put("targetId", targetId);
        params.put("reason", reason);
        if (!TextUtils.isEmpty(description)) {
            params.put("description", description);
        }

        btnSubmit.setEnabled(false);
        ApiClient.getInstance(this).postForm("/reports", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(ReportActivity.this, "举报已提交，我们会尽快处理", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "举报失败";
                                showError(message);
                            }
                        } catch (Exception e) {
                            showError("提交失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showError(error));
                }
            }, null);
    }

    private String getReasonFromRadioButton(int radioButtonId) {
        if (radioButtonId == R.id.rbSpam) return "SPAM";
        if (radioButtonId == R.id.rbHarassment) return "HARASSMENT";
        if (radioButtonId == R.id.rbInappropriate) return "INAPPROPRIATE";
        if (radioButtonId == R.id.rbFalseInfo) return "FALSE_INFO";
        if (radioButtonId == R.id.rbCopyright) return "COPYRIGHT";
        if (radioButtonId == R.id.rbOther) return "OTHER";
        return "OTHER";
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        btnSubmit.setEnabled(true);
    }
}
