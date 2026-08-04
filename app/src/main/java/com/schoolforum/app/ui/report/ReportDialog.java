package com.schoolforum.app.ui.report;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 举报对话框
 */
public class ReportDialog extends Dialog {

    public static final String TARGET_TYPE_POST = "post";
    public static final String TARGET_TYPE_COMMENT = "comment";

    private String targetType;
    private String targetId;

    private RadioGroup radioGroupReason;
    private TextInputEditText etDescription;
    private View btnSubmit;

    private final Gson gson = new Gson();
    private OnReportSubmittedListener listener;

    public interface OnReportSubmittedListener {
        void onReportSubmitted();
    }

    public ReportDialog(@NonNull Context context, String targetType, String targetId) {
        super(context);
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public void setOnReportSubmittedListener(OnReportSubmittedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report, null);
        setContentView(view);

        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        radioGroupReason = view.findViewById(R.id.radioGroupReason);
        etDescription = view.findViewById(R.id.etDescription);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        // 获取选中的原因
        int selectedId = radioGroupReason.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "请选择举报原因", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = getReasonFromRadioButton(selectedId);
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String reporterId = UserManager.getInstance(getContext()).getCurrentUserId();

        if (reporterId == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
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
        ApiClient.getInstance(getContext()).postForm("/reports", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    try {
                        JsonObject response = gson.fromJson(data, JsonObject.class);
                        if (response.has("success") && response.get("success").getAsBoolean()) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "举报已提交，我们会尽快处理", Toast.LENGTH_SHORT).show();
                            }
                            dismiss();
                            if (listener != null) {
                                listener.onReportSubmitted();
                            }
                        } else {
                            String message = response.has("message") ? response.get("message").getAsString() : "举报失败";
                            showError(message);
                        }
                    } catch (Exception e) {
                        showError("提交失败");
                    }
                }

                @Override
                public void onError(String error) {
                    showError(error);
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
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        btnSubmit.setEnabled(true);
    }
}
