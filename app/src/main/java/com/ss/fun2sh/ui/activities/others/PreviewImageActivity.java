package com.ss.fun2sh.ui.activities.others;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.base.BaseLoggableActivity;
import com.ss.fun2sh.ui.adapters.chats.BaseDialogMessagesAdapter;
import com.ss.fun2sh.ui.views.TouchImageView;
import com.ss.fun2sh.utils.image.ImageLoaderUtils;

import butterknife.Bind;

public class PreviewImageActivity extends BaseLoggableActivity {

    public static final String EXTRA_IMAGE_URL = "image";

    private static final int IMAGE_MAX_ZOOM = 4;

    @Bind(R.id.image_touchimageview)
    TouchImageView imageTouchImageView;

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, PreviewImageActivity.class);
        intent.putExtra(EXTRA_IMAGE_URL, url);
        context.startActivity(intent);
    }

    @Override
    protected int getContentResId() {
        return R.layout.activity_preview_image;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFields();
        setUpActionBarWithUpButton();
        initTouchImageView();
        displayImage();
    }

    private void initFields() {
        title = getString(R.string.preview_image_title);
    }

    private void displayImage() {
        imageTouchImageView.setImageBitmap(BaseDialogMessagesAdapter.bitmap);
        /*String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (!TextUtils.isEmpty(imageUrl)) {
            ImageLoader.getInstance().displayImage(imageUrl, imageTouchImageView,
                    ImageLoaderUtils.UIL_DEFAULT_DISPLAY_OPTIONS);
        }*/
    }

    private void initTouchImageView() {
        imageTouchImageView.setMaxZoom(IMAGE_MAX_ZOOM);
    }
}